<?php
/**
 * NEXA AI Payments Router
 */

require_once __DIR__ . '/../middleware/auth.php';
require_once __DIR__ . '/../middleware/response.php';
require_once __DIR__ . '/../services/payment_service.php';
require_once __DIR__ . '/../config/database.php';

function handlePaymentsRoutes(string $subPath, string $method) {
    $cleanPath = trim($subPath, '/');

    // 0. GET /api/payments/pricing or /api/payment/pricing
    if ($method === 'GET' && ($cleanPath === 'pricing' || $cleanPath === 'plans')) {
        sendSuccessResponse([
            'plans' => [
                [
                    'plan_key' => 'MONTHLY',
                    'name' => 'اشتراک یک‌ماهه Pro',
                    'price_toman' => 259000,
                    'duration_months' => 1,
                    'duration_days' => 30,
                    'discount_percent' => 0,
                    'myket_sku' => 'nexa_monthly'
                ],
                [
                    'plan_key' => 'QUARTERLY',
                    'name' => 'اشتراک سه‌ماهه Pro',
                    'price_toman' => 599000,
                    'duration_months' => 3,
                    'duration_days' => 90,
                    'discount_percent' => 23,
                    'myket_sku' => 'nexa_quarterly'
                ],
                [
                    'plan_key' => 'SEMI_ANNUAL',
                    'name' => 'اشتراک شش‌ماهه Pro',
                    'price_toman' => 999000,
                    'duration_months' => 6,
                    'duration_days' => 180,
                    'discount_percent' => 36,
                    'myket_sku' => 'nexa_semiannual'
                ],
                [
                    'plan_key' => 'ANNUAL',
                    'name' => 'اشتراک یک‌ساله VIP',
                    'price_toman' => 1599000,
                    'duration_months' => 12,
                    'duration_days' => 365,
                    'discount_percent' => 49,
                    'myket_sku' => 'nexa_annual'
                ]
            ]
        ]);
    }

    $user = authenticateUser();
    $pdo = getDbConnection();

    // 1. POST /api/payments/verify-myket -> Server-side verification for Myket In-App Purchase
    if ($method === 'POST' && ($cleanPath === 'verify-myket' || $cleanPath === 'verify_myket')) {
        $input = getJsonInput();
        $packageName = $input['packageName'] ?? $input['package_name'] ?? env('MYKET_PACKAGE_NAME', 'com.aistudio.nexaai.trading');
        $sku = $input['sku'] ?? $input['productId'] ?? $input['product_id'] ?? '';
        $purchaseToken = $input['purchaseToken'] ?? $input['token'] ?? $input['purchase_token'] ?? '';
        $planKey = $input['planKey'] ?? $input['plan'] ?? 'MONTHLY';

        try {
            $result = verifyMyketPurchase($user['id'], $packageName, $sku, $purchaseToken, $planKey);
            sendSuccessResponse($result);
        } catch (Exception $e) {
            sendErrorResponse($e->getMessage(), $e->getCode() ?: 400);
        }
    }

    // 2. POST /api/payments/checkout
    if ($method === 'POST' && ($cleanPath === 'checkout')) {
        $input = getJsonInput();
        $planKey = trim($input['plan'] ?? 'MONTHLY');
        sendSuccessResponse([
            'success' => true,
            'message' => 'پرداخت از طریق درگاه درون‌برنامه‌ای مایکت آماده است.',
            'order_id' => 'ord_' . substr(bin2hex(random_bytes(16)), 0, 10),
            'plan' => $planKey
        ]);
    }

    // 3. POST /api/payments -> Create payment intent
    if ($method === 'POST' && ($cleanPath === '' || $cleanPath === '/')) {
        $input = getJsonInput();
        $planName = trim($input['planName'] ?? 'MONTHLY');
        $amountToman = is_numeric($input['amountToman'] ?? null) ? (int)$input['amountToman'] : 259000;
        $transactionRef = 'INTENT_' . strtoupper(bin2hex(random_bytes(6)));

        $payment = createPaymentIntent($user['id'], $planName, $amountToman, $transactionRef);

        sendSuccessResponse([
            'message' => 'درخواست پرداخت ثبت گردید.',
            'payment' => $payment
        ], 201);
    }

    // 2. GET /api/payments -> List user's payments
    if ($method === 'GET' && ($subPath === '' || $subPath === '/')) {
        $stmt = $pdo->prepare("
            SELECT id, plan_name, amount_toman, payment_status, transaction_ref, created_at 
            FROM payments 
            WHERE user_id = ? 
            ORDER BY created_at DESC
        ");
        $stmt->execute([$user['id']]);
        $rows = $stmt->fetchAll();

        $paymentsList = array_map(function($r) {
            return [
                'id' => $r['id'],
                'planName' => $r['plan_name'],
                'amountToman' => (int)$r['amount_toman'],
                'status' => $r['payment_status'],
                'transactionRef' => $r['transaction_ref'],
                'createdAt' => $r['created_at']
            ];
        }, $rows);

        sendSuccessResponse(['payments' => $paymentsList]);
    }

    // 3. GET /api/payments/:id -> Get specific payment
    if ($method === 'GET' && !empty($subPath) && $subPath !== '/') {
        $paymentId = trim($subPath, '/');

        $stmt = $pdo->prepare("
            SELECT id, plan_name, amount_toman, payment_status, transaction_ref, created_at 
            FROM payments 
            WHERE id = ? AND user_id = ?
        ");
        $stmt->execute([$paymentId, $user['id']]);
        $r = $stmt->fetch();

        if (!$r) {
            sendErrorResponse('اطلاعات این پرداخت یافت نشد.', 404);
        }

        sendSuccessResponse([
            'payment' => [
                'id' => $r['id'],
                'planName' => $r['plan_name'],
                'amountToman' => (int)$r['amount_toman'],
                'status' => $r['payment_status'],
                'transactionRef' => $r['transaction_ref'],
                'createdAt' => $r['created_at']
            ]
        ]);
    }

    sendErrorResponse('مسیر درخواست شده در بخش Payments یافت نشد.', 404);
}
