-- ==============================================================================
-- INCREMENTAL MIGRATION: TẠO BẢNG UTILITY METERS VÀ UTILITY READINGS (TASK T04)
-- ==============================================================================

-- 1. Bảng đồng hồ đo tiêu thụ (điện, nước)
CREATE TABLE IF NOT EXISTS utility_meters (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id        SMALLINT NOT NULL REFERENCES hotels(id),
    meter_code      VARCHAR(50) NOT NULL,
    meter_type      VARCHAR(20) NOT NULL CHECK (meter_type IN ('ELECTRICITY', 'WATER')),
    location_label  VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Partial unique: đồng hồ đã xóa mềm không chiếm mã
CREATE UNIQUE INDEX IF NOT EXISTS uq_utility_meter_code_active
    ON utility_meters (hotel_id, meter_code) WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_utility_meters_hotel_type
    ON utility_meters (hotel_id, meter_type);

DROP TRIGGER IF EXISTS trg_utility_meters_updated_at ON utility_meters;
CREATE TRIGGER trg_utility_meters_updated_at
    BEFORE UPDATE ON utility_meters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 2. Bảng chỉ số tiêu thụ theo ngày
CREATE TABLE IF NOT EXISTS utility_readings (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meter_id        INT NOT NULL REFERENCES utility_meters(id),
    reading_date    DATE NOT NULL,
    reading_value   NUMERIC(12,3) NOT NULL CHECK (reading_value >= 0),
    is_meter_reset  BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_by     INT NOT NULL REFERENCES staffs(id),
    updated_by      INT REFERENCES staffs(id),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_utility_reading_per_day UNIQUE (meter_id, reading_date)
);

DROP TRIGGER IF EXISTS trg_utility_readings_updated_at ON utility_readings;
CREATE TRIGGER trg_utility_readings_updated_at
    BEFORE UPDATE ON utility_readings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 3. Quyền hạn (Permissions) cho UTILITY
INSERT INTO permissions (action, resource, status) VALUES
    ('VIEW',   'UTILITY', 'ACTIVE'),
    ('CREATE', 'UTILITY', 'ACTIVE'),
    ('UPDATE', 'UTILITY', 'ACTIVE')
ON CONFLICT (action, resource) DO NOTHING;

-- Phân quyền cho CHAIN_ADMIN, PROPERTY_MANAGER và ENGINEERING: VIEW + CREATE + UPDATE
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'ENGINEERING')
  AND p.resource = 'UTILITY'
  AND p.action IN ('VIEW', 'CREATE', 'UPDATE')
ON CONFLICT DO NOTHING;

-- Phân quyền cho REGION_MANAGER: chỉ VIEW
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'REGION_MANAGER'
  AND p.resource = 'UTILITY' AND p.action = 'VIEW'
ON CONFLICT DO NOTHING;
