<?php
/**
 * NEXA AI Subscription Expiration & Synchronization Service
 */

require_once __DIR__ . '/../config/database.php';

function checkAndUpdateSubscriptionStatus(string $userId): array {
    $pdo = getDbConnection();

    try {
        // 1. Expire subscriptions past end_date
        $stmt = $pdo->prepare("
            UPDATE subscriptions 
            SET status = 'EXPIRED' 
            WHERE user_id = ? AND status = 'ACTIVE' AND end_date IS NOT NULL AND end_date <= NOW()
        ");
        $stmt->execute([$userId]);

        // 2. Query active subscription
        $stmt = $pdo->prepare("
            SELECT id, plan_name, status, start_date, end_date 
            FROM subscriptions 
            WHERE user_id = ? AND status = 'ACTIVE' AND (end_date IS NULL OR end_date > NOW()) 
            ORDER BY end_date DESC LIMIT 1
        ");
        $stmt->execute([$userId]);
        $activeSub = $stmt->fetch();

        if ($activeSub) {
            // Update user premium flag
            $updateStmt = $pdo->prepare("UPDATE users SET is_premium = 1, subscription_plan = ? WHERE id = ?");
            $updateStmt->execute([$activeSub['plan_name'], $userId]);

            return [
                'isPremium' => true,
                'plan' => $activeSub['plan_name'],
                'status' => 'ACTIVE',
                'startDate' => $activeSub['start_date'],
                'endDate' => $activeSub['end_date']
            ];
        } else {
            // Demote user to FREE
            $updateStmt = $pdo->prepare("UPDATE users SET is_premium = 0, subscription_plan = 'FREE' WHERE id = ?");
            $updateStmt->execute([$userId]);

            return [
                'isPremium' => false,
                'plan' => 'FREE',
                'status' => 'INACTIVE',
                'startDate' => null,
                'endDate' => null
            ];
        }
    } catch (PDOException $e) {
        error_log("[SubscriptionService Error] " . $e->getMessage());
        return [
            'isPremium' => false,
            'plan' => 'FREE',
            'status' => 'INACTIVE',
            'startDate' => null,
            'endDate' => null
        ];
    }
}
