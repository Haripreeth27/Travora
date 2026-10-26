-- ==========================================================
-- Travora Corporate Travel & Expense Management
-- Database Migration - V6: User Management & Account Activation
-- ==========================================================

-- 1. Add created_at, activation_token_hash, and activation_token_expires_at to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS activation_token_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS activation_token_expires_at TIMESTAMP;

-- 2. Indexes for efficient lookup and token verification
CREATE INDEX IF NOT EXISTS idx_users_activation_token_hash ON users(activation_token_hash);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
