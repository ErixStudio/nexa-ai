-- ==========================================
-- NEXA AI Complete Production Database Schema
-- Compatible with MySQL 8.0+ & MariaDB
-- Database Name: h410448_NEXAAI
-- ==========================================

CREATE DATABASE IF NOT EXISTS `h410448_NEXAAI` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `h410448_NEXAAI`;

-- ------------------------------------------
-- 1. Users Table
-- ------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id` VARCHAR(64) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `is_verified` TINYINT(1) DEFAULT 0,
    `is_premium` TINYINT(1) DEFAULT 0,
    `subscription_plan` VARCHAR(32) DEFAULT 'FREE',
    `free_analysis_count` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_email` (`email`),
    INDEX `idx_users_is_premium` (`is_premium`),
    INDEX `idx_users_subscription_plan` (`subscription_plan`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------
-- 2. Chart Analyses Table
-- ------------------------------------------
CREATE TABLE IF NOT EXISTS `chart_analyses` (
    `id` VARCHAR(64) NOT NULL,
    `user_id` VARCHAR(64) NOT NULL,
    `symbol` VARCHAR(32) NOT NULL,
    `timeframe` VARCHAR(16) NOT NULL,
    `signal` VARCHAR(16) NOT NULL,
    `confidence` INT NOT NULL,
    `reasons_json` TEXT DEFAULT NULL,
    `entry_price` VARCHAR(32) DEFAULT NULL,
    `stop_loss` VARCHAR(32) DEFAULT NULL,
    `take_profit` VARCHAR(32) DEFAULT NULL,
    `risk_level` VARCHAR(16) DEFAULT 'LOW',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_chart_analyses_user_id` (`user_id`),
    INDEX `idx_chart_analyses_created_at` (`created_at`),
    CONSTRAINT `fk_chart_analyses_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------
-- 3. Payments Table
-- ------------------------------------------
CREATE TABLE IF NOT EXISTS `payments` (
    `id` VARCHAR(64) NOT NULL,
    `user_id` VARCHAR(64) NOT NULL,
    `plan_name` VARCHAR(32) NOT NULL,
    `amount_toman` INT NOT NULL,
    `payment_status` VARCHAR(32) DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED
    `transaction_ref` VARCHAR(128) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_payments_user_id` (`user_id`),
    INDEX `idx_payments_status` (`payment_status`),
    CONSTRAINT `fk_payments_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------
-- 4. Subscriptions Table
-- ------------------------------------------
CREATE TABLE IF NOT EXISTS `subscriptions` (
    `id` VARCHAR(64) NOT NULL,
    `user_id` VARCHAR(64) NOT NULL,
    `plan_name` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, CANCELLED
    `start_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `end_date` TIMESTAMP NULL DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_subscriptions_user_id` (`user_id`),
    INDEX `idx_subscriptions_status` (`status`),
    CONSTRAINT `fk_subscriptions_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------
-- 5. Refresh Tokens Table (Hashed Token & Rotation)
-- ------------------------------------------
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id` VARCHAR(64) NOT NULL,
    `user_id` VARCHAR(64) NOT NULL,
    `token_hash` VARCHAR(255) NOT NULL,
    `is_revoked` TINYINT(1) DEFAULT 0,
    `expires_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_refresh_tokens_user_id` (`user_id`),
    INDEX `idx_refresh_tokens_hash` (`token_hash`),
    CONSTRAINT `fk_refresh_tokens_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------
-- 6. OTP Verification Requests Table
-- ------------------------------------------
CREATE TABLE IF NOT EXISTS `otp_requests` (
    `id` VARCHAR(64) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `otp_hash` VARCHAR(255) NOT NULL,
    `is_used` TINYINT(1) DEFAULT 0,
    `expires_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_otp_requests_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
