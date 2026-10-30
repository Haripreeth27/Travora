-- ==========================================================
-- Trapple Corporate Travel & Directory Hardening
-- Database Migration - V7: Harden Employee Directory & Multi-Tenancy
-- ==========================================================

-- 1. Drop the legacy global unique constraint on employee_id from V1.
-- In PostgreSQL, employee_id VARCHAR(50) NOT NULL UNIQUE generates employees_employee_id_key constraint.
ALTER TABLE employees DROP CONSTRAINT IF EXISTS employees_employee_id_key;

-- 2. Drop the legacy single-column index so it can be replaced by tenant-aware indexes
DROP INDEX IF EXISTS idx_employees_employee_id;

-- 3. Ensure all existing employees have a valid company_id (default to initial tenant MISPL if any null)
UPDATE employees SET company_id = (SELECT id FROM companies WHERE code = 'MISPL' LIMIT 1) WHERE company_id IS NULL;

-- 4. Add company-scoped unique constraint for multi-tenant isolation
ALTER TABLE employees ADD CONSTRAINT uk_employees_company_emp_id UNIQUE (company_id, employee_id);

-- 5. Add missing employee directory fields with safe defaults
ALTER TABLE employees ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE employees ADD COLUMN IF NOT EXISTS contact_number VARCHAR(50);
ALTER TABLE employees ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE employees ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE employees ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 6. Indexes for company-scoped search, status filtering, and sorting
CREATE INDEX IF NOT EXISTS idx_employees_company_emp_id ON employees(company_id, employee_id);
CREATE INDEX IF NOT EXISTS idx_employees_company_active ON employees(company_id, active);
CREATE INDEX IF NOT EXISTS idx_employees_company_name ON employees(company_id, name);
CREATE INDEX IF NOT EXISTS idx_employees_company_dept ON employees(company_id, department);
CREATE INDEX IF NOT EXISTS idx_employees_company_email ON employees(company_id, email);

