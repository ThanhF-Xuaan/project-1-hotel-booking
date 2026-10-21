-- ==============================================================================
-- INCREMENTAL MIGRATION: 20261020-143000-create-policies-and-booking-snapshot.sql
-- Kiến trúc 3 bảng phân cấp chính sách Hủy / No-Show và Snapshot bất biến
-- ==============================================================================

-- ==============================================================================
-- BƯỚC 0: ĐẢM BẢO FUNCTION UPDATE_UPDATED_AT_COLUMN TỒN TẠI (IDEMPOTENT)
-- ==============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';


-- ==============================================================================
-- BƯỚC 0.5: TẠO RÀNG BUỘC UNIQUE (id, hotel_id) TRÊN HOTEL_ROOM_TYPES ĐỂ HỖ TRỢ COMPOSITE FK
-- ==============================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_hotel_room_types_id_hotel'
    ) THEN
        ALTER TABLE hotel_room_types 
        ADD CONSTRAINT uq_hotel_room_types_id_hotel UNIQUE (id, hotel_id);
    END IF;
END $$;


-- ==============================================================================
-- BẢNG 1: CANCELLATION_POLICIES (Chính sách Hủy phòng chuẩn hóa của Chuỗi)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS cancellation_policies (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    policy_code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    booking_type VARCHAR(20) NOT NULL DEFAULT 'FIT' CHECK (booking_type IN ('ALL', 'FIT', 'GIT', 'CORPORATE')),
    description TEXT,
    
    -- Mốc ngày tóm tắt (chỉ áp dụng cho Non-Tiered policies hoặc hiển thị tóm tắt UI)
    free_cancellation_before_days INT NOT NULL DEFAULT 7 CHECK (free_cancellation_before_days >= 0),
    
    -- Loại phí phạt hủy
    cancellation_fee_type VARCHAR(20) NOT NULL DEFAULT 'TIERED' 
        CHECK (cancellation_fee_type IN ('PERCENT', 'FIRST_NIGHT', 'FULL_DEPOSIT', 'FIXED_AMOUNT', 'TIERED')),
    
    -- Tỷ lệ phạt mặc định (CHỈ ÁP DỤNG khi cancellation_fee_type = 'PERCENT')
    default_penalty_percent NUMERIC(5,2) NOT NULL DEFAULT 100.00 
        CHECK (default_penalty_percent >= 0 AND default_penalty_percent <= 100),
    
    -- Bậc phạt chi tiết: NGUỒN SỰ THẬT (Single Source of Truth) khi cancellation_fee_type = 'TIERED' hoặc 'FULL_DEPOSIT'
    -- RÀNG BUỘC: tiers bắt buộc phải là JSON Array
    tiers JSONB NOT NULL DEFAULT '[]'::jsonb,
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    CONSTRAINT chk_cancellation_policies_tiers_array CHECK (jsonb_typeof(tiers) = 'array')
);

-- Partial Unique Index: Hỗ trợ soft-delete (cho phép tái sử dụng code sau khi xóa mềm)
DROP INDEX IF EXISTS uq_cancellation_policies_code_active;
CREATE UNIQUE INDEX uq_cancellation_policies_code_active 
ON cancellation_policies(policy_code) 
WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_cancellation_policies_lookup 
ON cancellation_policies(booking_type, is_active) 
WHERE is_deleted = FALSE;


-- ==============================================================================
-- BẢNG 2: NO_SHOW_POLICIES (Chính sách Khách không đến chuẩn hóa của Chuỗi)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS no_show_policies (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    policy_code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    booking_type VARCHAR(20) NOT NULL DEFAULT 'ALL' CHECK (booking_type IN ('ALL', 'FIT', 'GIT', 'CORPORATE')),
    description TEXT,
    
    -- Giờ chuyển No-Show (mặc định gắn liền với Night Audit 23:59:00)
    release_room_time TIME NOT NULL DEFAULT '23:59:00',
    
    -- Loại phạt No-Show
    penalty_type VARCHAR(30) NOT NULL DEFAULT 'PERCENT_FIRST_NIGHT_RATE' 
        CHECK (penalty_type IN ('PERCENT_FIRST_NIGHT_RATE', 'PERCENT_TOTAL_BOOKING', 'FULL_DEPOSIT', 'FIXED_AMOUNT')),
    
    -- Giá trị phạt (Tỷ lệ % từ 0-100, hoặc Số tiền cố định > 0 linh hoạt theo Currency)
    penalty_value NUMERIC(15,2) NOT NULL DEFAULT 100.00,
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Ràng buộc giá trị phạt:
    -- 1. % và PERCENT_FIRST_NIGHT_RATE / FULL_DEPOSIT giới hạn trong khoảng [0, 100]
    -- 2. FIXED_AMOUNT phải > 0 (không ép buộc số nguyên trong DB, hỗ trợ đa tiền tệ)
    CONSTRAINT chk_no_show_penalty_value CHECK (
        (penalty_type IN ('PERCENT_FIRST_NIGHT_RATE', 'PERCENT_TOTAL_BOOKING', 'FULL_DEPOSIT') 
            AND penalty_value >= 0 AND penalty_value <= 100) OR
        (penalty_type = 'FIXED_AMOUNT' AND penalty_value > 0)
    )
);

