-- Thêm các trường BaseEntity (is_deleted, updated_at) cho bảng hotel_room_type_inclusions
ALTER TABLE hotel_room_type_inclusions
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Đánh index cột is_deleted và hotel_room_type_id phục vụ tìm kiếm nhanh
CREATE INDEX IF NOT EXISTS idx_hotel_room_type_inclusions_active
    ON hotel_room_type_inclusions(hotel_room_type_id, is_deleted);
