-- ==============================================================================
-- TẠO BẢNG VOUCHERS (Mã giảm giá chủ động từ người dùng / Chiến dịch phát hành mã)
-- ==============================================================================
CREATE TABLE vouchers (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    -- Khách sạn áp dụng (NULL = Toàn chuỗi áp dụng)
    hotel_id SMALLINT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    
    -- Mã code voucher (Ví dụ: NEWYEAR10, TET2027, WELCOME50)
    voucher_code VARCHAR(50) UNIQUE NOT NULL,
    
    title VARCHAR(150) NOT NULL,
    description TEXT,
    
    -- Loại giảm giá: 'PERCENT' (% trên tiền phòng) hoặc 'FIXED' (số tiền cụ thể)
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    discount_value NUMERIC(15,2) NOT NULL CHECK (discount_value > 0),
    
    -- Phạm vi áp dụng: 'ROOM_ONLY' (Chỉ trừ tiền phòng), 'TOTAL_BOOKING' (Toàn đơn), 'SERVICE_ONLY'
    apply_scope VARCHAR(20) NOT NULL DEFAULT 'ROOM_ONLY' CHECK (apply_scope IN ('ROOM_ONLY', 'TOTAL_BOOKING', 'SERVICE_ONLY')),
    
    -- Điều kiện đơn hàng tối thiểu
    min_order_amount NUMERIC(15,2) DEFAULT 0,
    
    -- Mức giảm tối đa nếu discount_type là 'PERCENT' (Tránh thất thoát doanh thu)
    max_discount_amount NUMERIC(15,2) NULL,
    
    -- Giới hạn số lượng phát hành và số lượt đã dùng
    total_quantity INT NOT NULL DEFAULT 100,
    used_quantity INT NOT NULL DEFAULT 0,
    max_usage_per_user SMALLINT NOT NULL DEFAULT 1,
    
    -- Thời hạn hiệu lực
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITH TIME ZONE NOT NULL,
    
    status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED')),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_voucher_dates CHECK (valid_from <= valid_to),
    CONSTRAINT chk_voucher_quantity CHECK (used_quantity <= total_quantity)
);

-- ==============================================================================
-- TẠO BẢNG VOUCHER_USAGES (Lưu vết người đặt phòng sử dụng voucher)
-- ==============================================================================
CREATE TABLE voucher_usages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voucher_id INT NOT NULL REFERENCES vouchers(id) ON DELETE CASCADE,
    booking_guest_id BIGINT NOT NULL REFERENCES booking_guests(id) ON DELETE CASCADE,
    discount_amount NUMERIC(15,2) NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_voucher_usage_discount CHECK (discount_amount >= 0)
);

CREATE INDEX idx_vouchers_code_status ON vouchers(voucher_code, status, is_deleted);
CREATE INDEX idx_voucher_usages_guest ON voucher_usages(voucher_id, booking_guest_id);
