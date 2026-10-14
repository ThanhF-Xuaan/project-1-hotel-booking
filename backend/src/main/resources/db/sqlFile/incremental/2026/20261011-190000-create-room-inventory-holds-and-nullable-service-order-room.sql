-- 1. Bảng lưu trữ vết giữ phòng chi tiết theo từng session (Granular Hold Tracking kế thừa BaseEntity)
CREATE TABLE IF NOT EXISTS room_inventory_holds (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    hold_date DATE NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    release_reason VARCHAR(50),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_room_inventory_holds_active_session
    ON room_inventory_holds(session_id, status, is_deleted);
CREATE INDEX IF NOT EXISTS idx_room_inventory_holds_active_expires
    ON room_inventory_holds(expires_at, status, is_deleted);
CREATE INDEX IF NOT EXISTS idx_room_inventory_holds_room_date
    ON room_inventory_holds(hotel_room_type_id, hold_date);

-- 2. Cho phép room_instance_id trên service_orders được NULL khi đặt dịch vụ kèm theo lúc booking online
ALTER TABLE service_orders ALTER COLUMN room_instance_id DROP NOT NULL;
