-- ==============================================================================
-- Migration: Tái cấu trúc bảng hotel_room_type_inclusions & Nạp Master Items nhóm đặc thù (Hướng A)
-- File: 20261111-193000-refactor-hotel-room-type-inclusions.sql
-- ==============================================================================

-- 1. Bổ sung các cột mới cho hotel_room_type_inclusions
ALTER TABLE hotel_room_type_inclusions
    ADD COLUMN IF NOT EXISTS menu_id INT REFERENCES menus(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS master_item_id INT REFERENCES master_items(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS frequency VARCHAR(30) NOT NULL DEFAULT 'PER_NIGHT',
    ADD COLUMN IF NOT EXISTS target_type VARCHAR(30) NOT NULL DEFAULT 'PER_ROOM',
    ADD COLUMN IF NOT EXISTS custom_display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description TEXT;

-- 2. Chuyển các cột cũ (reference_type, reference_id) thành nullable để đảm bảo tương thích ngược
ALTER TABLE hotel_room_type_inclusions ALTER COLUMN reference_type DROP NOT NULL;
ALTER TABLE hotel_room_type_inclusions ALTER COLUMN reference_id DROP NOT NULL;

-- 3. Di chuyển dữ liệu cũ: Map reference_id sang menu_id nếu ID tồn tại trong bảng menus
UPDATE hotel_room_type_inclusions i
SET menu_id = i.reference_id
WHERE i.menu_id IS NULL
  AND i.reference_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM menus m WHERE m.id = i.reference_id);

-- 4. Thêm các ràng buộc kiểm tra (Check Constraints)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_hrt_inclusions_ref') THEN
        ALTER TABLE hotel_room_type_inclusions ADD CONSTRAINT chk_hrt_inclusions_ref
            CHECK ((menu_id IS NOT NULL) OR (master_item_id IS NOT NULL) OR (reference_id IS NOT NULL));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_hrt_inclusions_freq') THEN
        ALTER TABLE hotel_room_type_inclusions ADD CONSTRAINT chk_hrt_inclusions_freq
            CHECK (frequency IN ('PER_NIGHT', 'PER_STAY'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_hrt_inclusions_target') THEN
        ALTER TABLE hotel_room_type_inclusions ADD CONSTRAINT chk_hrt_inclusions_target
            CHECK (target_type IN ('PER_ROOM', 'PER_ADULT', 'PER_GUEST'));
    END IF;
END $$;

-- 5. Tạo các chỉ mục tối ưu truy vấn (Indexes)
CREATE INDEX IF NOT EXISTS idx_hrt_inclusions_menu ON hotel_room_type_inclusions(menu_id);
CREATE INDEX IF NOT EXISTS idx_hrt_inclusions_master_item ON hotel_room_type_inclusions(master_item_id);
CREATE INDEX IF NOT EXISTS idx_hrt_inclusions_active_lookup ON hotel_room_type_inclusions(hotel_room_type_id, is_deleted);

-- 6. HƯỚNG A: Seed Data bộ Master Item nhóm đặc sản địa phương (Generic Local Specialties)
-- Cho phép khách sạn cơ sở tự do tạo món đặc thù địa phương với custom_name mà chuỗi vẫn quản lý được nhóm hàng & thuế.

INSERT INTO master_items (code, name, description, item_type, default_base_price, tax_category_id, unit, status)
SELECT 
    'LOCAL_FOOD_SPECIALTY',
    'Đặc sản ẩm thực địa phương',
    'Danh mục chuẩn đại diện cho các món ăn, đặc sản ẩm thực riêng biệt của từng khách sạn cơ sở',
    'PRODUCT',
    0.00,
    (SELECT id FROM tax_categories WHERE category_code = 'FOOD' LIMIT 1),
    'PHAN',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM master_items WHERE code = 'LOCAL_FOOD_SPECIALTY');

INSERT INTO master_items (code, name, description, item_type, default_base_price, tax_category_id, unit, status)
SELECT 
    'LOCAL_BEVERAGE_SPECIALTY',
    'Đồ uống / Rượu đặc sản địa phương',
    'Danh mục chuẩn đại diện cho đồ uống, rượu vang, trà đặc sản riêng biệt của từng khách sạn cơ sở',
    'PRODUCT',
    0.00,
    (SELECT id FROM tax_categories WHERE category_code = 'ALCOHOLIC_DRINK' LIMIT 1),
    'CHAI',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM master_items WHERE code = 'LOCAL_BEVERAGE_SPECIALTY');

INSERT INTO master_items (code, name, description, item_type, default_base_price, tax_category_id, unit, status)
SELECT 
    'LOCAL_SOUVENIR',
    'Quà lưu niệm / Nông sản bản địa',
    'Danh mục chuẩn đại diện cho quà lưu niệm, nông sản sấy khô, thủ công mỹ nghệ tại địa phương',
    'PRODUCT',
    0.00,
    (SELECT id FROM tax_categories WHERE category_code = 'OTHER' LIMIT 1),
    'HOP',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM master_items WHERE code = 'LOCAL_SOUVENIR');

