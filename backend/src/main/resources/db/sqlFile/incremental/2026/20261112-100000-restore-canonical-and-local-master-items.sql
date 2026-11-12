-- ==============================================================================
-- Migration: Phục hồi và Chuẩn hóa toàn bộ Master Items (Local & Chain)
-- File: 20261112-100000-restore-canonical-and-local-master-items.sql
-- ==============================================================================

-- 1. Nạp và phục hồi toàn bộ 15 Master Items (5 Đặc sản Cơ sở LOCAL_* & 10 Chuẩn chuỗi CHAIN_*)
-- Sử dụng ON CONFLICT (code) DO UPDATE để đảm bảo tương thích 100% trên cả Local, Dev, Staging và Production.
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
VALUES
    -- ========================================================================
    -- NHÓM 1: BỘ MASTER ITEMS ĐẶC SẢN ĐỊA PHƯƠNG (HƯỚNG A - QUẢN LÝ ĐẶC THÙ CƠ SỞ)
    -- ========================================================================
    (
        'LOCAL_FOOD_SPECIALTY',
        'Đặc sản ẩm thực địa phương',
        'Danh mục chuẩn đại diện cho các món ăn, đặc sản ẩm thực riêng biệt của từng khách sạn cơ sở',
        'PRODUCT',
        0.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'FOOD' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_UNIT',
        'PHAN',
        'ACTIVE',
        false
    ),
    (
        'LOCAL_BEVERAGE_SPECIALTY',
        'Đồ uống / Rượu đặc sản địa phương',
        'Danh mục chuẩn đại diện cho đồ uống, rượu vang, trà đặc sản riêng biệt của từng khách sạn cơ sở',
        'PRODUCT',
        0.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'ALCOHOLIC_DRINK' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_UNIT',
        'CHAI',
        'ACTIVE',
        false
    ),
    (
        'LOCAL_SOUVENIR',
        'Quà lưu niệm / Nông sản bản địa',
        'Danh mục chuẩn đại diện cho quà lưu niệm, nông sản sấy khô, thủ công mỹ nghệ tại địa phương',
        'PRODUCT',
        0.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'OTHER' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_UNIT',
        'HOP',
        'ACTIVE',
        false
    ),
    (
        'LOCAL_EXPERIENCE_TOUR',
        'Tour trải nghiệm / Khám phá bản địa',
        'Danh mục chuẩn đại diện cho các dịch vụ trải nghiệm văn hóa, tour du lịch, trekking do khách sạn liên kết tổ chức',
        'SERVICE',
        0.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'OTHER' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_USE',
        'CHUYEN',
        'ACTIVE',
        false
    ),
    (
        'LOCAL_WELLNESS_SERVICE',
        'Trị liệu / Chăm sóc sức khỏe bản địa',
        'Danh mục chuẩn đại diện cho các gói ngâm chân thảo dược, tắm lá thuốc, massage trị liệu đặc thù của vùng',
        'SERVICE',
        0.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'SPA' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_USE',
        'LUOT',
        'ACTIVE',
        false
    ),

    -- ========================================================================
    -- NHÓM 2: BỘ MASTER ITEMS CHUẨN CHUỖI TOÀN HỆ THỐNG (CANONICAL CHAIN ITEMS)
    -- ========================================================================
    (
        'CHAIN_MINERAL_WATER',
        'Nước Suối Lavie 500ml',
        'Nước khoáng thiên nhiên đóng chai 500ml',
        'PRODUCT',
        15000.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'NON_ALCOHOLIC_DRINK' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_UNIT',
        'CHAI',
        'ACTIVE',
        false
    ),
    (
        'CHAIN_COCA_COLA',
        'Nước ngọt Coca Cola',
        'Nước ngọt có gas lon 330ml',
        'PRODUCT',
        20000.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'SUGARY_DRINK' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_UNIT',
        'LON',
        'ACTIVE',
        false
    ),
    (
        'CHAIN_HEINEKEN',
        'Bia Heineken',
        'Bia lon cao cấp 330ml',
        'PRODUCT',
        35000.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'ALCOHOLIC_DRINK' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_UNIT',
        'LON',
        'ACTIVE',
        false
    ),
    (
        'CHAIN_DALAT_WINE',
        'Rượu Vang Đỏ Đà Lạt',
        'Rượu vang đỏ đặc sản Đà Lạt chai 750ml',
        'PRODUCT',
        250000.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'ALCOHOLIC_DRINK' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_UNIT',
        'CHAI',
        'ACTIVE',
        false
    ),
    (
        'CHAIN_LAUNDRY',
        'Dịch vụ Giặt là (kg)',
        'Dịch vụ giặt ủi đồ quần áo tiêu chuẩn theo cân nặng',
        'SERVICE',
        30000.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'LAUNDRY' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_UNIT',
        'KG',
        'ACTIVE',
        false
    ),
    (
        'CHAIN_MOTORBIKE_RENTAL',
        'Thuê xe máy tay ga (Ngày)',
        'Dịch vụ cho thuê xe máy tay ga theo ngày cho khách lưu trú',
        'SERVICE',
        150000.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'RENTAL' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_UNIT',
        'NGAY',
        'ACTIVE',
        false
    ),
    (
        'CHAIN_BODY_MASSAGE',
        'Massage Trị Liệu Body (60p)',
        'Gói trị liệu massage toàn thân thư giãn 60 phút',
        'SERVICE',
        350000.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'SPA' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_USE',
        'LUOT',
        'ACTIVE',
        false
    ),
    (
        'CHAIN_HONEYMOON_DECOR',
        'Trang trí phòng trăng mật',
        'Gói setup hoa tươi, nến, thiên nga gấp khăn và bánh kem kỷ niệm',
        'SERVICE',
        500000.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'OTHER' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_STAY',
        'GOI',
        'ACTIVE',
        false
    ),
    (
        'CHAIN_BUFFET_BREAKFAST',
        'Buffet Sáng Tiêu Chuẩn Chuỗi',
        'Quyền lợi buffet sáng tiêu chuẩn áp dụng cho các hạng phòng cao cấp trên toàn hệ thống',
        'SERVICE',
        150000.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'FOOD' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_NIGHT',
        'PHAN',
        'ACTIVE',
        false
    ),
    (
        'CHAIN_AIRPORT_SHUTTLE',
        'Dịch Vụ Đón Tiễn Sân Bay',
        'Miễn phí đưa đón sân bay một chiều hoặc hai chiều theo tiêu chuẩn phòng cao cấp',
        'SERVICE',
        250000.00,
        COALESCE((SELECT id FROM tax_categories WHERE category_code = 'TRANSPORT' LIMIT 1), (SELECT id FROM tax_categories LIMIT 1)),
        'PER_USE',
        'CHUYEN',
        'ACTIVE',
        false
    )

ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    item_type = EXCLUDED.item_type,
    default_base_price = EXCLUDED.default_base_price,
    tax_category_id = EXCLUDED.tax_category_id,
    pricing_type = EXCLUDED.pricing_type,
    unit = EXCLUDED.unit,
    status = EXCLUDED.status,
    is_deleted = false,
    updated_at = CURRENT_TIMESTAMP;

-- 2. Đồng bộ lại master_item_id trên bảng menus theo đúng mã chuẩn
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

-- 3. Xóa bỏ an toàn các Master Items nhân bản lịch sử (MASTER_*) nếu còn tồn tại
DELETE FROM master_items
WHERE code LIKE 'MASTER_%';
