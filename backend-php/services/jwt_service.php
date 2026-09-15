<?php
/**
 * NEXA AI Pure PHP Lightweight JWT Generator & Validator
 */

require_once __DIR__ . '/../config/config.php';

class JwtService {

    private static function base64UrlEncode(string $data): string {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    private static function base64UrlDecode(string $data): string {
        return base64_decode(strtr($data, '-_', '+/'));
    }

    public static function generateToken(array $payload, int $expiresInSeconds = 604800): string {
        $secret = env('JWT_SECRET', 'DEFAULT_NEXA_SECRET_KEY_CHANGE_IN_ENV');
        
        $header = json_encode(['typ' => 'JWT', 'alg' => 'HS256']);
        
        $now = time();
        $payload['iat'] = $now;
        $payload['exp'] = $now + $expiresInSeconds;

        $base64UrlHeader = self::base64UrlEncode($header);
        $base64UrlPayload = self::base64UrlEncode(json_encode($payload));

        $signature = hash_hmac('sha256', $base64UrlHeader . "." . $base64UrlPayload, $secret, true);
        $base64UrlSignature = self::base64UrlEncode($signature);

        return $base64UrlHeader . "." . $base64UrlPayload . "." . $base64UrlSignature;
    }

    public static function verifyToken(string $jwt): ?array {
        $secret = env('JWT_SECRET', 'DEFAULT_NEXA_SECRET_KEY_CHANGE_IN_ENV');
        $tokenParts = explode('.', $jwt);

        if (count($tokenParts) !== 3) {
            return null;
        }

        list($headerB64, $payloadB64, $sigB64) = $tokenParts;

        $signature = self::base64UrlDecode($sigB64);
        $expectedSig = hash_hmac('sha256', $headerB64 . "." . $payloadB64, $secret, true);

        if (!hash_equals($signature, $expectedSig)) {
            return null; // Invalid signature
        }

        $payload = json_decode(self::base64UrlDecode($payloadB64), true);

        if (!$payload || !isset($payload['exp']) || $payload['exp'] < time()) {
            return null; // Expired or malformed
        }

        return $payload;
    }

    public static function hashToken(string $token): string {
        return hash('sha256', $token);
    }
}
