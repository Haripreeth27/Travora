-- ==========================================================================
-- V8: Add spreadsheet import batches and imported travel history records
-- Supports Excel/CSV upload → preview → import workflow
-- ==========================================================================

-- Import batches track each upload session
CREATE TABLE import_batches (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT       NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    uploaded_by_id  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    filename        VARCHAR(255) NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    file_type       VARCHAR(10)  NOT NULL,        -- XLSX, CSV
    sheet_name      VARCHAR(255),                  -- selected worksheet (xlsx)
    import_type     VARCHAR(50)  NOT NULL DEFAULT 'TRAVEL_HISTORY',
    status          VARCHAR(30)  NOT NULL DEFAULT 'PREVIEW',
                    -- PREVIEW, IMPORTING, COMPLETED, FAILED, EXPIRED
    total_rows      INTEGER      NOT NULL DEFAULT 0,
    valid_rows      INTEGER      NOT NULL DEFAULT 0,
    imported_rows   INTEGER      NOT NULL DEFAULT 0,
    skipped_rows    INTEGER      NOT NULL DEFAULT 0,
    error_rows      INTEGER      NOT NULL DEFAULT 0,
    column_mapping  TEXT,                          -- JSON: { targetField → sourceColumnHeader }
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP WITHOUT TIME ZONE,
    expires_at      TIMESTAMP WITHOUT TIME ZONE   -- preview auto-cleanup deadline
);

-- Individual records parsed from uploaded spreadsheets
CREATE TABLE imported_travel_records (
    id                      BIGSERIAL PRIMARY KEY,
    company_id              BIGINT       NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    import_batch_id         BIGINT       NOT NULL REFERENCES import_batches(id) ON DELETE CASCADE,
    employee_id             BIGINT       REFERENCES employees(id) ON DELETE SET NULL,  -- linked when reliably matched
    raw_employee_identifier VARCHAR(255),          -- raw employee ID/code from spreadsheet
    raw_employee_name       VARCHAR(500),          -- raw name from spreadsheet
    travel_date             DATE,
    return_date             DATE,
    from_location           VARCHAR(500),
    to_location             VARCHAR(500),
    ticket_reference        VARCHAR(255),
    department              VARCHAR(255),
    travel_mode             VARCHAR(50),
    provider                VARCHAR(255),
    cost                    NUMERIC(12, 2),
    project_name            VARCHAR(255),
    status                  VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
                            -- PENDING, VALID, MATCHED, UNMATCHED, DUPLICATE, INVALID, IMPORTED, SKIPPED
    match_confidence        VARCHAR(20),           -- HIGH, MEDIUM, LOW, NONE
    validation_errors       TEXT,                   -- JSON array of error strings
    source_row_number       INTEGER      NOT NULL,
    raw_row_data            TEXT,                   -- JSON snapshot of original spreadsheet row
    imported                BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Query indexes for multi-tenant access and common search patterns
CREATE INDEX idx_import_batches_company        ON import_batches(company_id);
CREATE INDEX idx_import_batches_company_status ON import_batches(company_id, status);
CREATE INDEX idx_import_batches_uploaded_by    ON import_batches(uploaded_by_id);

CREATE INDEX idx_imported_records_batch        ON imported_travel_records(import_batch_id);
CREATE INDEX idx_imported_records_company      ON imported_travel_records(company_id);
CREATE INDEX idx_imported_records_employee     ON imported_travel_records(employee_id);
CREATE INDEX idx_imported_records_date         ON imported_travel_records(company_id, travel_date);
CREATE INDEX idx_imported_records_status       ON imported_travel_records(import_batch_id, status);
CREATE INDEX idx_imported_records_imported     ON imported_travel_records(company_id, imported);

