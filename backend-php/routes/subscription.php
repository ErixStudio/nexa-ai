<?php
/**
 * NEXA AI Subscription Router
 */

require_once __DIR__ . '/../middleware/auth.php';
require_once __DIR__ . '/../middleware/response.php';
require_once __DIR__ . '/../services/subscription_service.php';

function handleSubscriptionRoutes(string $subPath, string $method) {
    if ($method !== 'GET') {
        sendErrorResponse('روش درخواست معتبر نیست.', 405);
    }

    $user = authenticateUser();
    $subStatus = checkAndUpdateSubscriptionStatus($user['id']);

    $freeLimit = (int)env('FREE_ANALYSIS_LIMIT', 3);
    $usedCount = (int)$user['free_analysis_count'];

    sendSuccessResponse([
        'isPremium' => $subStatus['isPremium'],
        'plan' => $subStatus['plan'],
        'status' => $subStatus['status'],
        'subscriptionDetails' => $subStatus['isPremium'] ? [
            'startDate' => $subStatus['startDate'],
            'endDate' => $subStatus['endDate']
        ] : null,
        'freeUsage' => [
            'limit' => $freeLimit,
            'used' => $usedCount,
            'remaining' => $subStatus['isPremium'] ? 'UNLIMITED' : max(0, $freeLimit - $usedCount)
        ]
    ]);
}
