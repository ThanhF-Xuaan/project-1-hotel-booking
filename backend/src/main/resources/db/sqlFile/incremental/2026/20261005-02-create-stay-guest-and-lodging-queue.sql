-- =========================================================================
-- Refactor & Unify: Chuyển đổi tên bảng tường minh theo phân công kiến trúc:
-- 1. Bảng booking_guests cũ (chứa khách ở từng phòng) -> stay_guests
-- 2. Bảng guests cũ (chứa hồ sơ người đặt phòng) -> booking_guests
-- 3. Bổ sung các cột định danh & lưu trú BCA cho stay_guests
-- 4. Tạo bảng lodging_queues liên kết với stay_guests
-- =========================================================================

-- 1. Đổi tên bảng booking_guests (của Member 2) thành stay_guests (khách lưu trú thực tế)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'booking_guests')
       AND NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'stay_guests') THEN
        ALTER TABLE booking_guests RENAME TO stay_guests;
    END IF;
END $$;

-- 2. Đổi tên bảng guests (của Member 2) thành booking_guests (hồ sơ khách đặt phòng)
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'guests')
       AND NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'booking_guests') THEN
        ALTER TABLE guests RENAME TO booking_guests;
    END IF;
END $$;

-- 3. Tạo bảng stay_guests nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS stay_guests (
    id BIGSERIAL PRIMARY KEY,
    booking_room_id BIGINT REFERENCES booking_rooms(id) ON DELETE CASCADE,
    hotel_id SMALLINT REFERENCES hotels(id),
    booking_id BIGINT REFERENCES bookings(id),
    room_id INT REFERENCES room_instances(id),
    room_number VARCHAR(20),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    full_name VARCHAR(250) NOT NULL,
    date_of_birth DATE,
    guest_type VARCHAR(20) DEFAULT 'ADULT',
    gender VARCHAR(20),
    nationality VARCHAR(100) NOT NULL DEFAULT 'Việt Nam',
    document_type VARCHAR(20) DEFAULT 'CCCD',
    document_number VARCHAR(50),
    permanent_address TEXT,
    current_address TEXT,
    check_in_time TIMESTAMP WITH TIME ZONE,
    expected_check_out_time TIMESTAMP WITH TIME ZONE,
    actual_check_out_time TIMESTAMP WITH TIME ZONE,
    reason_for_stay VARCHAR(150) DEFAULT 'Du lịch',
    document_image_url TEXT,
    image_purged_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(50),
    version BIGINT DEFAULT 0
);

-- Bổ sung các cột mở rộng cho stay_guests nếu bảng đã tồn tại từ trước khi rename
ALTER TABLE stay_guests
    ADD COLUMN IF NOT EXISTS hotel_id SMALLINT REFERENCES hotels(id),
    ADD COLUMN IF NOT EXISTS booking_id BIGINT REFERENCES bookings(id),
    ADD COLUMN IF NOT EXISTS room_id INT REFERENCES room_instances(id),
    ADD COLUMN IF NOT EXISTS room_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS gender VARCHAR(20),
    ADD COLUMN IF NOT EXISTS nationality VARCHAR(100) NOT NULL DEFAULT 'Việt Nam',
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(20) DEFAULT 'CCCD',
    ADD COLUMN IF NOT EXISTS document_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS permanent_address TEXT,
    ADD COLUMN IF NOT EXISTS current_address TEXT,
    ADD COLUMN IF NOT EXISTS check_in_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS expected_check_out_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS actual_check_out_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS reason_for_stay VARCHAR(150) DEFAULT 'Du lịch',
    ADD COLUMN IF NOT EXISTS document_image_url TEXT,
    ADD COLUMN IF NOT EXISTS image_purged_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- 4. Tạo bảng lodging_queues
CREATE TABLE IF NOT EXISTS lodging_queues (
    id BIGSERIAL PRIMARY KEY,
    stay_guest_id BIGINT NOT NULL REFERENCES stay_guests(id),
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    exported_at TIMESTAMP WITH TIME ZONE,
    batch_reference VARCHAR(100),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(50),
    version BIGINT DEFAULT 0
);

-- Chỉ mục
CREATE INDEX IF NOT EXISTS idx_stay_guests_hotel_checkin ON stay_guests(hotel_id, check_in_time);
CREATE INDEX IF NOT EXISTS idx_stay_guests_document ON stay_guests(document_number, is_deleted);
CREATE INDEX IF NOT EXISTS idx_lodging_queues_status_hotel ON lodging_queues(hotel_id, status, is_deleted);
CREATE INDEX IF NOT EXISTS idx_lodging_queues_stay_guest ON lodging_queues(stay_guest_id);

-- Thêm quyền RBAC cho Lodging Queue & Export
INSERT INTO roles_permissions (role_name, permission) VALUES
('ROLE_CHAIN_ADMIN', 'ROLE_LODGING_VIEW'),
('ROLE_CHAIN_ADMIN', 'ROLE_LODGING_MANAGE'),
('ROLE_CHAIN_ADMIN', 'ROLE_LODGING_EXPORT'),
('ROLE_PROPERTY_MANAGER', 'ROLE_LODGING_VIEW'),
('ROLE_PROPERTY_MANAGER', 'ROLE_LODGING_MANAGE'),
('ROLE_PROPERTY_MANAGER', 'ROLE_LODGING_EXPORT'),
('ROLE_RECEPTIONIST', 'ROLE_LODGING_VIEW'),
('ROLE_RECEPTIONIST', 'ROLE_LODGING_MANAGE'),
('ROLE_RECEPTIONIST', 'ROLE_LODGING_EXPORT')
ON CONFLICT DO NOTHING;
