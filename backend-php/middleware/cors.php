<?php
/**
 * NEXA AI CORS Handler Middleware
 */

require_once __DIR__ . '/../config/config.php';

function handleCors() {
    $corsOrigin = env('CORS_ORIGIN', '*');
    $httpOrigin = $_SERVER['HTTP_ORIGIN'] ?? '';

    if ($corsOrigin === '*') {
        header("Access-Control-Allow-Origin: *");
    } else {
        $allowedOrigins = array_map('trim', explode(',', $corsOrigin));
        if (in_array($httpOrigin, $allowedOrigins)) {
            header("Access-Control-Allow-Origin: {$httpOrigin}");
            header("Access-Control-Allow-Credentials: true");
        }
    }

    header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
    header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

    // Handle Preflight OPTIONS request
    if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
        http_response_code(200);
        exit;
    }
}
