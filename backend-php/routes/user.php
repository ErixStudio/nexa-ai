<?php
/**
 * NEXA AI User Router
 */

require_once __DIR__ . '/../middleware/auth.php';
require_once __DIR__ . '/../middleware/response.php';
require_once __DIR__ . '/../services/subscription_service.php';
require_once __DIR__ . '/../config/database.php';

function handleUserRoutes(string $subPath, string $method) {
    $user = authenticateUser(); // User isolated via JWT token!
    $pdo = getDbConnection();

    switch ($subPath) {
        case 'me':
            if ($method !== 'GET') sendErrorResponse('روش درخواست معتبر نیست.', 405);

            $subStatus = checkAndUpdateSubscriptionStatus($user['id']);
            $freeLimit = (int)env('FREE_ANALYSIS_LIMIT', 3);
            
            $today = date('Y-m-d');
            $lastDate = !empty($user['last_analysis_date']) ? substr($user['last_analysis_date'], 0, 10) : null;
            $usedCount = (int)$user['free_analysis_count'];

            // Daily reset if last analysis was on a previous day
            if ($lastDate !== $today && !$subStatus['isPremium']) {
                $usedCount = 0;
                $stmt = $pdo->prepare("UPDATE users SET free_analysis_count = 0, last_analysis_date = ? WHERE id = ?");
                $stmt->execute([$today, $user['id']]);
                $lastDate = $today;
            }

            $remainingFree = $subStatus['isPremium'] ? 'UNLIMITED' : max(0, $freeLimit - $usedCount);

            sendSuccessResponse([
                'user' => [
                    'id' => $user['id'],
                    'email' => $user['email'],
                    'isVerified' => (bool)$user['is_verified'],
                    'isPremium' => $subStatus['isPremium'],
                    'subscriptionPlan' => $subStatus['plan'],
                    'freeAnalysisCount' => $usedCount,
                    'free_analysis_count' => $usedCount,
                    'freeAnalysisLimit' => $freeLimit,
                    'remainingFreeAnalyses' => $remainingFree,
                    'lastAnalysisDate' => $lastDate,
                    'last_analysis_date' => $lastDate,
                    'createdAt' => $user['created_at']
                ]
            ]);
            break;

        case 'analyses':
            if ($method !== 'GET') sendErrorResponse('روش درخواست معتبر نیست.', 405);

            $stmt = $pdo->prepare("
                SELECT id, symbol, timeframe, `signal`, confidence, reasons_json, entry_price, stop_loss, take_profit, risk_level, created_at 
                FROM chart_analyses 
                WHERE user_id = ? 
                ORDER BY created_at DESC 
                LIMIT 100
            ");
            $stmt->execute([$user['id']]);
            $rows = $stmt->fetchAll();

            $formattedAnalyses = array_map(function($row) {
                $reasons = json_decode($row['reasons_json'] ?? '[]', true);
                return [
                    'id' => $row['id'],
                    'symbol' => $row['symbol'],
                    'timeframe' => $row['timeframe'],
                    'signal' => $row['signal'],
                    'confidence' => (int)$row['confidence'],
                    'reasons' => is_array($reasons) ? $reasons : [$row['reasons_json']],
                    'entryPrice' => $row['entry_price'],
                    'stopLoss' => $row['stop_loss'],
                    'takeProfit' => $row['take_profit'],
                    'riskLevel' => $row['risk_level'],
                    'createdAt' => $row['created_at']
                ];
            }, $rows);

            sendSuccessResponse([
                'count' => count($formattedAnalyses),
                'analyses' => $formattedAnalyses
            ]);
            break;

        default:
            sendErrorResponse('مسیر درخواست شده در بخش User یافت نشد.', 404);
            break;
    }
}
