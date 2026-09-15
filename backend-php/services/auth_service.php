<?php
/**
 * NEXA AI Authentication Logic Service
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/jwt_service.php';
require_once __DIR__ . '/email_service.php';

class AuthService {

    public static function register(string $email, string $password): array {
        $cleanEmail = strtolower(trim($email));

        if (!filter_var($cleanEmail, FILTER_VALIDATE_EMAIL)) {
            throw new Exception('ایمیل وارد شده معتبر نیست.', 400);
        }

        if (strlen($password) < 6) {
            throw new Exception('رمز عبور باید حداقل ۶ کاراکتر باشد.', 400);
        }

        $pdo = getDbConnection();

        // Check if email already exists
        $stmt = $pdo->prepare("SELECT id, is_verified FROM users WHERE email = ?");
        $stmt->execute([$cleanEmail]);
        $existingUser = $stmt->fetch();

        if ($existingUser) {
            if ((int)$existingUser['is_verified'] === 1) {
                throw new Exception('این ایمیل قبلاً در سیستم ثبت‌نام شده است.', 400);
            } else {
                // If existing user is unverified, remove incomplete record to allow clean re-register
                $stmtDel = $pdo->prepare("DELETE FROM users WHERE id = ?");
                $stmtDel->execute([$existingUser['id']]);
            }
        }

        $userId = 'usr_' . substr(bin2hex(random_bytes(16)), 0, 16);
        $passwordHash = password_hash($password, PASSWORD_BCRYPT);
        $isOtpEnabled = env('OTP_ENABLED', true) === true;

        if ($isOtpEnabled) {
            // Create unverified user and OTP record in DB transaction first
            $pdo->beginTransaction();
            try {
                // Insert unverified user
                $stmt = $pdo->prepare("
                    INSERT INTO users (id, email, password_hash, is_verified, is_premium, subscription_plan, free_analysis_count) 
                    VALUES (?, ?, ?, 0, 0, 'FREE', 0)
                ");
                $stmt->execute([$userId, $cleanEmail, $passwordHash]);

                // Generate 6-digit OTP
                $otpCode = (string)random_int(100000, 999999);
                $otpHash = JwtService::hashToken($otpCode);
                $expiryMinutes = (int)env('OTP_EXPIRY_MINUTES', 10);
                $expiresAt = date('Y-m-d H:i:s', time() + ($expiryMinutes * 60));
                $otpId = 'otp_' . substr(bin2hex(random_bytes(16)), 0, 16);

                // Invalidate any old OTPs for this email
                $stmtInv = $pdo->prepare("UPDATE otp_requests SET is_used = 1 WHERE email = ?");
                $stmtInv->execute([$cleanEmail]);

                // Store hashed OTP
                $stmtOtp = $pdo->prepare("INSERT INTO otp_requests (id, email, otp_hash, is_used, expires_at) VALUES (?, ?, ?, 0, ?)");
                $stmtOtp->execute([$otpId, $cleanEmail, $otpHash, $expiresAt]);

                $pdo->commit();
            } catch (Exception $e) {
                if ($pdo->inTransaction()) {
                    $pdo->rollBack();
                }
                throw $e;
            }

            // Send Email via SMTP OUTSIDE the DB transaction to prevent table locking
            try {
                EmailService::sendOtpEmail($cleanEmail, $otpCode);
            } catch (Exception $e) {
                // If SMTP delivery fails, clean up the unverified user and OTP to avoid incomplete state
                $stmtCleanUser = $pdo->prepare("DELETE FROM users WHERE id = ?");
                $stmtCleanUser->execute([$userId]);

                $stmtCleanOtp = $pdo->prepare("DELETE FROM otp_requests WHERE email = ?");
                $stmtCleanOtp->execute([$cleanEmail]);

                error_log("[Register Error] Email delivery failed for {$cleanEmail}: EMAIL_SEND_FAILED");
                throw new Exception("ارسال ایمیل تایید با خطا مواجه شد. (EMAIL_SEND_FAILED)", 400);
            }

            return [
                'success' => true,
                'requireOtp' => true,
                'require_otp' => true,
                'email' => $cleanEmail,
                'message' => 'ثبت‌نام انجام شد. کد تایید به ایمیل شما ارسال گردید.'
            ];
        }

        // Direct Login when OTP_ENABLED = false
        $stmt = $pdo->prepare("
            INSERT INTO users (id, email, password_hash, is_verified, is_premium, subscription_plan, free_analysis_count) 
            VALUES (?, ?, ?, 1, 0, 'FREE', 0)
        ");
        $stmt->execute([$userId, $cleanEmail, $passwordHash]);

        $accessToken = JwtService::generateToken(['userId' => $userId, 'email' => $cleanEmail], (int)env('JWT_EXPIRES_IN', 604800));
        $refreshToken = JwtService::generateToken(['userId' => $userId, 'type' => 'refresh', 'nonce' => bin2hex(random_bytes(8))], (int)env('REFRESH_TOKEN_EXPIRES_IN', 2592000));

        self::saveRefreshToken($userId, $refreshToken);

        return [
            'success' => true,
            'token' => $accessToken,
            'refreshToken' => $refreshToken,
            'refresh_token' => $refreshToken,
            'requireOtp' => false,
            'require_otp' => false,
            'user' => [
                'id' => $userId,
                'email' => $cleanEmail,
                'isVerified' => true,
                'is_verified' => true,
                'isPremium' => false,
                'is_premium' => false,
                'subscriptionPlan' => 'FREE',
                'subscription_plan' => 'FREE',
                'freeAnalysisCount' => 0,
                'free_analysis_count' => 0
            ]
        ];
    }

    public static function resendOtp(string $email): array {
        $cleanEmail = strtolower(trim($email));
        if (!filter_var($cleanEmail, FILTER_VALIDATE_EMAIL)) {
            throw new Exception('ایمیل معتبر نیست.', 400);
        }

        $pdo = getDbConnection();

        // Check if user exists
        $stmt = $pdo->prepare("SELECT id, is_verified FROM users WHERE email = ?");
        $stmt->execute([$cleanEmail]);
        $user = $stmt->fetch();

        if (!$user) {
            throw new Exception('کاربری با این ایمیل یافت نشد.', 404);
        }

        if ((int)$user['is_verified'] === 1) {
            throw new Exception('این حساب کاربری قبلاً تایید شده است.', 400);
        }

        // Rate limit check (cooldown: 60 seconds)
        $stmtRate = $pdo->prepare("SELECT created_at FROM otp_requests WHERE email = ? ORDER BY created_at DESC LIMIT 1");
        $stmtRate->execute([$cleanEmail]);
        $lastOtp = $stmtRate->fetch();

        if ($lastOtp && !empty($lastOtp['created_at'])) {
            $elapsedSeconds = time() - strtotime($lastOtp['created_at']);
            $cooldown = 60;
            if ($elapsedSeconds < $cooldown) {
                $secondsLeft = $cooldown - $elapsedSeconds;
                throw new Exception("لطفاً {$secondsLeft} ثانیه دیگر برای دریافت مجدد کد تایید صبر کنید.", 429);
            }
        }

        $otpCode = (string)random_int(100000, 999999);
        $otpHash = JwtService::hashToken($otpCode);
        $expiryMinutes = (int)env('OTP_EXPIRY_MINUTES', 10);
        $expiresAt = date('Y-m-d H:i:s', time() + ($expiryMinutes * 60));
        $otpId = 'otp_' . substr(bin2hex(random_bytes(16)), 0, 16);

        // Invalidate older OTPs
        $stmtInv = $pdo->prepare("UPDATE otp_requests SET is_used = 1 WHERE email = ?");
        $stmtInv->execute([$cleanEmail]);

        // Insert new OTP request
        $stmtOtp = $pdo->prepare("INSERT INTO otp_requests (id, email, otp_hash, is_used, expires_at) VALUES (?, ?, ?, 0, ?)");
        $stmtOtp->execute([$otpId, $cleanEmail, $otpHash, $expiresAt]);

        // Send Email via SMTP
        try {
            EmailService::sendOtpEmail($cleanEmail, $otpCode);
        } catch (Exception $e) {
            // Delete newly created OTP request if delivery fails
            $stmtClean = $pdo->prepare("DELETE FROM otp_requests WHERE id = ?");
            $stmtClean->execute([$otpId]);

            error_log("[Resend OTP Error] Email delivery failed for {$cleanEmail}: EMAIL_SEND_FAILED");
            throw new Exception("ارسال ایمیل تایید با خطا مواجه شد. (EMAIL_SEND_FAILED)", 400);
        }

        return [
            'success' => true,
            'message' => 'کد تایید جدید به ایمیل شما ارسال شد.'
        ];
    }

    public static function sendOtp(string $email): array {
        return self::resendOtp($email);
    }

    public static function verifyOtp(string $email, string $otpCode): array {
        $cleanEmail = strtolower(trim($email));
        $cleanCode = trim($otpCode);

        if (empty($cleanEmail) || empty($cleanCode)) {
            throw new Exception('ایمیل و کد تایید الزامی است.', 400);
        }

        $incomingHash = JwtService::hashToken($cleanCode);

        $pdo = getDbConnection();
        $stmt = $pdo->prepare("
            SELECT id, expires_at FROM otp_requests 
            WHERE email = ? AND otp_hash = ? AND is_used = 0 
            ORDER BY created_at DESC LIMIT 1
        ");
        $stmt->execute([$cleanEmail, $incomingHash]);
        $row = $stmt->fetch();

        if (!$row) {
            throw new Exception('کد تایید وارد شده اشتباه است.', 400);
        }

        if (strtotime($row['expires_at']) < time()) {
            throw new Exception('کد تایید منقضی شده است. لطفاً کد جدید درخواست کنید.', 400);
        }

        // Mark OTP as used
        $stmtUsed = $pdo->prepare("UPDATE otp_requests SET is_used = 1 WHERE id = ?");
        $stmtUsed->execute([$row['id']]);

        // Invalidate all other OTPs for this email
        $stmtInv = $pdo->prepare("UPDATE otp_requests SET is_used = 1 WHERE email = ?");
        $stmtInv->execute([$cleanEmail]);

        // Mark user verified
        $stmtUser = $pdo->prepare("UPDATE users SET is_verified = 1 WHERE email = ?");
        $stmtUser->execute([$cleanEmail]);

        // Fetch user record
        $stmtFetch = $pdo->prepare("SELECT id, email, is_verified, is_premium, subscription_plan, free_analysis_count FROM users WHERE email = ?");
        $stmtFetch->execute([$cleanEmail]);
        $user = $stmtFetch->fetch();

        if (!$user) {
            throw new Exception('کاربر یافت نشد.', 404);
        }

        require_once __DIR__ . '/subscription_service.php';
        $subStatus = checkAndUpdateSubscriptionStatus($user['id']);

        $accessToken = JwtService::generateToken(['userId' => $user['id'], 'email' => $user['email']], (int)env('JWT_EXPIRES_IN', 604800));
        $refreshToken = JwtService::generateToken(['userId' => $user['id'], 'type' => 'refresh', 'nonce' => bin2hex(random_bytes(8))], (int)env('REFRESH_TOKEN_EXPIRES_IN', 2592000));

        self::saveRefreshToken($user['id'], $refreshToken);

        return [
            'success' => true,
            'token' => $accessToken,
            'refreshToken' => $refreshToken,
            'refresh_token' => $refreshToken,
            'requireOtp' => false,
            'require_otp' => false,
            'user' => [
                'id' => $user['id'],
                'email' => $user['email'],
                'isVerified' => true,
                'is_verified' => true,
                'isPremium' => $subStatus['isPremium'],
                'is_premium' => $subStatus['isPremium'],
                'subscriptionPlan' => $subStatus['plan'],
                'subscription_plan' => $subStatus['plan'],
                'freeAnalysisCount' => (int)$user['free_analysis_count'],
                'free_analysis_count' => (int)$user['free_analysis_count']
            ]
        ];
    }

    public static function login(string $email, string $password): array {
        $cleanEmail = strtolower(trim($email));
        $pdo = getDbConnection();

        $stmt = $pdo->prepare("SELECT id, email, password_hash, is_verified, is_premium, subscription_plan, free_analysis_count FROM users WHERE email = ?");
        $stmt->execute([$cleanEmail]);
        $user = $stmt->fetch();

        if (!$user || !password_verify($password, $user['password_hash'])) {
            throw new Exception('ایمیل یا رمز عبور اشتباه است.', 401);
        }

        require_once __DIR__ . '/subscription_service.php';
        $subStatus = checkAndUpdateSubscriptionStatus($user['id']);

        $accessToken = JwtService::generateToken(['userId' => $user['id'], 'email' => $cleanEmail], (int)env('JWT_EXPIRES_IN', 604800));
        $refreshToken = JwtService::generateToken(['userId' => $user['id'], 'type' => 'refresh', 'nonce' => bin2hex(random_bytes(8))], (int)env('REFRESH_TOKEN_EXPIRES_IN', 2592000));

        self::saveRefreshToken($user['id'], $refreshToken);

        return [
            'success' => true,
            'token' => $accessToken,
            'refreshToken' => $refreshToken,
            'refresh_token' => $refreshToken,
            'requireOtp' => false,
            'require_otp' => false,
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
                'free_analysis_count' => (int)$user['free_analysis_count']
            ]
        ];
    }

    public static function refresh(string $refreshToken): array {
        $payload = JwtService::verifyToken($refreshToken);

        if (!$payload || !isset($payload['userId']) || ($payload['type'] ?? '') !== 'refresh') {
            throw new Exception('توکن بازنشانی منقضی یا نامعتبر است.', 401);
        }

        $pdo = getDbConnection();
        $incomingHash = JwtService::hashToken($refreshToken);

        $stmt = $pdo->prepare("SELECT id, user_id FROM refresh_tokens WHERE token_hash = ? AND is_revoked = 0 AND expires_at > NOW()");
        $stmt->execute([$incomingHash]);
        $row = $stmt->fetch();

        if (!$row) {
            throw new Exception('توکن بازنشانی باطل شده یا یافت نشد.', 401);
        }

        // Revoke old token (Rotation)
        $stmt = $pdo->prepare("UPDATE refresh_tokens SET is_revoked = 1 WHERE id = ?");
        $stmt->execute([$row['id']]);

        // Get user details
        $stmt = $pdo->prepare("SELECT email FROM users WHERE id = ?");
        $stmt->execute([$row['user_id']]);
        $user = $stmt->fetch();

        if (!$user) {
            throw new Exception('کاربر یافت نشد.', 401);
        }

        $newAccessToken = JwtService::generateToken(['userId' => $row['user_id'], 'email' => $user['email']], (int)env('JWT_EXPIRES_IN', 604800));
        $newRefreshToken = JwtService::generateToken(['userId' => $row['user_id'], 'type' => 'refresh', 'nonce' => bin2hex(random_bytes(8))], (int)env('REFRESH_TOKEN_EXPIRES_IN', 2592000));

        self::saveRefreshToken($row['user_id'], $newRefreshToken);

        return [
            'success' => true,
            'token' => $newAccessToken,
            'refreshToken' => $newRefreshToken,
            'refresh_token' => $newRefreshToken
        ];
    }

    public static function saveRefreshToken(string $userId, string $refreshToken): void {
        $pdo = getDbConnection();
        $tokenId = 'rt_' . substr(bin2hex(random_bytes(16)), 0, 16);
        $tokenHash = JwtService::hashToken($refreshToken);
        $expiresAt = date('Y-m-d H:i:s', time() + (int)env('REFRESH_TOKEN_EXPIRES_IN', 2592000));

        $stmt = $pdo->prepare("INSERT INTO refresh_tokens (id, user_id, token_hash, is_revoked, expires_at) VALUES (?, ?, ?, 0, ?)");
        $stmt->execute([$tokenId, $userId, $tokenHash, $expiresAt]);
    }
}
