<?php
/**
 * NEXA AI Authentication Guard Middleware
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../services/jwt_service.php';
require_once __DIR__ . '/response.php';

function authenticateUser(): array {
    $authHeader = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';

    if (empty($authHeader) && function_exists('apache_request_headers')) {
        $headers = apache_request_headers();
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
    }

    if (empty($authHeader) || !preg_match('/Bearer\s+(.*)$/i', $authHeader, $matches)) {
        sendErrorResponse('توکن امنیتی (Bearer Token) یافت نشد. لطفاً ابتدا وارد حساب شوید.', 401, 'UNAUTHORIZED');
    }

    $token = trim($matches[1]);
    $payload = JwtService::verifyToken($token);

    if (!$payload || !isset($payload['userId'])) {
        sendErrorResponse('توکن امنیتی شما منقضی شده یا نامعتبر است.', 401, 'INVALID_TOKEN');
    }

    $pdo = getDbConnection();
    $stmt = $pdo->prepare("SELECT id, email, is_verified, is_premium, subscription_plan, free_analysis_count, last_analysis_date, created_at FROM users WHERE id = ?");
    $stmt->execute([$payload['userId']]);
    $user = $stmt->fetch();

    if (!$user) {
        sendErrorResponse('کاربر صاحب این توکن در سیستم یافت نشد.', 401, 'USER_NOT_FOUND');
    }

    return $user;
}
