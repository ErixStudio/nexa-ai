<?php
/**
 * NEXA AI Standardized JSON Response Middleware
 */

function sendSuccessResponse(array $data = [], int $statusCode = 200) {
    header('Content-Type: application/json; charset=utf-8');
    http_response_code($statusCode);
    
    $response = array_merge(['success' => true], $data);
    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function sendErrorResponse(string $message, int $statusCode = 400, string $errorCode = 'BAD_REQUEST', array $extra = []) {
    header('Content-Type: application/json; charset=utf-8');
    http_response_code($statusCode);
    
    $response = array_merge([
        'success' => false,
        'message' => $message,
        'error' => [
            'code' => $errorCode,
            'message' => $message
        ]
    ], $extra);
    
    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function getJsonInput(): array {
    $raw = file_get_contents('php://input');
    if (empty($raw)) {
        return $_POST;
    }
    $decoded = json_decode($raw, true);
    return is_array($decoded) ? $decoded : [];
}
