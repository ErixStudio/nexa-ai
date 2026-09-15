<?php
/**
 * NEXA AI Analysis Router
 */

require_once __DIR__ . '/../middleware/auth.php';
require_once __DIR__ . '/../middleware/response.php';
require_once __DIR__ . '/../services/subscription_service.php';
require_once __DIR__ . '/../services/gemini_service.php';
require_once __DIR__ . '/../config/database.php';

function handleAnalysisRoutes($subPathOrSegments, string $method) {
    $user = authenticateUser();
    $pdo = getDbConnection();

    if (is_array($subPathOrSegments)) {
        $first = strtolower($subPathOrSegments[0] ?? '');
        $second = strtolower($subPathOrSegments[1] ?? '');
        $id = $subPathOrSegments[2] ?? '';

        if ($first === 'analyze' && empty($second)) {
            $subPath = 'analyze';
        } else {
            $subPath = $second ?: $first;
        }
    } else {
        $subPath = strtolower((string)$subPathOrSegments);
        $id = '';
    }

    if ($method === 'POST' && ($subPath === 'analyze' || $subPath === '' || $subPath === '/')) {
        $input = getJsonInput();

        $symbol = trim($input['symbol'] ?? '');
        $timeframe = trim($input['timeframe'] ?? '');
        $base64Image = trim($input['base64Image'] ?? $input['image'] ?? '');
        $mimeType = trim($input['mimeType'] ?? 'image/jpeg');

        // Input validation
        if (empty($symbol) || strlen($symbol) > 32) {
            sendErrorResponse('نماد معاملاتی (Symbol) معتبر نیست.', 400);
        }

        if (empty($timeframe) || strlen($timeframe) > 16) {
            sendErrorResponse('تایم‌فریم (Timeframe) معتبر نیست.', 400);
        }

        if (empty($base64Image) || strlen($base64Image) < 50) {
            sendErrorResponse('تصویر چارت ارسال نشده یا فرمت آن ناخوانا است.', 400);
        }

        // Check dynamic subscription status
        $subStatus = checkAndUpdateSubscriptionStatus($user['id']);
        $freeLimit = (int)env('FREE_ANALYSIS_LIMIT', 3);
        $isPremium = $subStatus['isPremium'];

        $reservedFree = false;

        // Atomic Quota Reservation for Free Users
        if (!$isPremium) {
            try {
                $pdo->beginTransaction();

                $stmt = $pdo->prepare("SELECT free_analysis_count, last_analysis_date FROM users WHERE id = ? FOR UPDATE");
                $stmt->execute([$user['id']]);
                $uRow = $stmt->fetch();

                $today = date('Y-m-d');
                $lastDate = $uRow && !empty($uRow['last_analysis_date']) ? substr($uRow['last_analysis_date'], 0, 10) : null;
                $currentUsed = $uRow ? (int)$uRow['free_analysis_count'] : 0;

                // Daily Reset Check: If last analysis date is different from today, reset quota to 0
                if ($lastDate !== $today) {
                    $currentUsed = 0;
                    $stmt = $pdo->prepare("UPDATE users SET free_analysis_count = 0, last_analysis_date = ? WHERE id = ?");
                    $stmt->execute([$today, $user['id']]);
                }

                if ($currentUsed >= $freeLimit) {
                    $pdo->rollBack();
                    sendErrorResponse(
                        'سقف تحلیلهای رایگان شما به پایان رسیده است. برای ادامه، اشتراک ویژه تهیه کنید.',
                        403,
                        'QUOTA_EXCEEDED',
                        [
                            'quotaExceeded' => true,
                            'freeLimit' => $freeLimit,
                            'usedCount' => $currentUsed,
                            'lastAnalysisDate' => $today,
                            'last_analysis_date' => $today
                        ]
                    );
                }

                // Increment atomically and update last_analysis_date
                $stmt = $pdo->prepare("UPDATE users SET free_analysis_count = ?, last_analysis_date = ? WHERE id = ?");
                $stmt->execute([$currentUsed + 1, $today, $user['id']]);

                $pdo->commit();
                $reservedFree = true;
            } catch (Exception $e) {
                if ($pdo->inTransaction()) $pdo->rollBack();
                sendErrorResponse('خطا در بررسی سقف اعتبارات رایگان.', 500);
            }
        }

        // Call Gemini Vision Service
        try {
            $result = analyzeChartWithGemini($symbol, $timeframe, $base64Image, $mimeType);
        } catch (Exception $e) {
            // Restore quota if reserved
            if ($reservedFree) {
                $stmt = $pdo->prepare("UPDATE users SET free_analysis_count = GREATEST(0, free_analysis_count - 1) WHERE id = ?");
                $stmt->execute([$user['id']]);
            }
            sendErrorResponse($e->getMessage(), $e->getCode() ?: 500);
        }

        // Handle Non-Chart Image
        if (!$result['isChart']) {
            if ($reservedFree) {
                $stmt = $pdo->prepare("UPDATE users SET free_analysis_count = GREATEST(0, free_analysis_count - 1) WHERE id = ?");
                $stmt->execute([$user['id']]);
            }

            sendErrorResponse(
                $result['invalidReason'] ?? 'تصویر ارسال شده یک چارت معاملاتی معتبر نیست.',
                400,
                'INVALID_CHART_IMAGE',
                [
                    'analysis' => [
                        'isChart' => false,
                        'is_chart' => false,
                        'invalidReason' => $result['invalidReason'] ?? 'تصویر ارسال شده یک چارت معاملاتی معتبر نیست.',
                        'invalid_reason' => $result['invalidReason'] ?? 'تصویر ارسال شده یک چارت معاملاتی معتبر نیست.'
                    ]
                ]
            );
        }

        // Save valid analysis record
        $analysisId = 'ana_' . substr(bin2hex(random_bytes(16)), 0, 16);
        $reasonsJson = json_encode($result['reasons'], JSON_UNESCAPED_UNICODE);

        try {
            $stmt = $pdo->prepare("
                INSERT INTO chart_analyses 
                (id, user_id, symbol, timeframe, `signal`, confidence, reasons_json, entry_price, stop_loss, take_profit, risk_level) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ");
            $stmt->execute([
                $analysisId,
                $user['id'],
                strtoupper($symbol),
                $timeframe,
                $result['signal'],
                $result['confidence'],
                $reasonsJson,
                $result['entry'],
                $result['stopLoss'],
                $result['takeProfit'],
                $result['riskLevel']
            ]);
        } catch (Exception $e) {
            // Restore quota if reserved
            if ($reservedFree) {
                $stmt = $pdo->prepare("UPDATE users SET free_analysis_count = GREATEST(0, free_analysis_count - 1) WHERE id = ?");
                $stmt->execute([$user['id']]);
            }
            sendErrorResponse('خطا در ذخیره‌سازی نتیجه تحلیل در پایگاه داده.', 500);
        }

        // Get final remaining free analysis count and last_analysis_date
        $stmt = $pdo->prepare("SELECT free_analysis_count, last_analysis_date FROM users WHERE id = ?");
        $stmt->execute([$user['id']]);
        $finalRow = $stmt->fetch();
        $finalFreeCount = $finalRow ? (int)$finalRow['free_analysis_count'] : 0;
        $finalLastDate = $finalRow && !empty($finalRow['last_analysis_date']) ? $finalRow['last_analysis_date'] : date('Y-m-d');
        $remainingFree = $isPremium ? 'UNLIMITED' : max(0, $freeLimit - $finalFreeCount);

        sendSuccessResponse([
            'analysis' => [
                'id' => $analysisId,
                'symbol' => strtoupper($symbol),
                'timeframe' => $timeframe,
                'signal' => $result['signal'],
                'confidence' => $result['confidence'],
                'entry' => $result['entry'],
                'entry_price' => $result['entry'],
                'stopLoss' => $result['stopLoss'],
                'stop_loss' => $result['stopLoss'],
                'takeProfit' => $result['takeProfit'],
                'take_profit' => $result['takeProfit'],
                'reasons' => $result['reasons'],
                'riskLevel' => $result['riskLevel'],
                'risk_level' => $result['riskLevel'],
                'isChart' => true,
                'is_chart' => true
            ],
            'remainingFreeAnalyses' => $remainingFree,
            'freeAnalysisCount' => $finalFreeCount,
            'free_analysis_count' => $finalFreeCount,
            'lastAnalysisDate' => $finalLastDate,
            'last_analysis_date' => $finalLastDate
        ]);
    }

    if ($method === 'DELETE' && ($subPath === 'history' || $subPath === 'analyses') && !empty($id)) {
        $stmt = $pdo->prepare("DELETE FROM chart_analyses WHERE id = ? AND user_id = ?");
        $stmt->execute([$id, $user['id']]);

        if ($stmt->rowCount() > 0) {
            sendSuccessResponse(['message' => 'تحلیل با موفقیت حذف گردید.']);
        } else {
            sendErrorResponse('تحلیل مورد نظر یافت نشد یا دسترسی حذف آن وجود ندارد.', 404, 'NOT_FOUND');
        }
    }

    if ($method === 'GET' && ($subPath === 'history' || $subPath === 'analyses')) {
        $stmt = $pdo->prepare("
            SELECT id, user_id, symbol, timeframe, `signal`, confidence, reasons_json, entry_price, stop_loss, take_profit, risk_level, created_at 
            FROM chart_analyses 
            WHERE user_id = ? 
            ORDER BY created_at DESC
        ");
        $stmt->execute([$user['id']]);
        $rows = $stmt->fetchAll();

        $formattedList = array_map(function($r) use ($user) {
            $reasons = json_decode($r['reasons_json'] ?? '[]', true);
            $itemUserId = $r['user_id'] ?? $user['id'];
            return [
                'id' => $r['id'],
                'user_id' => $itemUserId,
                'userId' => $itemUserId,
                'symbol' => $r['symbol'],
                'timeframe' => $r['timeframe'],
                'signal' => $r['signal'],
                'confidence' => (int)$r['confidence'],
                'entry' => $r['entry_price'],
                'entry_price' => $r['entry_price'],
                'entryPrice' => $r['entry_price'],
                'stopLoss' => $r['stop_loss'],
                'stop_loss' => $r['stop_loss'],
                'takeProfit' => $r['take_profit'],
                'take_profit' => $r['take_profit'],
                'reasons' => is_array($reasons) ? $reasons : [$r['reasons_json']],
                'riskLevel' => $r['risk_level'],
                'risk_level' => $r['risk_level'],
                'createdAt' => $r['created_at'],
                'created_at' => $r['created_at']
            ];
        }, $rows);

        sendSuccessResponse([
            'history' => $formattedList,
            'analyses' => $formattedList
        ]);
    }

    sendErrorResponse('مسیر درخواست شده در بخش Analyze یافت نشد.', 404);
}