-- Partial Unique Index
DROP INDEX IF EXISTS uq_no_show_policies_code_active;
CREATE UNIQUE INDEX uq_no_show_policies_code_active 
ON no_show_policies(policy_code) 
WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_no_show_policies_lookup 
ON no_show_policies(booking_type, is_active) 
WHERE is_deleted = FALSE;


-- ==============================================================================
-- BẢNG 3: PROPERTY_POLICY_OVERRIDES (Bảng Ghi Đè & Gán Chính Sách Cơ Sở)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS property_policy_overrides (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    -- Khóa ngoại cơ sở & hạng phòng (Kiểu dữ liệu hoàn toàn tương thích với hotel_room_types)
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id) ON DELETE RESTRICT,
    hotel_room_type_id INT NULL,
    
    -- 2 FK NULLABLE: Cơ sở có thể override độc lập Hủy, No-Show hoặc cả hai
    cancellation_policy_id INT NULL REFERENCES cancellation_policies(id) ON DELETE RESTRICT,
    no_show_policy_id INT NULL REFERENCES no_show_policies(id) ON DELETE RESTRICT,
    
    -- CÁC TRƯỜNG CHO PHÉP GHI ĐÈ THIẾT YẾU (NULL = Dùng giá trị chuẩn của Chuỗi)
    free_cancellation_before_days_override INT NULL CHECK (free_cancellation_before_days_override IS NULL OR free_cancellation_before_days_override >= 0),
    no_show_release_time_override TIME NULL,
    
    -- Bắt buộc ghi nhận lý do cơ sở ghi đè
    override_reason VARCHAR(255) NOT NULL,
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- KHÓA NGOẠI KÉP (COMPOSITE FOREIGN KEY): Đảm bảo hotel_room_type_id phải thuộc đúng hotel_id
    CONSTRAINT fk_property_policy_overrides_hotel_room_type
        FOREIGN KEY (hotel_room_type_id, hotel_id)
        REFERENCES hotel_room_types(id, hotel_id)
        ON DELETE RESTRICT,

    -- Đảm bảo bản ghi có ý nghĩa: ít nhất phải gán policy hoặc override 1 trường
    CONSTRAINT chk_override_has_content CHECK (
        cancellation_policy_id IS NOT NULL OR 
        no_show_policy_id IS NOT NULL OR 
        free_cancellation_before_days_override IS NOT NULL OR 
        no_show_release_time_override IS NOT NULL
    )
);

-- Trigger Function: Ngăn chặn triệt để việc gán free_cancellation_before_days_override cho chính sách TIERED
CREATE OR REPLACE FUNCTION check_property_policy_override_compatibility()
RETURNS TRIGGER AS $$
DECLARE
    v_fee_type VARCHAR(20);
BEGIN
    IF NEW.cancellation_policy_id IS NOT NULL AND NEW.free_cancellation_before_days_override IS NOT NULL THEN
        SELECT cancellation_fee_type INTO v_fee_type 
        FROM cancellation_policies 
        WHERE id = NEW.cancellation_policy_id;
        
        IF v_fee_type = 'TIERED' THEN
            RAISE EXCEPTION 'free_cancellation_before_days_override cannot be set for TIERED cancellation policy (id: %)', NEW.cancellation_policy_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_property_policy_overrides_compat ON property_policy_overrides;
CREATE TRIGGER trg_property_policy_overrides_compat
    BEFORE INSERT OR UPDATE ON property_policy_overrides
    FOR EACH ROW EXECUTE FUNCTION check_property_policy_override_compatibility();

-- PostgreSQL 16: Unique Index sử dụng NULLS NOT DISTINCT
DROP INDEX IF EXISTS uq_property_policy_overrides_active;
CREATE UNIQUE INDEX uq_property_policy_overrides_active 
ON property_policy_overrides(hotel_id, hotel_room_type_id) 
NULLS NOT DISTINCT 
WHERE is_deleted = FALSE AND is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_property_policy_overrides_hotel 
ON property_policy_overrides(hotel_id, is_active) 
WHERE is_deleted = FALSE;


