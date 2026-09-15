<?php
/**
 * NEXA AI MySQL Database Connection Handler via PDO
 */

require_once __DIR__ . '/config.php';

function getDbConnection(): PDO {
    static $pdo = null;

    if ($pdo === null) {
        $host = env('DB_HOST', 'localhost');
        $port = env('DB_PORT', '3306');
        $dbname = env('DB_NAME', 'h410448_NEXAAI');
        $user = env('DB_USER', 'root');
        $password = env('DB_PASSWORD', '');

        $dsn = "mysql:host={$host};port={$port};dbname={$dbname};charset=utf8mb4";

        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
            PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci"
        ];

        try {
            $pdo = new PDO($dsn, $user, $password, $options);
        } catch (PDOException $e) {
            header('Content-Type: application/json; charset=utf-8');
            http_response_code(500);
            echo json_encode([
                'success' => false,
                'message' => 'خطا در اتصال به پایگاه داده MySQL.',
                'error' => [
                    'code' => 'DATABASE_CONNECTION_ERROR',
                    'message' => env('APP_DEBUG') ? $e->getMessage() : 'ارتباط با دیتابیس برقرار نشد.'
                ]
            ], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }

    return $pdo;
}
