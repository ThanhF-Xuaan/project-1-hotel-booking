-- Utility meter and reading tables for electricity/water consumption tracking.
-- Run after V003.
-- Note on permissions: Role assignments below only apply on existing databases where roles already exist.
-- On fresh databases where roles are seeded later via database/04-seed-data-global-master-data.sql,
-- the role assignments in this migration will match 0 rows; run database/10-seed-utility-permissions.sql
-- manually after running the master data seed.

-- ==========================================================================
-- 1. UTILITY METERS
-- ==========================================================================

CREATE TABLE utility_meters (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id        SMALLINT NOT NULL REFERENCES hotels(id),
    meter_code      VARCHAR(50) NOT NULL,
    meter_type      VARCHAR(20) NOT NULL CHECK (meter_type IN ('ELECTRICITY', 'WATER')),
    location_label  VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Partial unique: soft-deleted meters do not block the code for reuse.
CREATE UNIQUE INDEX uq_utility_meter_code_active
    ON utility_meters (hotel_id, meter_code) WHERE is_deleted = FALSE;

CREATE INDEX idx_utility_meters_hotel_type
    ON utility_meters (hotel_id, meter_type);

CREATE TRIGGER set_updated_at BEFORE UPDATE ON utility_meters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==========================================================================
-- 2. UTILITY READINGS
-- ==========================================================================

CREATE TABLE utility_readings (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meter_id        INT NOT NULL REFERENCES utility_meters(id),
    reading_date    DATE NOT NULL,
    reading_value   NUMERIC(12,3) NOT NULL CHECK (reading_value >= 0),
    is_meter_reset  BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_by     INT NOT NULL REFERENCES staffs(id),
    updated_by      INT REFERENCES staffs(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    -- One reading per meter per day. UNIQUE implicitly creates an index.
    CONSTRAINT uq_utility_reading_per_day UNIQUE (meter_id, reading_date)
);

CREATE TRIGGER set_updated_at BEFORE UPDATE ON utility_readings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==========================================================================
-- 3. PERMISSIONS
-- ==========================================================================

INSERT INTO permissions (action, resource) VALUES
    ('VIEW',   'UTILITY'),
    ('CREATE', 'UTILITY'),
    ('UPDATE', 'UTILITY')
ON CONFLICT (action, resource) DO NOTHING;

-- CHAIN_ADMIN + PROPERTY_MANAGER: full access (VIEW + CREATE + UPDATE)
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('CHAIN_ADMIN', 'PROPERTY_MANAGER')
  AND p.resource = 'UTILITY'
  AND p.action IN ('VIEW', 'CREATE', 'UPDATE')
ON CONFLICT DO NOTHING;

-- REGION_MANAGER: read-only (VIEW)
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'REGION_MANAGER'
  AND p.resource = 'UTILITY' AND p.action = 'VIEW'
ON CONFLICT DO NOTHING;
