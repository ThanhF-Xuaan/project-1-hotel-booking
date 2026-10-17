-- 1. Xóa cột room_instance_id khỏi service_orders (bỏ quan hệ trực tiếp)
ALTER TABLE service_orders DROP COLUMN IF EXISTS room_instance_id;

-- 2. Đổi tên booking_id thành reference_id và cho phép NULL (đối với khách vãng lai WALK_IN)
ALTER TABLE service_orders DROP CONSTRAINT IF EXISTS service_orders_booking_id_fkey;
ALTER TABLE service_orders ALTER COLUMN booking_id DROP NOT NULL;
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'service_orders' AND column_name = 'booking_id'
    ) THEN
        ALTER TABLE service_orders RENAME COLUMN booking_id TO reference_id;
    END IF;
END $$;

-- 3. Thêm cột reference_type đại diện cho 4 cấp độ hạch toán (BOOKING, BOOKING_ROOM, STAY_GUEST, WALK_IN)
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS reference_type VARCHAR(50) NOT NULL DEFAULT 'BOOKING';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_service_orders_reference_type'
    ) THEN
        ALTER TABLE service_orders ADD CONSTRAINT chk_service_orders_reference_type
            CHECK (reference_type IN ('BOOKING', 'BOOKING_ROOM', 'STAY_GUEST', 'WALK_IN'));
    END IF;
END $$;

-- 4. Thêm cột hotel_id trực tiếp trên service_orders để hạch toán doanh thu F&B/Dịch vụ cơ sở
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS hotel_id SMALLINT REFERENCES hotels(id);

-- Cập nhật dữ liệu cũ: Điền hotel_id dựa trên booking hiện tại
UPDATE service_orders so
SET hotel_id = b.hotel_id
FROM bookings b
WHERE so.reference_id = b.id AND so.reference_type = 'BOOKING' AND so.hotel_id IS NULL;

-- Fallback cho các bản ghi cũ nếu có
UPDATE service_orders SET hotel_id = 1 WHERE hotel_id IS NULL;

-- Đặt NOT NULL cho hotel_id
ALTER TABLE service_orders ALTER COLUMN hotel_id SET NOT NULL;

-- 5. Tạo Indexes phục vụ tra cứu
CREATE INDEX IF NOT EXISTS idx_service_orders_reference ON service_orders(reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_service_orders_hotel ON service_orders(hotel_id);
