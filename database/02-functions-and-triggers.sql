-- ==============================================================================
-- 7. FUNCTIONS, TRIGGERS & ADVANCED CONSTRAINTS (BTREE_GIST)
-- ==============================================================================

-- Bật extension (Bắt buộc phải có để dùng EXCLUDE với các cột INT, VARCHAR)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ------------------------------------------------------------------------------
-- 7.1. TỰ ĐỘNG CẬP NHẬT CỘT `updated_at` CHO TẤT CẢ CÁC BẢNG
-- ------------------------------------------------------------------------------
-- PostgreSQL mặc định chỉ set CURRENT_TIMESTAMP lúc INSERT. 
-- Để cột updated_at tự nhảy giờ khi có lệnh UPDATE, ta cần Trigger.

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Viết một khối DO ẩn danh (Anonymous DO block) để tự động quét toàn bộ database
-- và gắn Trigger này vào BẤT KỲ bảng nào có chứa cột 'updated_at'.
DO $$
DECLARE
    t record;
BEGIN
    FOR t IN
        SELECT table_name
        FROM information_schema.columns
        WHERE column_name = 'updated_at'
          AND table_schema = 'public'
    LOOP
        EXECUTE format('
            CREATE OR REPLACE TRIGGER set_updated_at
            BEFORE UPDATE ON %I
            FOR EACH ROW
            EXECUTE FUNCTION update_updated_at_column();
        ', t.table_name);
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- ------------------------------------------------------------------------------
-- 7.2. TỰ ĐỘNG SINH MÃ ĐẶT PHÒNG (BOOKING NUMBER) ĐẸP MẮT
-- ------------------------------------------------------------------------------
-- Ví dụ: BKG-20260620-0001
CREATE SEQUENCE IF NOT EXISTS booking_number_seq START 1;

CREATE OR REPLACE FUNCTION generate_booking_number()
RETURNS TRIGGER AS $$
BEGIN
    -- Nếu Backend truyền xuống mã rỗng, DB sẽ tự động sinh mã
    IF NEW.booking_number IS NULL OR NEW.booking_number = '' THEN
        NEW.booking_number := 'BKG-' 
                           || to_char(CURRENT_DATE, 'YYYYMMDD') 
                           || '-' 
                           || LPAD(nextval('booking_number_seq')::text, 4, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_generate_booking_number
BEFORE INSERT ON bookings
FOR EACH ROW
EXECUTE FUNCTION generate_booking_number();


-- ------------------------------------------------------------------------------
-- 7.3. TỰ ĐỘNG SINH MÃ HÓA ĐƠN DỊCH VỤ (SERVICE ORDER NUMBER)
-- ------------------------------------------------------------------------------
-- Ví dụ: SRV-20260620-0001
CREATE SEQUENCE IF NOT EXISTS service_order_seq START 1;

CREATE OR REPLACE FUNCTION generate_service_order_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.order_number IS NULL OR NEW.order_number = '' THEN
        NEW.order_number := 'SRV-' 
                         || to_char(CURRENT_DATE, 'YYYYMMDD') 
                         || '-' 
                         || LPAD(nextval('service_order_seq')::text, 4, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_generate_service_order_number
BEFORE INSERT ON service_orders
FOR EACH ROW
EXECUTE FUNCTION generate_service_order_number();


-- ------------------------------------------------------------------------------
-- 7.4. TRIGGER BẢO VỆ DỮ LIỆU CỐT LÕI (CHỐNG XÓA CỨNG - SOFT DELETE)
-- ------------------------------------------------------------------------------
-- Thay vì xóa hẳn (DELETE) một khách sạn hay loại phòng gây lỗi khóa ngoại, 
-- Trigger này sẽ chặn lệnh DELETE và chuyển nó thành UPDATE is_deleted = TRUE (Soft Delete).

CREATE OR REPLACE FUNCTION prevent_hard_delete_and_soft_delete()
RETURNS TRIGGER AS $$
BEGIN
    -- [ĐÃ SỬA]: Với các bảng có cột status, chuyển về INACTIVE. Nếu bảng không có cột status thì sẽ lỗi.
    -- Giải pháp an toàn nhất là CHỈ update `is_deleted = TRUE`. Logic 'INACTIVE' nên để Backend xử lý hoặc chia ra trigger riêng.
    
    EXECUTE format('UPDATE %I SET is_deleted = TRUE WHERE id = $1', TG_TABLE_NAME)
    USING OLD.id;
    
    -- Trả về NULL để hủy bỏ lệnh DELETE cứng ban đầu
    RETURN NULL; 
END;
$$ LANGUAGE plpgsql;

-- Áp dụng cho bảng Khách sạn (hotels)
CREATE OR REPLACE TRIGGER soft_delete_hotels
BEFORE DELETE ON hotels
FOR EACH ROW
EXECUTE FUNCTION prevent_hard_delete_and_soft_delete();

-- Áp dụng cho bảng Loại phòng hệ thống (room_types)
CREATE OR REPLACE TRIGGER soft_delete_room_types
BEFORE DELETE ON room_types
FOR EACH ROW
EXECUTE FUNCTION prevent_hard_delete_and_soft_delete();


-- ------------------------------------------------------------------------------
-- 7.5. EXCLUDE CONSTRAINTS (CHỐNG CHỒNG LẤP THỜI GIAN BẰNG BTREE_GIST)
-- ------------------------------------------------------------------------------

-- Quy tắc 1: Bảng `pricing_rules` (Luật giá)
-- Trong cùng 1 khoảng thời gian, 1 Loại phòng (hotel_room_type_id) 
-- không được phép có 2 luật giá cùng Cấp độ (rule_type) đè lên nhau.
ALTER TABLE pricing_rules
ADD CONSTRAINT ex_pricing_rule_overlap
EXCLUDE USING gist (
    hotel_room_type_id WITH =,
    rule_type WITH =,
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (is_deleted = FALSE);

-- Quy tắc 2: Bảng `campaigns` (Chiến dịch khuyến mãi)
-- Trong cùng 1 Khách sạn (hotel_id), không được phép có 2 Campaign trùng tên đang diễn ra cùng lúc.
ALTER TABLE campaigns
ADD CONSTRAINT ex_campaign_name_overlap
EXCLUDE USING gist (
    hotel_id WITH =,
    name WITH =,
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (is_deleted = FALSE);

-- Quy tắc 3: Bảng `surcharge_rules` (Phụ phí)
-- Phụ phí CHỈ chồng lấp nếu Khác hạng phòng HOẶC Khác độ tuổi (age_policy_id) HOẶC Khác Loại phụ phí (rule_type).
-- Ví dụ: Phụ phí Thêm người lớn KHÁC Phụ phí Thêm trẻ em. 
ALTER TABLE surcharge_rules
ADD CONSTRAINT ex_surcharge_rule_overlap
EXCLUDE USING gist (
    hotel_room_type_id WITH =,
    rule_type WITH =,
    -- Cần cast ID về dạng INT bình thường (bỏ qua nếu age_policy_id có thể NULL)
    COALESCE(age_policy_id, 0) WITH =, 
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (is_deleted = FALSE);

-- Quy tắc 4: Bảng `discount_rules` (Luật giảm giá)
-- Cùng 1 loại phòng, cùng 1 rule_type, không được trùng thời gian.
ALTER TABLE discount_rules
ADD CONSTRAINT ex_discount_rule_overlap
EXCLUDE USING gist (
    hotel_room_type_id WITH =,
    rule_type WITH =,
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (is_deleted = FALSE);