INSERT INTO master_items (code, name, description, item_type, default_base_price, tax_category_id, pricing_type, unit, status)
SELECT 
    'LOCAL_EXPERIENCE_TOUR',
    'Tour trải nghiệm / Khám phá bản địa',
    'Danh mục chuẩn đại diện cho các dịch vụ trải nghiệm văn hóa, tour du lịch, trekking do khách sạn liên kết tổ chức',
    'SERVICE',
    0.00,
    (SELECT id FROM tax_categories WHERE category_code = 'OTHER' LIMIT 1),
    'PER_USE',
    'CHUYEN',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM master_items WHERE code = 'LOCAL_EXPERIENCE_TOUR');

INSERT INTO master_items (code, name, description, item_type, default_base_price, tax_category_id, pricing_type, unit, status)
SELECT 
    'LOCAL_WELLNESS_SERVICE',
    'Trị liệu / Chăm sóc sức khỏe bản địa',
    'Danh mục chuẩn đại diện cho các gói ngâm chân thảo dược, tắm lá thuốc, massage trị liệu đặc thù của vùng',
    'SERVICE',
    0.00,
    (SELECT id FROM tax_categories WHERE category_code = 'SPA' LIMIT 1),
    'PER_USE',
    'LUOT',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM master_items WHERE code = 'LOCAL_WELLNESS_SERVICE');

-- 7. Seed thêm các Master Item gói chuẩn chuỗi thường dùng làm Inclusions
INSERT INTO master_items (code, name, description, item_type, default_base_price, tax_category_id, pricing_type, unit, status)
SELECT 
    'CHAIN_BUFFET_BREAKFAST',
    'Buffet Sáng Tiêu Chuẩn Chuỗi',
    'Quyền lợi buffet sáng tiêu chuẩn áp dụng cho các hạng phòng cao cấp trên toàn hệ thống',
    'SERVICE',
    150000.00,
    (SELECT id FROM tax_categories WHERE category_code = 'FOOD' LIMIT 1),
    'PER_NIGHT',
    'PHAN',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM master_items WHERE code = 'CHAIN_BUFFET_BREAKFAST');

INSERT INTO master_items (code, name, description, item_type, default_base_price, tax_category_id, pricing_type, unit, status)
SELECT 
    'CHAIN_AIRPORT_SHUTTLE',
    'Dịch Vụ Đón Tiễn Sân Bay',
    'Miễn phí đưa đón sân bay một chiều hoặc hai chiều theo tiêu chuẩn phòng cao cấp',
    'SERVICE',
    250000.00,
    (SELECT id FROM tax_categories WHERE category_code = 'TRANSPORT' LIMIT 1),
    'PER_USE',
    'CHUYEN',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM master_items WHERE code = 'CHAIN_AIRPORT_SHUTTLE');

