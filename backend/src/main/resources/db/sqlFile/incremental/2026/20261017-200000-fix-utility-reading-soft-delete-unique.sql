-- ==============================================================================
-- INCREMENTAL MIGRATION: KHAC PHUC SOFT-DELETE CHO UTILITY READINGS (TASK T04)
-- ==============================================================================

-- 1. Dam bao cot is_deleted ton tai tren bang utility_readings
ALTER TABLE utility_readings
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Tao partial unique index chi ap dung cho cac ban ghi con hoat dong (is_deleted = FALSE)
-- Thuc hien tao index truoc khi drop constraint cu de luon duy tri tinh bao ve duy nhat
CREATE UNIQUE INDEX IF NOT EXISTS uq_utility_reading_active_per_day
    ON utility_readings (meter_id, reading_date) WHERE is_deleted = FALSE;

-- 3. Xoa constraint cu bao gom ca ban ghi xoa mem
ALTER TABLE utility_readings DROP CONSTRAINT IF EXISTS uq_utility_reading_per_day;
