<?php
/**
 * NEXA AI PHP Front Controller Router
 */

require_once __DIR__ . '/middleware/cors.php';
require_once __DIR__ . '/middleware/response.php';

// Always apply CORS headers
handleCors();

// Parse Request Path
$requestUri = $_SERVER['REQUEST_URI'] ?? '/';
$parsedUrl = parse_url($requestUri, PHP_URL_PATH);
$rawPath = trim($parsedUrl, '/');

// Strip base prefix if hosted in subdirectory or /api
$path = preg_replace('/^api\//i', '', $rawPath);
$segments = array_filter(explode('/', $path));
$segments = array_values($segments); // Re-index array

$firstSegment = strtolower($segments[0] ?? '');
$subSegment = strtolower($segments[1] ?? '');
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

// Route Dispatcher
try {
    switch ($firstSegment) {
        case 'auth':
            require_once __DIR__ . '/routes/auth.php';
            handleAuthRoutes($subSegment, $method);
            break;

        case 'user':
            require_once __DIR__ . '/routes/user.php';
            handleUserRoutes($subSegment, $method);
            break;

        case 'analyze':
        case 'analysis':
            require_once __DIR__ . '/routes/analysis.php';
            handleAnalysisRoutes($segments, $method);
            break;

        case 'subscription':
            require_once __DIR__ . '/routes/subscription.php';
            handleSubscriptionRoutes($subSegment, $method);
            break;

        case 'payment':
        case 'payments':
            require_once __DIR__ . '/routes/payments.php';
            $remainingPath = implode('/', array_slice($segments, 1));
            handlePaymentsRoutes($remainingPath, $method);
            break;

        case '':
        case 'health':
            try {
                require_once __DIR__ . '/config/database.php';
                $pdo = getDbConnection();
                $stmt = $pdo->query("SELECT 1");
                $dbStatus = $stmt ? 'connected' : 'disconnected';
                sendSuccessResponse([
                    'status' => 'ONLINE',
                    'db' => $dbStatus,
                    'service' => 'NEXA AI PHP Backend',
                    'version' => '2.0.0-cPanel',
                    'timestamp' => date('c')
                ]);
            } catch (Throwable $e) {
                sendErrorResponse('ارتباط با پایگاه داده برقرار نشد.', 500, 'DB_ERROR', [
                    'status' => 'OFFLINE',
                    'db' => 'disconnected'
                ]);
            }
            break;

        default:
            sendErrorResponse('مسیر API مورد نظر روی این سرور یافت نشد.', 404, 'ENDPOINT_NOT_FOUND');
            break;
    }
} catch (Throwable $e) {
    error_log("[NEXA Uncaught Error] " . $e->getMessage() . "\n" . $e->getTraceAsString());
    sendErrorResponse(
        env('APP_DEBUG') ? $e->getMessage() : 'خطای سرور رخ داده است. لطفاً مجدداً تلاش کنید.',
        500,
        'INTERNAL_SERVER_ERROR'
    );
}
