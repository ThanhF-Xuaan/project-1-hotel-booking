-- ==============================================================================
-- INCREMENTAL MIGRATION: REFACTOR & UNIFY GUEST TABLES (TASK T05)
-- 1. Đổi tên booking_guests (cũ) -> stay_guests (khách lưu trú thực tế tại phòng)
-- 2. Đổi tên guests (cũ) -> booking_guests (hồ sơ khách đặt phòng)
-- 3. Bổ sung các cột định danh & lưu trú BCA cho stay_guests
-- 4. Tạo bảng lodging_queues liên kết với stay_guests
-- 5. Cấu hình Triggers, Indexes và RBAC Permissions chuẩn
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. ĐỔI TÊN BẢNG AN TOÀN THEO ĐÚNG MÔ HÌNH NGHIỆP VỤ
-- ------------------------------------------------------------------------------

-- 1.1. Đổi tên booking_guests (cũ) thành stay_guests (nếu bảng booking_guests chứa booking_room_id)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'booking_guests' AND column_name = 'booking_room_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'stay_guests'
    ) THEN
        ALTER TABLE booking_guests RENAME TO stay_guests;
    END IF;
END $$;

-- 1.2. Đổi tên guests (cũ) thành booking_guests (nếu bảng guests chứa public_id)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'guests' AND column_name = 'public_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'booking_guests' AND column_name = 'public_id'
    ) THEN
        ALTER TABLE guests RENAME TO booking_guests;
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- 2. TẠO HOẶC MỞ RỘNG BẢNG STAY_GUESTS (Khách lưu trú thực tế tại phòng)
-- ------------------------------------------------------------------------------

-- 2.1. Khởi tạo bảng stay_guests nếu chưa tồn tại (trường hợp fresh install)
CREATE TABLE IF NOT EXISTS stay_guests (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id                SMALLINT REFERENCES hotels(id),
    booking_id              BIGINT REFERENCES bookings(id) ON DELETE SET NULL,
    booking_room_id         BIGINT REFERENCES booking_rooms(id) ON DELETE SET NULL,
    room_id                 INT REFERENCES room_instances(id) ON DELETE SET NULL,
    
    room_number             VARCHAR(20),
    first_name              VARCHAR(100),
    last_name               VARCHAR(100),
    full_name               VARCHAR(250) NOT NULL,
    date_of_birth           DATE,
    
    guest_type              VARCHAR(20) NOT NULL DEFAULT 'ADULT',
    gender                  VARCHAR(20),
    nationality             VARCHAR(100) NOT NULL DEFAULT 'Việt Nam',
    document_type           VARCHAR(20) NOT NULL DEFAULT 'CCCD',
    document_number         VARCHAR(50),
    
    permanent_address       TEXT,
    current_address         TEXT,
    
    check_in_time           TIMESTAMP WITH TIME ZONE,
    expected_check_out_time TIMESTAMP WITH TIME ZONE,
    actual_check_out_time   TIMESTAMP WITH TIME ZONE,
    reason_for_stay         VARCHAR(150) DEFAULT 'Du lịch',
    
    document_image_url      TEXT,
    image_purged_at         TIMESTAMP WITH TIME ZONE,

    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2.2. Bổ sung các cột mở rộng cho stay_guests nếu bảng đã tồn tại từ trước sau khi rename
ALTER TABLE stay_guests
    ADD COLUMN IF NOT EXISTS hotel_id SMALLINT REFERENCES hotels(id),
    ADD COLUMN IF NOT EXISTS booking_id BIGINT REFERENCES bookings(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS booking_room_id BIGINT REFERENCES booking_rooms(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS room_id INT REFERENCES room_instances(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS room_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS gender VARCHAR(20),
    ADD COLUMN IF NOT EXISTS nationality VARCHAR(100) NOT NULL DEFAULT 'Việt Nam',
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(20) NOT NULL DEFAULT 'CCCD',
    ADD COLUMN IF NOT EXISTS document_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS permanent_address TEXT,
    ADD COLUMN IF NOT EXISTS current_address TEXT,
    ADD COLUMN IF NOT EXISTS check_in_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS expected_check_out_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS actual_check_out_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS reason_for_stay VARCHAR(150) DEFAULT 'Du lịch',
    ADD COLUMN IF NOT EXISTS document_image_url TEXT,
    ADD COLUMN IF NOT EXISTS image_purged_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 2.3. Cập nhật date_of_birth từ birth_date (nếu có dữ liệu từ bảng booking_guests cũ)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'stay_guests' AND column_name = 'birth_date'
    ) THEN
        UPDATE stay_guests SET date_of_birth = birth_date WHERE date_of_birth IS NULL;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'stay_guests' AND column_name = 'identity_number'
    ) THEN
        UPDATE stay_guests SET document_number = identity_number WHERE document_number IS NULL;
    END IF;
END $$;

-- 2.4. Chỉ mục cho stay_guests
CREATE INDEX IF NOT EXISTS idx_stay_guests_hotel_checkin
    ON stay_guests(hotel_id, check_in_time);

CREATE INDEX IF NOT EXISTS idx_stay_guests_document
    ON stay_guests(document_number) WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_stay_guests_booking_room
    ON stay_guests(booking_room_id);

CREATE INDEX IF NOT EXISTS idx_stay_guests_booking
    ON stay_guests(booking_id);

-- 2.5. Trigger tự động cập nhật updated_at cho stay_guests
DROP TRIGGER IF EXISTS trg_stay_guests_updated_at ON stay_guests;
CREATE TRIGGER trg_stay_guests_updated_at
    BEFORE UPDATE ON stay_guests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ------------------------------------------------------------------------------
-- 3. TẠO BẢNG LODGING_QUEUES (Hàng chờ khai báo lưu trú gửi BCA)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS lodging_queues (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stay_guest_id       BIGINT NOT NULL REFERENCES stay_guests(id) ON DELETE CASCADE,
    hotel_id            SMALLINT NOT NULL REFERENCES hotels(id),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_message       TEXT,
    exported_at         TIMESTAMP WITH TIME ZONE,
    batch_reference     VARCHAR(100),

    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_lodging_queue_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'EXPORTED', 'FAILED', 'CANCELLED')
    )
);

-- Chỉ mục cho lodging_queues
CREATE INDEX IF NOT EXISTS idx_lodging_queues_status_hotel
    ON lodging_queues(hotel_id, status) WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_lodging_queues_stay_guest
    ON lodging_queues(stay_guest_id);

-- Trigger tự động cập nhật updated_at cho lodging_queues
DROP TRIGGER IF EXISTS trg_lodging_queues_updated_at ON lodging_queues;
CREATE TRIGGER trg_lodging_queues_updated_at
    BEFORE UPDATE ON lodging_queues
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ------------------------------------------------------------------------------
-- 4. PHÂN QUYỀN RBAC (Permissions & Role Permissions)
-- ------------------------------------------------------------------------------

-- Thêm các quyền hạn LODGING mới vào bảng permissions nếu chưa tồn tại
INSERT INTO permissions (action, resource, status) VALUES
    ('VIEW',   'LODGING', 'ACTIVE'),
    ('MANAGE', 'LODGING', 'ACTIVE'),
    ('EXPORT', 'LODGING', 'ACTIVE')
ON CONFLICT (action, resource) DO NOTHING;

-- Gán quyền cho CHAIN_ADMIN, PROPERTY_MANAGER và RECEPTIONIST vào role_permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')
  AND p.resource = 'LODGING'
  AND p.action IN ('VIEW', 'MANAGE', 'EXPORT')
ON CONFLICT DO NOTHING;
