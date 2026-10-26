-- ==========================================================
-- Travora Corporate Travel & Expense Management
-- Database Migration - V5: Add Expense Reimbursement Audit
-- ==========================================================

ALTER TABLE expenses ADD COLUMN IF NOT EXISTS reimbursed_by_id BIGINT REFERENCES users(id);
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS reimbursed_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_expenses_reimbursed_by_id ON expenses(reimbursed_by_id);
