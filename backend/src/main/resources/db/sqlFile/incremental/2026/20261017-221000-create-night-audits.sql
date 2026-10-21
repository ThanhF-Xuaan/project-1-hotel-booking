-- ==============================================================================
-- INCREMENTAL MIGRATION: CREATE NIGHT_AUDITS TABLE
-- Lưu trữ biên bản chốt ca kiểm toán đêm / đóng ngày (End of Day Financial & Operational Closure)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS night_audits (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id                    SMALLINT NOT NULL REFERENCES hotels(id),
    audit_date                  DATE NOT NULL,

    -- Chỉ số công suất & vận hành phòng
    occupied_rooms_count        INT NOT NULL DEFAULT 0,
    no_show_bookings_count      INT NOT NULL DEFAULT 0,

    -- Chỉ số doanh thu tài chính trong ngày (Financial Revenue Snapshot)
    total_daily_room_revenue    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_daily_service_revenue NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_daily_revenue         NUMERIC(15, 2) NOT NULL DEFAULT 0,

    -- Vết kiểm toán vận hành
    status                      VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    executed_by                 VARCHAR(100),
    executed_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes                       TEXT,

    -- BaseEntity tracking
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_unique_hotel_night_audit UNIQUE (hotel_id, audit_date)
);

CREATE INDEX IF NOT EXISTS idx_night_audits_hotel_date ON night_audits(hotel_id, audit_date);

-- Trigger tự động cập nhật updated_at
DROP TRIGGER IF EXISTS trg_night_audits_updated_at ON night_audits;
CREATE TRIGGER trg_night_audits_updated_at
    BEFORE UPDATE ON night_audits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
