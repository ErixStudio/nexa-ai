<?php
/**
 * NEXA AI Payment & Secure Server-Side Subscription Activator Service
 */

require_once __DIR__ . '/../config/database.php';

/**
 * Safely adds calendar months to a DateTime without month-end overflow issues
 * (e.g. Jan 31 + 1 month -> Feb 28/29, Mar 31 + 1 month -> Apr 30)
 */
function addCalendarMonths(DateTime $baseDate, int $months): DateTime {
    $result = clone $baseDate;
    $day = (int)$result->format('d');
    $result->modify("+{$months} months");
    if ((int)$result->format('d') !== $day) {
        $result->modify('last day of previous month');
    }
    return $result;
}

function createPaymentIntent(string $userId, string $planName, int $amountToman, string $transactionRef): array {
    $pdo = getDbConnection();
    $paymentId = 'pay_' . substr(bin2hex(random_bytes(16)), 0, 16);

    $stmt = $pdo->prepare("
        INSERT INTO payments (id, user_id, plan_name, amount_toman, payment_status, transaction_ref) 
        VALUES (?, ?, ?, ?, 'PENDING', ?)
    ");
    $stmt->execute([$paymentId, $userId, $planName, $amountToman, $transactionRef]);

    return [
        'id' => $paymentId,
        'userId' => $userId,
        'planName' => $planName,
        'amountToman' => $amountToman,
        'status' => 'PENDING',
        'transactionRef' => $transactionRef
    ];
}

/**
 * Internal Server-Side Function: CANNOT be directly invoked by untrusted client request.
 */
function activateSubscriptionAfterVerifiedPayment(string $paymentId, string $verifiedRefId): array {
    $pdo = getDbConnection();

    $planMonthsMap = [
        'MONTHLY' => 1,
        'QUARTERLY' => 3,
        'SEMI_ANNUAL' => 6,
        'ANNUAL' => 12,
    ];

    try {
        $pdo->beginTransaction();

        $stmt = $pdo->prepare("SELECT id, user_id, plan_name, amount_toman, payment_status FROM payments WHERE id = ? FOR UPDATE");
        $stmt->execute([$paymentId]);
        $payment = $stmt->fetch();

        if (!$payment) {
            throw new Exception('رکورد پرداخت یافت نشد.');
        }

        if ($payment['payment_status'] === 'COMPLETED') {
            $pdo->commit();
            return ['message' => 'پرداخت قبلاً تایید و فعال شده است.'];
        }

        // Mark payment COMPLETED
        $stmt = $pdo->prepare("UPDATE payments SET payment_status = 'COMPLETED', transaction_ref = ? WHERE id = ?");
        $stmt->execute([$verifiedRefId, $paymentId]);

        $planMonths = $planMonthsMap[$payment['plan_name']] ?? 1;

        // Check if user already has an active subscription to extend
        $stmtActive = $pdo->prepare("
            SELECT end_date FROM subscriptions 
            WHERE user_id = ? AND status = 'ACTIVE' AND end_date > NOW() 
            ORDER BY end_date DESC LIMIT 1
        ");
        $stmtActive->execute([$payment['user_id']]);
        $activeSub = $stmtActive->fetch();

        $now = new DateTime();
        $startDate = $now->format('Y-m-d H:i:s');
        if ($activeSub && !empty($activeSub['end_date'])) {
            $baseDate = new DateTime($activeSub['end_date']);
            if ($baseDate < $now) {
                $baseDate = $now;
            }
        } else {
            $baseDate = $now;
        }

        $endDate = addCalendarMonths($baseDate, $planMonths)->format('Y-m-d H:i:s');

        // Insert active subscription with exact calendar duration
        $subId = 'sub_' . substr(bin2hex(random_bytes(16)), 0, 16);

        $stmt = $pdo->prepare("
            INSERT INTO subscriptions (id, user_id, plan_name, status, start_date, end_date) 
            VALUES (?, ?, ?, 'ACTIVE', ?, ?)
        ");
        $stmt->execute([$subId, $payment['user_id'], $payment['plan_name'], $startDate, $endDate]);

        // Update user premium
        $stmt = $pdo->prepare("UPDATE users SET is_premium = 1, subscription_plan = ? WHERE id = ?");
        $stmt->execute([$payment['plan_name'], $payment['user_id']]);

        $pdo->commit();

        return [
            'success' => true,
            'subscriptionId' => $subId,
            'planName' => $payment['plan_name'],
            'startDate' => $startDate,
            'endDate' => $endDate
        ];
    } catch (Exception $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log("[PaymentService Error] " . $e->getMessage());
        throw $e;
    }
}

/**
 * Server-Side Verification for Myket In-App Purchases
 */
function verifyMyketPurchase(string $userId, string $packageName, string $sku, string $purchaseToken, string $planKey): array {
    $pdo = getDbConnection();

    $cleanToken = trim($purchaseToken);
    $cleanSku = trim($sku);
    $cleanPackage = trim($packageName);
    $cleanPlan = strtoupper(trim($planKey));

    if (empty($cleanToken) || empty($cleanSku)) {
        throw new Exception('اطلاعات توکن خرید یا شناسه محصول مایکت نامعتبر است.', 400);
    }

    // 1. Authoritative SKU & Plan Configuration (Determined solely on the server)
    $skuMap = [
        'nexa_monthly' => ['plan' => 'MONTHLY', 'months' => 1, 'amount' => 259000],
        'nexa_quarterly' => ['plan' => 'QUARTERLY', 'months' => 3, 'amount' => 599000],
        'nexa_semiannual' => ['plan' => 'SEMI_ANNUAL', 'months' => 6, 'amount' => 999000],
        'nexa_annual' => ['plan' => 'ANNUAL', 'months' => 12, 'amount' => 1599000],
    ];

    $skuLower = strtolower($cleanSku);
    if (!isset($skuMap[$skuLower])) {
        throw new Exception("شناسه محصول مایکت ({$cleanSku}) نامعتبر یا تعریف‌نشده است.", 400);
    }

    $planInfo = $skuMap[$skuLower];
    $normalizedPlan = $planInfo['plan'];
    $durationMonths = $planInfo['months'];
    $amountToman = $planInfo['amount'];

    // Expected package verification
    $expectedPackage = env('MYKET_PACKAGE_NAME', 'com.aistudio.nexaai.trading');
    if (!empty($cleanPackage) && $cleanPackage !== $expectedPackage && $cleanPackage !== 'com.example') {
        throw new Exception('شناسه پکیج ارسال شده با بسته نرم‌افزاری مجاز تطابق ندارد.', 400);
    }

    if (!empty($cleanPlan) && $cleanPlan !== $normalizedPlan && $cleanPlan !== 'PRO' && $cleanPlan !== 'VIP') {
        throw new Exception("شناسه محصول مایکت با پلن انتخابی تطابق ندارد.", 400);
    }

    // 2. Myket Server Verification via Developer API (FAIL CLOSED if API key is not configured)
    $myketApiKey = env('MYKET_API_KEY');
    if (empty($myketApiKey) || $myketApiKey === 'YOUR_MYKET_API_KEY') {
        throw new Exception('سرویس بررسی اعتبار خرید مایکت بر روی سرور تنظیم نشده است (MYKET_API_KEY_REQUIRED).', 503);
    }

    $verifyUrl = "https://developer.myket.ir/api/applications/{$cleanPackage}/purchases/products/{$cleanSku}/tokens/{$cleanToken}";
    
    $ch = curl_init($verifyUrl);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => [
            "X-Access-Token: {$myketApiKey}",
            "Accept: application/json"
        ],
        CURLOPT_TIMEOUT => 15,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2
    ]);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($httpCode !== 200) {
        error_log("[Myket Verify Failed] HTTP {$httpCode} - Package: {$cleanPackage}, SKU: {$cleanSku}, Token: {$cleanToken}");
        throw new Exception('تایید خرید از سمت سرور مایکت با خطا مواجه شد.', 400);
    }

    $resJson = json_decode($response, true);
    $purchaseState = $resJson['purchaseState'] ?? -1;
    if ($purchaseState !== 0) {
        error_log("[Myket Invalid State] purchaseState is not 0 for SKU: {$cleanSku}");
        throw new Exception('وضعیت خرید در سرور مایکت معتبر نیست.', 400);
    }

    // 3. Atomic Replay / Duplicate Prevention & Duration Calculation inside DB Transaction
    try {
        $pdo->beginTransaction();

        $stmtCheck = $pdo->prepare("SELECT id, user_id, plan_name FROM payments WHERE transaction_ref = ? FOR UPDATE");
        $stmtCheck->execute([$cleanToken]);
        $existing = $stmtCheck->fetch();

        if ($existing) {
            $pdo->rollBack();
            throw new Exception('این شناسه خرید قبلاً در سیستم ثبت و تایید شده است (DUPLICATE_TOKEN).', 400);
        }

        // 4. Calculate subscription dates with extension support for existing active subscriptions
        $stmtActive = $pdo->prepare("
            SELECT end_date FROM subscriptions 
            WHERE user_id = ? AND status = 'ACTIVE' AND end_date > NOW() 
            ORDER BY end_date DESC LIMIT 1 FOR UPDATE
        ");
        $stmtActive->execute([$userId]);
        $activeSub = $stmtActive->fetch();

        $now = new DateTime();
        $startDate = $now->format('Y-m-d H:i:s');
        if ($activeSub && !empty($activeSub['end_date'])) {
            $baseDate = new DateTime($activeSub['end_date']);
            if ($baseDate < $now) {
                $baseDate = $now;
            }
        } else {
            $baseDate = $now;
        }

        $endDate = addCalendarMonths($baseDate, $durationMonths)->format('Y-m-d H:i:s');

        $paymentId = 'pay_myket_' . substr(bin2hex(random_bytes(16)), 0, 12);
        $stmtPay = $pdo->prepare("
            INSERT INTO payments (id, user_id, plan_name, amount_toman, payment_status, transaction_ref) 
            VALUES (?, ?, ?, ?, 'COMPLETED', ?)
        ");
        $stmtPay->execute([$paymentId, $userId, $normalizedPlan, $amountToman, $cleanToken]);

        $subId = 'sub_myket_' . substr(bin2hex(random_bytes(16)), 0, 12);

        $stmtSub = $pdo->prepare("
            INSERT INTO subscriptions (id, user_id, plan_name, status, start_date, end_date) 
            VALUES (?, ?, ?, 'ACTIVE', ?, ?)
        ");
        $stmtSub->execute([$subId, $userId, $normalizedPlan, $startDate, $endDate]);

        $stmtUser = $pdo->prepare("UPDATE users SET is_premium = 1, subscription_plan = ? WHERE id = ?");
        $stmtUser->execute([$normalizedPlan, $userId]);

        $pdo->commit();

        return [
            'success' => true,
            'isPremium' => true,
            'subscriptionPlan' => $normalizedPlan,
            'planName' => $normalizedPlan,
            'durationMonths' => $durationMonths,
            'startDate' => $startDate,
            'endDate' => $endDate,
            'message' => 'اشتراک ویژه با موفقیت از طریق مایکت تایید و فعال گردید.'
        ];
    } catch (Exception $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log("[Myket Activation Error] " . $e->getMessage());
        if ($e->getCode() === 400) {
            throw $e;
        }
        throw new Exception('خطا در ذخیره‌سازی و فعال‌سازی اشتراک در دیتابیس.', 500);
    }
}
