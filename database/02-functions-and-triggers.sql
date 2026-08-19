-- ==============================================================================
-- 1. TỰ ĐỘNG CẬP NHẬT CỘT `updated_at` CHO TẤT CẢ CÁC BẢNG
-- ==============================================================================
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
-- Cách này giúp bạn không phải viết tay hàng chục cái CREATE TRIGGER.
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
            CREATE TRIGGER set_updated_at
            BEFORE UPDATE ON %I
            FOR EACH ROW
            EXECUTE FUNCTION update_updated_at_column();
        ', t.table_name);
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- ==============================================================================
-- 2. TỰ ĐỘNG SINH MÃ ĐẶT PHÒNG (BOOKING NUMBER) ĐẸP MẮT
-- ==============================================================================
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

CREATE TRIGGER trigger_generate_booking_number
BEFORE INSERT ON bookings
FOR EACH ROW
EXECUTE FUNCTION generate_booking_number();


-- ==============================================================================
-- 3. TỰ ĐỘNG SINH MÃ HÓA ĐƠN DỊCH VỤ (SERVICE ORDER NUMBER)
-- ==============================================================================
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

CREATE TRIGGER trigger_generate_service_order_number
BEFORE INSERT ON service_orders
FOR EACH ROW
EXECUTE FUNCTION generate_service_order_number();


-- ==============================================================================
-- 4. TRIGGER BẢO VỆ DỮ LIỆU CỐT LÕI (CHỐNG XÓA CỨNG)
-- ==============================================================================
-- Thay vì xóa hẳn (DELETE) một khách sạn hay loại phòng gây lỗi khóa ngoại, 
-- Trigger này sẽ chặn lệnh DELETE và chuyển nó thành UPDATE is_deleted = TRUE (Soft Delete).

CREATE OR REPLACE FUNCTION prevent_hard_delete_and_soft_delete()
RETURNS TRIGGER AS $$
BEGIN
    -- Chuyển trạng thái thành INACTIVE và is_deleted = TRUE
    EXECUTE format('UPDATE %I SET is_deleted = TRUE, status = ''INACTIVE'' WHERE id = $1', TG_TABLE_NAME)
    USING OLD.id;
    
    -- Trả về NULL để hủy bỏ lệnh DELETE cứng ban đầu
    RETURN NULL; 
END;
$$ LANGUAGE plpgsql;

-- Áp dụng cho bảng Khách sạn và Loại phòng
CREATE TRIGGER soft_delete_hotels
BEFORE DELETE ON hotels
FOR EACH ROW
EXECUTE FUNCTION prevent_hard_delete_and_soft_delete();

CREATE TRIGGER soft_delete_room_types
BEFORE DELETE ON room_types
FOR EACH ROW
EXECUTE FUNCTION prevent_hard_delete_and_soft_delete();




-- 1. Bật extension (Bắt buộc phải có để dùng EXCLUDE với các cột INT, VARCHAR)
CREATE EXTENSION IF NOT EXISTS btree_gist;


-- 3. Chặn chồng lấp cho PRICING RULES (Tăng giá)
ALTER TABLE pricing_rules
ADD CONSTRAINT ex_pricing_rule_overlap
EXCLUDE USING gist (
    hotel_room_type_id WITH =,
    rule_type WITH =,
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (is_deleted = FALSE);

-- 4. Chặn chồng lấp cho SURCHARGE RULES (Phụ phí)
-- Với phụ phí, cho phép trùng thời gian nếu Khác đối tượng (VD: Người lớn vs Trẻ em)

ALTER TABLE campaigns
ADD CONSTRAINT ex_campaign_name_overlap
EXCLUDE USING gist (
    hotel_id WITH =,
    name WITH =,
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (is_deleted = FALSE);