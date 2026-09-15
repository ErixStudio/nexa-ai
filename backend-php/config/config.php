<?php
/**
 * NEXA AI Environment Config Loader
 */

function loadEnv($envPath = __DIR__ . '/../.env') {
    if (!file_exists($envPath)) {
        // Fallback if .env is placed one level above public_html
        $envPath = __DIR__ . '/../../.env';
    }

    if (file_exists($envPath)) {
        $lines = file($envPath, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
        foreach ($lines as $line) {
            $line = trim($line);
            if (empty($line) || strpos($line, '#') === 0) continue;
            
            list($key, $val) = explode('=', $line, 2) + [null, null];
            if ($key && $val !== null) {
                $key = trim($key);
                $val = trim($val, " \t\n\r\0\x0B\"'");
                putenv("{$key}={$val}");
                $_ENV[$key] = $val;
                $_SERVER[$key] = $val;
            }
        }
    }
}

// Automatically load on include
loadEnv();

function env($key, $default = null) {
    $value = $_ENV[$key] ?? $_SERVER[$key] ?? getenv($key);
    if ($value === false || $value === null || $value === '') {
        return $default;
    }
    if ($value === 'true') return true;
    if ($value === 'false') return false;
    return $value;
}
