-- ==============================================================================
-- INCREMENTAL MIGRATION: CONVERT SOFT-DELETE UNIQUE CONSTRAINTS TO PARTIAL INDEXES
-- Purpose: Cho phép tái sử dụng/tạo lại mã (code, room_number, tax_code, etc.)
--          khi bản ghi cũ đã bị xóa mềm (is_deleted = TRUE).
-- Nguyên tắc: Tạo Partial Unique Index (WHERE is_deleted = FALSE) trước,
--             sau đó DROP constraint cũ để duy trì tính toàn vẹn dữ liệu.
-- ==============================================================================

-- 1. REGIONS (Khu vực / Vùng)
CREATE UNIQUE INDEX IF NOT EXISTS uq_regions_code_active 
    ON regions (code) WHERE is_deleted = FALSE;
ALTER TABLE regions DROP CONSTRAINT IF EXISTS regions_code_key;

-- 2. DEPARTMENTS (Phòng ban)
CREATE UNIQUE INDEX IF NOT EXISTS uq_departments_code_active 
    ON departments (code) WHERE is_deleted = FALSE;
ALTER TABLE departments DROP CONSTRAINT IF EXISTS departments_code_key;

-- 3. ROLES (Vai trò)
CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_code_active 
    ON roles (code) WHERE is_deleted = FALSE;
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_code_key;

-- 4. PERMISSIONS (Quyền hạn)
CREATE UNIQUE INDEX IF NOT EXISTS uq_permissions_action_resource_active 
    ON permissions (action, resource) WHERE is_deleted = FALSE;
ALTER TABLE permissions DROP CONSTRAINT IF EXISTS uq_permission_action_resource;

-- 5. ROOM TYPES (Loại phòng chuẩn chuỗi)
CREATE UNIQUE INDEX IF NOT EXISTS uq_room_types_code_active 
    ON room_types (code) WHERE is_deleted = FALSE;
ALTER TABLE room_types DROP CONSTRAINT IF EXISTS room_types_code_key;

-- 6. HOTEL ROOM TYPES (Hạng phòng cơ sở)
CREATE UNIQUE INDEX IF NOT EXISTS uq_hotel_room_types_active 
    ON hotel_room_types (hotel_id, room_type_id) WHERE is_deleted = FALSE;
ALTER TABLE hotel_room_types DROP CONSTRAINT IF EXISTS uk_hotel_room_type;
ALTER TABLE hotel_room_types DROP CONSTRAINT IF EXISTS hotel_room_types_hotel_id_room_type_id_key;

-- 7. ROOM INSTANCES (Phòng vật lý)
CREATE UNIQUE INDEX IF NOT EXISTS uq_room_instances_number_active 
    ON room_instances (hotel_id, room_number) WHERE is_deleted = FALSE;
ALTER TABLE room_instances DROP CONSTRAINT IF EXISTS uk_room_number;

-- 8. ROOM FEATURES (Tiện ích phòng)
CREATE UNIQUE INDEX IF NOT EXISTS uq_room_features_code_active 
    ON room_features (code) WHERE is_deleted = FALSE;
ALTER TABLE room_features DROP CONSTRAINT IF EXISTS room_features_code_key;

-- 9. ROOM BEDS (Loại giường)
CREATE UNIQUE INDEX IF NOT EXISTS uq_room_beds_name_active 
    ON room_beds (name) WHERE is_deleted = FALSE;
ALTER TABLE room_beds DROP CONSTRAINT IF EXISTS room_beds_name_key;

-- 10. TAX CATEGORIES & VAT RULES (Thuế & VAT)
CREATE UNIQUE INDEX IF NOT EXISTS uq_tax_categories_code_active 
    ON tax_categories (category_code) WHERE is_deleted = FALSE;
ALTER TABLE tax_categories DROP CONSTRAINT IF EXISTS tax_categories_category_code_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_vat_rules_code_active 
    ON vat_rules (vat_code) WHERE is_deleted = FALSE;
ALTER TABLE vat_rules DROP CONSTRAINT IF EXISTS vat_rules_vat_code_key;

-- 11. HOTEL AGE POLICIES (Chính sách độ tuổi)
CREATE UNIQUE INDEX IF NOT EXISTS uq_hotel_age_policies_active 
    ON hotel_age_policies (hotel_id, guest_type) WHERE is_deleted = FALSE;
ALTER TABLE hotel_age_policies DROP CONSTRAINT IF EXISTS hotel_age_policies_hotel_id_guest_type_key;
ALTER TABLE hotel_age_policies DROP CONSTRAINT IF EXISTS uk_hotel_guest_type;

-- 12. VOUCHERS (Mã giảm giá)
CREATE UNIQUE INDEX IF NOT EXISTS uq_vouchers_code_active 
    ON vouchers (voucher_code) WHERE is_deleted = FALSE;
ALTER TABLE vouchers DROP CONSTRAINT IF EXISTS vouchers_voucher_code_key;

-- 13. MASTER ITEMS (Mặt hàng F&B / Dịch vụ)
CREATE UNIQUE INDEX IF NOT EXISTS uq_master_items_code_active 
    ON master_items (code) WHERE is_deleted = FALSE;
ALTER TABLE master_items DROP CONSTRAINT IF EXISTS master_items_code_key;

-- 14. HOLIDAY CALENDARS (Lịch ngày lễ)
CREATE UNIQUE INDEX IF NOT EXISTS uq_holiday_name_date_active 
    ON holiday_calendars (name, date) WHERE is_deleted = FALSE;
ALTER TABLE holiday_calendars DROP CONSTRAINT IF EXISTS uq_holiday_name_date;

-- 15. COMPANIES (Doanh nghiệp đối tác)
CREATE UNIQUE INDEX IF NOT EXISTS uq_companies_tax_code_active 
    ON companies (tax_code) WHERE is_deleted = FALSE;
ALTER TABLE companies DROP CONSTRAINT IF EXISTS companies_tax_code_key;

-- 16. NIGHT AUDITS (Chốt sổ đêm)
CREATE UNIQUE INDEX IF NOT EXISTS uq_night_audits_hotel_date_active 
    ON night_audits (hotel_id, audit_date) WHERE is_deleted = FALSE;
ALTER TABLE night_audits DROP CONSTRAINT IF EXISTS chk_unique_hotel_night_audit;

-- 17. STAFFS (Tài khoản nhân sự)
CREATE UNIQUE INDEX IF NOT EXISTS uq_staffs_username_active 
    ON staffs (username) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_staffs_email_active 
    ON staffs (email) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_staffs_phone_active 
    ON staffs (phone) WHERE is_deleted = FALSE;
ALTER TABLE staffs DROP CONSTRAINT IF EXISTS staffs_username_key;
ALTER TABLE staffs DROP CONSTRAINT IF EXISTS staffs_email_key;
ALTER TABLE staffs DROP CONSTRAINT IF EXISTS staffs_phone_key;

