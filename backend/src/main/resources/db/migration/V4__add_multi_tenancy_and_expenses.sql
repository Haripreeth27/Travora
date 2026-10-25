-- ==========================================================
-- Travora Corporate Travel & Expense Management
-- Database Migration - V4: Multi-Tenancy & Expenses Schema
-- ==========================================================

-- 1. Companies (Tenants)
CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    domain VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_companies_code ON companies(code);
CREATE INDEX IF NOT EXISTS idx_companies_active ON companies(active);

-- Seed default initial tenant
INSERT INTO companies (name, code, domain, active)
VALUES ('Mahathi Infra Services Pvt. Ltd.', 'MISPL', 'mahathiinfra.com', true)
ON CONFLICT (code) DO NOTHING;

-- 2. Associate Employees with Company
ALTER TABLE employees ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES companies(id);
UPDATE employees SET company_id = (SELECT id FROM companies WHERE code = 'MISPL' LIMIT 1) WHERE company_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_employees_company_id ON employees(company_id);

-- 3. Associate Users with Company
ALTER TABLE users ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES companies(id);
UPDATE users SET company_id = (SELECT id FROM companies WHERE code = 'MISPL' LIMIT 1) WHERE company_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_company_id ON users(company_id);

-- 4. Associate Travel Requests with Company
ALTER TABLE travel_requests ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES companies(id);
UPDATE travel_requests SET company_id = (SELECT id FROM companies WHERE code = 'MISPL' LIMIT 1) WHERE company_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_travel_requests_company_id ON travel_requests(company_id);

-- 5. Associate Bookings with Company
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES companies(id);
UPDATE bookings b 
SET company_id = tr.company_id 
FROM travel_requests tr 
WHERE b.travel_request_id = tr.id AND b.company_id IS NULL;
UPDATE bookings SET company_id = (SELECT id FROM companies WHERE code = 'MISPL' LIMIT 1) WHERE company_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_bookings_company_id ON bookings(company_id);

-- 6. Associate Notifications with Company
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES companies(id);
UPDATE notifications n 
SET company_id = u.company_id 
FROM users u 
WHERE n.recipient_id = u.id AND n.company_id IS NULL;
UPDATE notifications SET company_id = (SELECT id FROM companies WHERE code = 'MISPL' LIMIT 1) WHERE company_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_company_id ON notifications(company_id);

-- 7. Associate Bulk Bookings with Company
ALTER TABLE bulk_bookings ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES companies(id);
UPDATE bulk_bookings SET company_id = (SELECT id FROM companies WHERE code = 'MISPL' LIMIT 1) WHERE company_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_bulk_bookings_company_id ON bulk_bookings(company_id);

-- 8. Corporate Expense Management
CREATE TABLE IF NOT EXISTS expenses (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    travel_request_id BIGINT REFERENCES travel_requests(id) ON DELETE SET NULL,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    category VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    expense_date DATE NOT NULL,
    merchant VARCHAR(255) NOT NULL,
    description TEXT,
    receipt_url VARCHAR(500),
    receipt_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    approver_id BIGINT REFERENCES users(id),
    approval_comment TEXT,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_expenses_company_id ON expenses(company_id);
CREATE INDEX IF NOT EXISTS idx_expenses_employee_id ON expenses(employee_id);
CREATE INDEX IF NOT EXISTS idx_expenses_travel_request_id ON expenses(travel_request_id);
CREATE INDEX IF NOT EXISTS idx_expenses_status ON expenses(status);
CREATE INDEX IF NOT EXISTS idx_expenses_expense_date ON expenses(expense_date);
