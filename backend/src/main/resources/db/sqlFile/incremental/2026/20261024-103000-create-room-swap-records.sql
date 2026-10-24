-- ==============================================================================
-- INCREMENTAL MIGRATION: 20261024-103000-create-room-swap-records.sql
-- Bảng lưu vết lịch sử đổi phòng (Room Swap / Room Move) theo chuẩn nghiệp vụ PMS khách sạn
-- ==============================================================================

CREATE TABLE IF NOT EXISTS room_swap_records (
    id BIGSERIAL PRIMARY KEY,
    booking_room_id BIGINT NOT NULL,
    from_room_instance_id INT NOT NULL,
    to_room_instance_id INT NOT NULL,
    from_hotel_room_type_id INT NOT NULL,
    to_hotel_room_type_id INT NOT NULL,
    swap_date DATE NOT NULL,
    effective_date DATE NOT NULL,
    reason TEXT NOT NULL,
    is_upgrade BOOLEAN NOT NULL DEFAULT FALSE,
    is_complimentary BOOLEAN NOT NULL DEFAULT FALSE,
    price_difference NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    performed_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_room_swap_booking_room FOREIGN KEY (booking_room_id) REFERENCES booking_rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_room_swap_from_room FOREIGN KEY (from_room_instance_id) REFERENCES room_instances(id),
    CONSTRAINT fk_room_swap_to_room FOREIGN KEY (to_room_instance_id) REFERENCES room_instances(id),
    CONSTRAINT fk_room_swap_from_type FOREIGN KEY (from_hotel_room_type_id) REFERENCES hotel_room_types(id),
    CONSTRAINT fk_room_swap_to_type FOREIGN KEY (to_hotel_room_type_id) REFERENCES hotel_room_types(id)
);

CREATE INDEX IF NOT EXISTS idx_room_swap_booking_room ON room_swap_records(booking_room_id);
CREATE INDEX IF NOT EXISTS idx_room_swap_dates ON room_swap_records(swap_date, effective_date);
