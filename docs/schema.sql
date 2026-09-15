-- =========================================================
-- NEXA AI Trading Assistant - Database Schema (SQL Export)
-- Compatible with PostgreSQL, MySQL, Supabase, Firebase SQLite
-- =========================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    otp_code VARCHAR(6) DEFAULT NULL,
    otp_expiry BIGINT DEFAULT 0,
    is_premium BOOLEAN DEFAULT FALSE,
    subscription_plan VARCHAR(50) DEFAULT 'FREE', -- 'FREE', 'MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL'
    subscription_start_date BIGINT DEFAULT 0,
    subscription_expiry_date BIGINT DEFAULT 0,
    free_analysis_count INT DEFAULT 0,
    created_at BIGINT NOT NULL
);

-- Index on user email for fast lookup during login
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);


-- 2. Analysis History Table
CREATE TABLE IF NOT EXISTS analysis_history (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    timeframe VARCHAR(16) NOT NULL,
    image_uri TEXT DEFAULT NULL,
    signal VARCHAR(16) NOT NULL, -- 'LONG', 'SHORT'
    confidence INT NOT NULL,     -- 0 to 100
    reasons_json TEXT NOT NULL,  -- JSON array of candlestick/technical reasons
    entry_price VARCHAR(32) NOT NULL,
    stop_loss VARCHAR(32) NOT NULL,
    take_profit VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL, -- 'LOW', 'MEDIUM', 'HIGH'
    created_at BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index on user_id and symbol for quick query filtering
CREATE INDEX IF NOT EXISTS idx_analysis_user ON analysis_history(user_id);
CREATE INDEX IF NOT EXISTS idx_analysis_symbol ON analysis_history(symbol);


-- 3. Payments & Subscriptions Table
CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    plan_name VARCHAR(50) NOT NULL,
    amount_toman INT NOT NULL,
    payment_status VARCHAR(32) NOT NULL, -- 'COMPLETED', 'PENDING', 'FAILED'
    gateway VARCHAR(32) DEFAULT 'ZarinPal',
    transaction_ref VARCHAR(128) DEFAULT NULL,
    created_at BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payments_user ON payments(user_id);


-- 4. Default Seed Data (Sample Users for Demo / Testing)
INSERT INTO users (
    id, email, password_hash, is_verified, is_premium, subscription_plan, 
    subscription_start_date, subscription_expiry_date, free_analysis_count, created_at
) VALUES 
('user_demo_1', 'demo@nexa.ai', 'e10adc3949ba59abbe56e057f20f883e', TRUE, TRUE, 'ANNUAL', 1770000000000, 1801536000000, 0, 1770000000000),
('user_demo_2', 'trader@nexa.ai', 'e10adc3949ba59abbe56e057f20f883e', TRUE, FALSE, 'FREE', 1770000000000, 0, 1, 1770000000000)
ON CONFLICT DO NOTHING;
