-- ==========================================================
-- Travora Travel Management System
-- Database Schema Definition - V1
-- Target Engine: PostgreSQL 15+ / Compatible with H2
-- ==========================================================

-- 1. Employees Directory
CREATE TABLE IF NOT EXISTS employees (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    department VARCHAR(100),
    designation VARCHAR(100),
    location VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_employees_employee_id ON employees(employee_id);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees(department);

-- 2. User Accounts & Credentials
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    employee_id VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_employee_id ON users(employee_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 3. Travel Requests
CREATE TABLE IF NOT EXISTS travel_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    trip_type VARCHAR(50) NOT NULL,
    from_location VARCHAR(255) NOT NULL,
    to_location VARCHAR(255) NOT NULL,
    travel_date DATE NOT NULL,
    return_date DATE,
    project_name VARCHAR(255) NOT NULL,
    reason TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approver_name VARCHAR(255),
    approval_comment TEXT,
    approval_date TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_travel_requests_employee_id ON travel_requests(employee_id);
CREATE INDEX IF NOT EXISTS idx_travel_requests_status ON travel_requests(status);
CREATE INDEX IF NOT EXISTS idx_travel_requests_travel_date ON travel_requests(travel_date);

-- 4. Bookings
CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    travel_request_id BIGINT NOT NULL REFERENCES travel_requests(id) ON DELETE CASCADE,
    booking_type VARCHAR(50) NOT NULL,
    booking_reference VARCHAR(100) NOT NULL,
    provider VARCHAR(150),
    cost NUMERIC(19, 2) NOT NULL,
    savings NUMERIC(19, 2) DEFAULT 0,
    notes TEXT,
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled BOOLEAN NOT NULL DEFAULT false,
    cancellation_reason TEXT,
    cancellation_charge NUMERIC(19, 2) DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_bookings_travel_request_id ON bookings(travel_request_id);
CREATE INDEX IF NOT EXISTS idx_bookings_reference ON bookings(booking_reference);
CREATE INDEX IF NOT EXISTS idx_bookings_cancelled ON bookings(cancelled);

-- 5. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    travel_request_id BIGINT,
    booking_id BIGINT,
    read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created ON notifications(recipient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_read ON notifications(recipient_id, read);
