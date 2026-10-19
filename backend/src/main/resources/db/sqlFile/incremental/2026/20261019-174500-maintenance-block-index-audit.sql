CREATE INDEX IF NOT EXISTS idx_room_maintenance_block_room_status
    ON room_maintenance_blocks (room_instance_id, status);

CREATE INDEX IF NOT EXISTS idx_room_maintenance_block_range
    ON room_maintenance_blocks (start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_room_maintenance_block_active
    ON room_maintenance_blocks (room_instance_id, start_date, end_date)
    WHERE status = 'ACTIVE';

ALTER TABLE room_maintenance_blocks
    ADD COLUMN IF NOT EXISTS updated_by_staff_id INT REFERENCES staffs(id);

ALTER TABLE room_maintenance_blocks
    ADD COLUMN IF NOT EXISTS notes TEXT;

ALTER TABLE room_availability
    DROP CONSTRAINT IF EXISTS chk_ooo_rooms_non_negative;

ALTER TABLE room_availability
    ADD CONSTRAINT chk_ooo_rooms_non_negative CHECK (ooo_rooms >= 0);
