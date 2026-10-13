-- ==========================================================
-- Travora Travel Management System
-- Bulk Booking & Group Movements - V3
-- ==========================================================

CREATE TABLE IF NOT EXISTS bulk_bookings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    from_location VARCHAR(255) NOT NULL,
    travel_dates VARCHAR(255) NOT NULL,
    departure_date DATE,
    return_date DATE,
    travel_mode VARCHAR(50) NOT NULL,
    provider VARCHAR(150),
    group_reference VARCHAR(100) NOT NULL,
    headcount INT NOT NULL DEFAULT 1,
    total_cost NUMERIC(19, 2) NOT NULL,
    cost_per_person NUMERIC(19, 2) NOT NULL,
    savings NUMERIC(19, 2) DEFAULT 0,
    project VARCHAR(255),
    departments VARCHAR(500),
    passenger_manifest_summary TEXT,
    itinerary_notes TEXT,
    coordinator_name VARCHAR(150),
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    approver_status VARCHAR(50) DEFAULT 'PENDING_REVIEW',
    approver_comment TEXT,
    approver_reviewed_at TIMESTAMP,
    attachment_name VARCHAR(255),
    attachment_data TEXT
);

CREATE INDEX IF NOT EXISTS idx_bulk_bookings_group_ref ON bulk_bookings(group_reference);
CREATE INDEX IF NOT EXISTS idx_bulk_bookings_status ON bulk_bookings(status);
CREATE INDEX IF NOT EXISTS idx_bulk_bookings_travel_mode ON bulk_bookings(travel_mode);
CREATE INDEX IF NOT EXISTS idx_bulk_bookings_logged_at ON bulk_bookings(logged_at DESC);

CREATE TABLE IF NOT EXISTS bulk_booking_passengers (
    id BIGSERIAL PRIMARY KEY,
    bulk_booking_id BIGINT NOT NULL REFERENCES bulk_bookings(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    employee_id VARCHAR(50),
    department VARCHAR(100),
    seat_or_coach VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_bb_passengers_booking_id ON bulk_booking_passengers(bulk_booking_id);
CREATE INDEX IF NOT EXISTS idx_bb_passengers_employee_id ON bulk_booking_passengers(employee_id);
