-- 1. Bổ sung cột full_name cho bảng booking_guests (hồ sơ khách đại diện đặt phòng CRM)
ALTER TABLE booking_guests
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(250);

-- 2. Bổ sung cột notes cho bảng bookings (ghi chú / yêu cầu đặc biệt của khách cho đơn đặt phòng)
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS notes TEXT;