-- ==============================================================================
-- BỔ SUNG CỘT SNAPSHOT VÀO BẢNG BOOKING_DETAILS (Bảo Vệ Tính Bất Biến Tài Chính)
-- ==============================================================================
ALTER TABLE booking_details 
ADD COLUMN IF NOT EXISTS cancellation_policy_id INT NULL REFERENCES cancellation_policies(id) ON DELETE RESTRICT,
ADD COLUMN IF NOT EXISTS no_show_policy_id INT NULL REFERENCES no_show_policies(id) ON DELETE RESTRICT,
ADD COLUMN IF NOT EXISTS cancellation_policy_snapshot JSONB NULL,
ADD COLUMN IF NOT EXISTS no_show_policy_snapshot JSONB NULL;

CREATE INDEX IF NOT EXISTS idx_booking_details_cancel_policy ON booking_details(cancellation_policy_id);
CREATE INDEX IF NOT EXISTS idx_booking_details_noshow_policy ON booking_details(no_show_policy_id);


-- ==============================================================================
-- TRIGGERS AUTO-UPDATE UPDATED_AT (Idempotent với DROP TRIGGER IF EXISTS)
-- ==============================================================================
DROP TRIGGER IF EXISTS trg_cancellation_policies_updated_at ON cancellation_policies;
CREATE TRIGGER trg_cancellation_policies_updated_at 
    BEFORE UPDATE ON cancellation_policies 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trg_no_show_policies_updated_at ON no_show_policies;
CREATE TRIGGER trg_no_show_policies_updated_at 
    BEFORE UPDATE ON no_show_policies 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trg_property_policy_overrides_updated_at ON property_policy_overrides;
CREATE TRIGGER trg_property_policy_overrides_updated_at 
    BEFORE UPDATE ON property_policy_overrides 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ==============================================================================
-- DỮ LIỆU KHỞI TẠO CHUẨN TOÀN CHUỖI (Master Seed Data)
-- ==============================================================================
INSERT INTO cancellation_policies (policy_code, name, booking_type, description, free_cancellation_before_days, cancellation_fee_type, default_penalty_percent, tiers)
VALUES 
('CANCEL_FIT_STANDARD', 'Chính sách Hủy Khách lẻ Tiêu chuẩn Toàn Chuỗi', 'FIT', 
 'Miễn phí trước 7 ngày, phạt 50% từ 4-6 ngày, phạt 100% trong vòng 3 ngày (tính trên tổng tiền phòng).', 7, 'TIERED', 100.00, 
 '[{"min_days": 7, "max_days": null, "penalty_percent": 0.00, "description": "Hủy trước >= 7 ngày: Miễn phí (0%)"}, {"min_days": 4, "max_days": 6, "penalty_percent": 50.00, "description": "Hủy trước 4-6 ngày: Phạt 50% tổng tiền phòng"}, {"min_days": 0, "max_days": 3, "penalty_percent": 100.00, "description": "Hủy trong vòng 3 ngày: Phạt 100% tổng tiền phòng"}]'::jsonb),

('CANCEL_GIT_STANDARD', 'Chính sách Hủy Khách đoàn Tiêu chuẩn Toàn Chuỗi', 'GIT', 
 'Đoàn hủy trước Cut-off Date (>= 14 ngày): 0% phạt (hoàn cọc); Hủy sau Cut-off (< 14 ngày): phạt 100% tiền đặt cọc thực tế đã nhận.', 14, 'FULL_DEPOSIT', 100.00, 
 '[{"min_days": 14, "max_days": null, "penalty_percent": 0.00, "description": "Hủy trước Cut-off (>= 14 ngày): Không phạt (0% trên tiền cọc)"}, {"min_days": 0, "max_days": 13, "penalty_percent": 100.00, "description": "Hủy sau Cut-off (< 14 ngày): Mất toàn bộ tiền cọc thực tế (100% trên tiền cọc)"}]'::jsonb)
ON CONFLICT DO NOTHING;

INSERT INTO no_show_policies (policy_code, name, booking_type, description, release_room_time, penalty_type, penalty_value)
VALUES 
('NOSHOW_STANDARD', 'Chính sách Khách không đến (No-Show) Tiêu chuẩn Toàn Chuỗi', 'ALL', 
 'Khách không đến nhận phòng trước 23:59:00 ngày check-in sẽ bị phạt 100% tiền phòng đêm đầu tiên (cơ sở: giá phòng đêm đầu first_night_rate) và giải phóng phòng về quỹ bán.', '23:59:00', 'PERCENT_FIRST_NIGHT_RATE', 100.00)
ON CONFLICT DO NOTHING;
