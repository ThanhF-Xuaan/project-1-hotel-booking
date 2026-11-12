-- ==============================================================================
-- Migration: Phục hồi và Chuẩn hóa toàn bộ Master Items (Local & Chain)
-- File: 20261112-100000-restore-canonical-and-local-master-items.sql
-- ==============================================================================

-- 1. Cập nhật các bản ghi Master Items đã tồn tại theo mã code
UPDATE master_items m
SET
    name = v.name,
    description = v.description,
    item_type = v.item_type,
    default_base_price = v.default_base_price,
    tax_category_id = COALESCE((SELECT id FROM tax_categories WHERE category_code = v.tax_category_code LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
    pricing_type = v.pricing_type,
    unit = v.unit,
    status = v.status,
    is_deleted = false,
    updated_at = CURRENT_TIMESTAMP
FROM (
    VALUES
        -- NHÓM 1: BỘ MASTER ITEMS ĐẶC SẢN ĐỊA PHƯƠNG (HƯỚNG A - CƠ SỞ)
        ('LOCAL_FOOD_SPECIALTY', 'Đặc sản ẩm thực địa phương', 'Danh mục chuẩn đại diện cho các món ăn, đặc sản ẩm thực riêng biệt của từng khách sạn cơ sở', 'PRODUCT', 0.00, 'FOOD', 'PER_UNIT', 'PHAN', 'ACTIVE'),
        ('LOCAL_BEVERAGE_SPECIALTY', 'Đồ uống / Rượu đặc sản địa phương', 'Danh mục chuẩn đại diện cho đồ uống, rượu vang, trà đặc sản riêng biệt của từng khách sạn cơ sở', 'PRODUCT', 0.00, 'ALCOHOLIC_DRINK', 'PER_UNIT', 'CHAI', 'ACTIVE'),
        ('LOCAL_SOUVENIR', 'Quà lưu niệm / Nông sản bản địa', 'Danh mục chuẩn đại diện cho quà lưu niệm, nông sản sấy khô, thủ công mỹ nghệ tại địa phương', 'PRODUCT', 0.00, 'OTHER', 'PER_UNIT', 'HOP', 'ACTIVE'),
        ('LOCAL_EXPERIENCE_TOUR', 'Tour trải nghiệm / Khám phá bản địa', 'Danh mục chuẩn đại diện cho các dịch vụ trải nghiệm văn hóa, tour du lịch, trekking do khách sạn liên kết tổ chức', 'SERVICE', 0.00, 'OTHER', 'PER_USE', 'CHUYEN', 'ACTIVE'),
        ('LOCAL_WELLNESS_SERVICE', 'Trị liệu / Chăm sóc sức khỏe bản địa', 'Danh mục chuẩn đại diện cho các gói ngâm chân thảo dược, tắm lá thuốc, massage trị liệu đặc thù của vùng', 'SERVICE', 0.00, 'SPA', 'PER_USE', 'LUOT', 'ACTIVE'),

        -- NHÓM 2: BỘ MASTER ITEMS CHUẨN CHUỖI TOÀN HỆ THỐNG
        ('CHAIN_MINERAL_WATER', 'Nước Suối Lavie 500ml', 'Nước khoáng thiên nhiên đóng chai 500ml', 'PRODUCT', 15000.00, 'NON_ALCOHOLIC_DRINK', 'PER_UNIT', 'CHAI', 'ACTIVE'),
        ('CHAIN_COCA_COLA', 'Nước ngọt Coca Cola', 'Nước ngọt có gas lon 330ml', 'PRODUCT', 20000.00, 'SUGARY_DRINK', 'PER_UNIT', 'LON', 'ACTIVE'),
        ('CHAIN_HEINEKEN', 'Bia Heineken', 'Bia lon cao cấp 330ml', 'PRODUCT', 35000.00, 'ALCOHOLIC_DRINK', 'PER_UNIT', 'LON', 'ACTIVE'),
        ('CHAIN_DALAT_WINE', 'Rượu Vang Đỏ Đà Lạt', 'Rượu vang đỏ đặc sản Đà Lạt chai 750ml', 'PRODUCT', 250000.00, 'ALCOHOLIC_DRINK', 'PER_UNIT', 'CHAI', 'ACTIVE'),
        ('CHAIN_LAUNDRY', 'Dịch vụ Giặt là (kg)', 'Dịch vụ giặt ủi đồ quần áo tiêu chuẩn theo cân nặng', 'SERVICE', 30000.00, 'LAUNDRY', 'PER_UNIT', 'KG', 'ACTIVE'),
        ('CHAIN_MOTORBIKE_RENTAL', 'Thuê xe máy tay ga (Ngày)', 'Dịch vụ cho thuê xe máy tay ga theo ngày cho khách lưu trú', 'SERVICE', 150000.00, 'RENTAL', 'PER_UNIT', 'NGAY', 'ACTIVE'),
        ('CHAIN_BODY_MASSAGE', 'Massage Trị Liệu Body (60p)', 'Gói trị liệu massage toàn thân thư giãn 60 phút', 'SERVICE', 350000.00, 'SPA', 'PER_USE', 'LUOT', 'ACTIVE'),
        ('CHAIN_HONEYMOON_DECOR', 'Trang trí phòng trăng mật', 'Gói setup hoa tươi, nến, thiên nga gấp khăn và bánh kem kỷ niệm', 'SERVICE', 500000.00, 'OTHER', 'PER_STAY', 'GOI', 'ACTIVE'),
        ('CHAIN_BUFFET_BREAKFAST', 'Buffet Sáng Tiêu Chuẩn Chuỗi', 'Quyền lợi buffet sáng tiêu chuẩn áp dụng cho các hạng phòng cao cấp trên toàn hệ thống', 'SERVICE', 150000.00, 'FOOD', 'PER_NIGHT', 'PHAN', 'ACTIVE'),
        ('CHAIN_AIRPORT_SHUTTLE', 'Dịch Vụ Đón Tiễn Sân Bay', 'Miễn phí đưa đón sân bay một chiều hoặc hai chiều theo tiêu chuẩn phòng cao cấp', 'SERVICE', 250000.00, 'TRANSPORT', 'PER_USE', 'CHUYEN', 'ACTIVE')
) AS v(code, name, description, item_type, default_base_price, tax_category_code, pricing_type, unit, status)
WHERE m.code = v.code;

-- 2. Thêm mới các Master Items chưa từng tồn tại
INSERT INTO master_items (
    code,
    name,
    description,
    item_type,
    default_base_price,
    tax_category_id,
    pricing_type,
    unit,
    status,
    is_deleted
)
SELECT
    v.code,
    v.name,
    v.description,
    v.item_type,
    v.default_base_price,
    COALESCE((SELECT id FROM tax_categories WHERE category_code = v.tax_category_code LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
    v.pricing_type,
    v.unit,
    v.status,
    false
FROM (
    VALUES
        -- NHÓM 1: BỘ MASTER ITEMS ĐẶC SẢN ĐỊA PHƯƠNG (HƯỚNG A - CƠ SỞ)
        ('LOCAL_FOOD_SPECIALTY', 'Đặc sản ẩm thực địa phương', 'Danh mục chuẩn đại diện cho các món ăn, đặc sản ẩm thực riêng biệt của từng khách sạn cơ sở', 'PRODUCT', 0.00, 'FOOD', 'PER_UNIT', 'PHAN', 'ACTIVE'),
        ('LOCAL_BEVERAGE_SPECIALTY', 'Đồ uống / Rượu đặc sản địa phương', 'Danh mục chuẩn đại diện cho đồ uống, rượu vang, trà đặc sản riêng biệt của từng khách sạn cơ sở', 'PRODUCT', 0.00, 'ALCOHOLIC_DRINK', 'PER_UNIT', 'CHAI', 'ACTIVE'),
        ('LOCAL_SOUVENIR', 'Quà lưu niệm / Nông sản bản địa', 'Danh mục chuẩn đại diện cho quà lưu niệm, nông sản sấy khô, thủ công mỹ nghệ tại địa phương', 'PRODUCT', 0.00, 'OTHER', 'PER_UNIT', 'HOP', 'ACTIVE'),
        ('LOCAL_EXPERIENCE_TOUR', 'Tour trải nghiệm / Khám phá bản địa', 'Danh mục chuẩn đại diện cho các dịch vụ trải nghiệm văn hóa, tour du lịch, trekking do khách sạn liên kết tổ chức', 'SERVICE', 0.00, 'OTHER', 'PER_USE', 'CHUYEN', 'ACTIVE'),
        ('LOCAL_WELLNESS_SERVICE', 'Trị liệu / Chăm sóc sức khỏe bản địa', 'Danh mục chuẩn đại diện cho các gói ngâm chân thảo dược, tắm lá thuốc, massage trị liệu đặc thù của vùng', 'SERVICE', 0.00, 'SPA', 'PER_USE', 'LUOT', 'ACTIVE'),

        -- NHÓM 2: BỘ MASTER ITEMS CHUẨN CHUỖI TOÀN HỆ THỐNG
        ('CHAIN_MINERAL_WATER', 'Nước Suối Lavie 500ml', 'Nước khoáng thiên nhiên đóng chai 500ml', 'PRODUCT', 15000.00, 'NON_ALCOHOLIC_DRINK', 'PER_UNIT', 'CHAI', 'ACTIVE'),
        ('CHAIN_COCA_COLA', 'Nước ngọt Coca Cola', 'Nước ngọt có gas lon 330ml', 'PRODUCT', 20000.00, 'SUGARY_DRINK', 'PER_UNIT', 'LON', 'ACTIVE'),
        ('CHAIN_HEINEKEN', 'Bia Heineken', 'Bia lon cao cấp 330ml', 'PRODUCT', 35000.00, 'ALCOHOLIC_DRINK', 'PER_UNIT', 'LON', 'ACTIVE'),
        ('CHAIN_DALAT_WINE', 'Rượu Vang Đỏ Đà Lạt', 'Rượu vang đỏ đặc sản Đà Lạt chai 750ml', 'PRODUCT', 250000.00, 'ALCOHOLIC_DRINK', 'PER_UNIT', 'CHAI', 'ACTIVE'),
        ('CHAIN_LAUNDRY', 'Dịch vụ Giặt là (kg)', 'Dịch vụ giặt ủi đồ quần áo tiêu chuẩn theo cân nặng', 'SERVICE', 30000.00, 'LAUNDRY', 'PER_UNIT', 'KG', 'ACTIVE'),
        ('CHAIN_MOTORBIKE_RENTAL', 'Thuê xe máy tay ga (Ngày)', 'Dịch vụ cho thuê xe máy tay ga theo ngày cho khách lưu trú', 'SERVICE', 150000.00, 'RENTAL', 'PER_UNIT', 'NGAY', 'ACTIVE'),
        ('CHAIN_BODY_MASSAGE', 'Massage Trị Liệu Body (60p)', 'Gói trị liệu massage toàn thân thư giãn 60 phút', 'SERVICE', 350000.00, 'SPA', 'PER_USE', 'LUOT', 'ACTIVE'),
        ('CHAIN_HONEYMOON_DECOR', 'Trang trí phòng trăng mật', 'Gói setup hoa tươi, nến, thiên nga gấp khăn và bánh kem kỷ niệm', 'SERVICE', 500000.00, 'OTHER', 'PER_STAY', 'GOI', 'ACTIVE'),
        ('CHAIN_BUFFET_BREAKFAST', 'Buffet Sáng Tiêu Chuẩn Chuỗi', 'Quyền lợi buffet sáng tiêu chuẩn áp dụng cho các hạng phòng cao cấp trên toàn hệ thống', 'SERVICE', 150000.00, 'FOOD', 'PER_NIGHT', 'PHAN', 'ACTIVE'),
        ('CHAIN_AIRPORT_SHUTTLE', 'Dịch Vụ Đón Tiễn Sân Bay', 'Miễn phí đưa đón sân bay một chiều hoặc hai chiều theo tiêu chuẩn phòng cao cấp', 'SERVICE', 250000.00, 'TRANSPORT', 'PER_USE', 'CHUYEN', 'ACTIVE')
) AS v(code, name, description, item_type, default_base_price, tax_category_code, pricing_type, unit, status)
WHERE NOT EXISTS (
    SELECT 1 FROM master_items mi WHERE mi.code = v.code
);

-- 3. Đồng bộ lại master_item_id trên bảng menus theo đúng mã chuẩn
UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_BUFFET_BREAKFAST' LIMIT 1)
WHERE name ILIKE '%Buffet%'
  AND (master_item_id IS NULL OR master_item_id NOT IN (SELECT id FROM master_items WHERE code = 'CHAIN_BUFFET_BREAKFAST'));

UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_MINERAL_WATER' LIMIT 1)
WHERE (name ILIKE '%Lavie%' OR name ILIKE '%nước suối%' OR name ILIKE '%nuoc suoi%')
  AND (master_item_id IS NULL OR master_item_id NOT IN (SELECT id FROM master_items WHERE code = 'CHAIN_MINERAL_WATER'));

UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_COCA_COLA' LIMIT 1)
WHERE name ILIKE '%Coca%'
  AND (master_item_id IS NULL OR master_item_id NOT IN (SELECT id FROM master_items WHERE code = 'CHAIN_COCA_COLA'));

UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_HEINEKEN' LIMIT 1)
WHERE name ILIKE '%Heineken%'
  AND (master_item_id IS NULL OR master_item_id NOT IN (SELECT id FROM master_items WHERE code = 'CHAIN_HEINEKEN'));

UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_DALAT_WINE' LIMIT 1)
WHERE name ILIKE '%Vang%'
  AND (master_item_id IS NULL OR master_item_id NOT IN (SELECT id FROM master_items WHERE code = 'CHAIN_DALAT_WINE'));

UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_LAUNDRY' LIMIT 1)
WHERE name ILIKE '%Gi_t l_%'
  AND (master_item_id IS NULL OR master_item_id NOT IN (SELECT id FROM master_items WHERE code = 'CHAIN_LAUNDRY'));

UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_AIRPORT_SHUTTLE' LIMIT 1)
WHERE name ILIKE '%s_n bay%'
  AND (master_item_id IS NULL OR master_item_id NOT IN (SELECT id FROM master_items WHERE code = 'CHAIN_AIRPORT_SHUTTLE'));

UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_MOTORBIKE_RENTAL' LIMIT 1)
WHERE name ILIKE '%xe m_y%'
  AND (master_item_id IS NULL OR master_item_id NOT IN (SELECT id FROM master_items WHERE code = 'CHAIN_MOTORBIKE_RENTAL'));

UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_BODY_MASSAGE' LIMIT 1)
WHERE name ILIKE '%Massage%'
  AND (master_item_id IS NULL OR master_item_id NOT IN (SELECT id FROM master_items WHERE code = 'CHAIN_BODY_MASSAGE'));

UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_HONEYMOON_DECOR' LIMIT 1)
WHERE name ILIKE '%tr_ng m_t%'
  AND (master_item_id IS NULL OR master_item_id NOT IN (SELECT id FROM master_items WHERE code = 'CHAIN_HONEYMOON_DECOR'));

-- 4. Xóa bỏ an toàn các Master Items nhân bản lịch sử (MASTER_*) nếu còn tồn tại
DELETE FROM master_items
WHERE code LIKE 'MASTER_%';
