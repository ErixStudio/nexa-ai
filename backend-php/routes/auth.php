<?php
/**
 * NEXA AI Auth Router
 */

require_once __DIR__ . '/../services/auth_service.php';
require_once __DIR__ . '/../middleware/auth.php';
require_once __DIR__ . '/../middleware/response.php';
require_once __DIR__ . '/../services/subscription_service.php';

function handleAuthRoutes(string $subPath, string $method) {
    $input = getJsonInput();

    switch ($subPath) {
        case 'register':
            if ($method !== 'POST') sendErrorResponse('روش درخواست معتبر نیست.', 405);
            
            $email = $input['email'] ?? '';
            $password = $input['password'] ?? '';
            
            try {
                $res = AuthService::register($email, $password);
                sendSuccessResponse(array_merge(['message' => 'ثبت‌نام با موفقیت انجام شد.'], $res), 201);
            } catch (Exception $e) {
                sendErrorResponse($e->getMessage(), $e->getCode() ?: 400);
            }
            break;

        case 'login':
            if ($method !== 'POST') sendErrorResponse('روش درخواست معتبر نیست.', 405);

            $email = $input['email'] ?? '';
            $password = $input['password'] ?? '';

            try {
                $res = AuthService::login($email, $password);
                sendSuccessResponse(array_merge(['message' => 'ورود با موفقیت انجام شد.'], $res));
            } catch (Exception $e) {
                sendErrorResponse($e->getMessage(), $e->getCode() ?: 401);
            }
            break;

        case 'refresh':
            if ($method !== 'POST') sendErrorResponse('روش درخواست معتبر نیست.', 405);

            $refreshToken = $input['refreshToken'] ?? $input['refresh_token'] ?? '';
            if (empty($refreshToken)) sendErrorResponse('توکن بازنشانی (Refresh Token) الزامی است.', 400);

            try {
                $res = AuthService::refresh($refreshToken);
                sendSuccessResponse($res);
            } catch (Exception $e) {
                sendErrorResponse($e->getMessage(), 401);
            }
            break;

        case 'send-otp':
        case 'resend-otp':
            if ($method !== 'POST') sendErrorResponse('روش درخواست معتبر نیست.', 405);

            $email = $input['email'] ?? '';
            try {
                $res = AuthService::sendOtp($email);
                sendSuccessResponse($res);
            } catch (Exception $e) {
                sendErrorResponse($e->getMessage(), $e->getCode() ?: 400);
            }
            break;

        case 'verify-otp':
            if ($method !== 'POST') sendErrorResponse('روش درخواست معتبر نیست.', 405);

            $email = $input['email'] ?? '';
            $otpCode = $input['otpCode'] ?? $input['otp_code'] ?? $input['otp'] ?? '';

            try {
                $res = AuthService::verifyOtp($email, $otpCode);
                sendSuccessResponse(array_merge(['message' => 'حساب کاربری با موفقیت تایید گردید.'], $res));
            } catch (Exception $e) {
                sendErrorResponse($e->getMessage(), $e->getCode() ?: 400);
            }
            break;

        case 'me':
            if ($method !== 'GET') sendErrorResponse('روش درخواست معتبر نیست.', 405);

            $user = authenticateUser();
            $subStatus = checkAndUpdateSubscriptionStatus($user['id']);

            sendSuccessResponse([
                'user' => [
                    'id' => $user['id'],
                    'email' => $user['email'],
                    'isVerified' => (bool)$user['is_verified'],
                    'is_verified' => (bool)$user['is_verified'],
                    'isPremium' => $subStatus['isPremium'],
                    'is_premium' => $subStatus['isPremium'],
                    'subscriptionPlan' => $subStatus['plan'],
                    'subscription_plan' => $subStatus['plan'],
                    'freeAnalysisCount' => (int)$user['free_analysis_count'],
                    'free_analysis_count' => (int)$user['free_analysis_count'],
                    'createdAt' => $user['created_at'],
                    'created_at' => $user['created_at']
                ]
            ]);
            break;

        default:
            sendErrorResponse('مسیر درخواست شده در بخش Auth یافت نشد.', 404);
            break;
    }
}
