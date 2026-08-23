-- ==============================================================================
-- LỚP 2: ORGANIZATION BASE (Thực thể Doanh nghiệp)
-- Bảng: hotel_age_policies (Chính sách độ tuổi chung cho toàn chuỗi)
-- ==============================================================================

INSERT INTO hotel_age_policies (hotel_id, guest_type, min_age, max_age)
SELECT 
    h.id, 
    p.guest_type, 
    p.min_age, 
    p.max_age
FROM hotels h
CROSS JOIN (
    VALUES 
        ('INFANT', 0, 5),   -- Trẻ sơ sinh/Em bé: 0 đến 5 tuổi
        ('CHILD', 6, 11),   -- Trẻ em: 6 đến 11 tuổi
        ('ADULT', 12, 99)   -- Người lớn: Từ 12 tuổi trở lên
) AS p(guest_type, min_age, max_age)
ON CONFLICT (hotel_id, guest_type) DO UPDATE 
SET 
    min_age = EXCLUDED.min_age,
    max_age = EXCLUDED.max_age,
    updated_at = CURRENT_TIMESTAMP;

-- ==============================================================================
-- LỚP 3: CẤU HÌNH GIÁ & KHUYẾN MÃI (PRICING & CAMPAIGNS)
-- Bảng: campaigns (Các chiến dịch Marketing theo từng khách sạn)
-- ==============================================================================

-- 1. Chiến dịch mùa hè cho Viettel Luxury Hà Nội
INSERT INTO campaigns (hotel_id, name, description, start_date, end_date, status)
SELECT 
    (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội'),
    'Flash Sale Chào Hè 2026',
    'Giảm giá sâu các hạng phòng cao cấp và miễn phí dịch vụ đi kèm cho khách lưu trú tại thủ đô.',
    '2026-06-01',
    '2026-08-31',
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM campaigns 
    WHERE name = 'Flash Sale Chào Hè 2026' 
    AND hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội')
);

-- 2. Chiến dịch Lễ hội pháo hoa cho Viettel Grand Đà Nẵng
INSERT INTO campaigns (hotel_id, name, description, start_date, end_date, status)
SELECT 
    (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng'),
    'Lễ hội Pháo hoa Quốc tế Đà Nẵng (DIFF 2026)',
    'Kích cầu lưu trú mùa lễ hội pháo hoa quốc tế, ưu đãi đặc biệt cho phòng view biển.',
    '2026-06-15',
    '2026-07-31',
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM campaigns 
    WHERE name = 'Lễ hội Pháo hoa Quốc tế Đà Nẵng (DIFF 2026)' 
    AND hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng')
);

-- 3. Chiến dịch Đón tuyết cho Viettel Boutique Sapa
INSERT INTO campaigns (hotel_id, name, description, start_date, end_date, status)
SELECT 
    (SELECT id FROM hotels WHERE name = 'Viettel Boutique Sapa'),
    'Săn Mây Đón Rét Đầu Mùa 2026',
    'Chương trình ưu đãi đặt sớm (Early Bird) cho mùa đông Sapa 2026.',
    '2026-10-01',
    '2026-12-31',
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM campaigns 
    WHERE name = 'Săn Mây Đón Rét Đầu Mùa 2026' 
    AND hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Boutique Sapa')
);

-- ==============================================================================
-- LỚP 3.1: CẤU HÌNH HÀNG HÓA & DỊCH VỤ GỐC (CUỐN MENU CHUNG CHO POS/LỄ TÂN)
-- Đẩy dữ liệu vào bảng MENUS trước, sau đó tự động tách vào CATALOG_ITEMS và SERVICES
-- ==============================================================================

WITH inserted_menus AS (
    INSERT INTO menus (hotel_id, name, menu_type, tax_category_id, base_price, status)
    SELECT 
        h.id,
        c.name,
        c.menu_type,
        (SELECT id FROM tax_categories WHERE category_code = c.tax_code LIMIT 1), -- Lấy ID Thuế
        c.base_price,
        'ACTIVE'
    FROM hotels h
    CROSS JOIN (
        VALUES 
            -- Nhóm Ẩm thực (F&B) -> Thuộc loại PRODUCT
            ('Buffet Sáng Tiêu Chuẩn', 'PRODUCT', 'FOOD', 250000.00),
            ('Nước Suối Lavie 500ml', 'PRODUCT', 'NON_ALCOHOLIC_DRINK', 20000.00),
            ('Nước ngọt Coca Cola', 'PRODUCT', 'SUGARY_DRINK', 30000.00),
            ('Bia Heineken', 'PRODUCT', 'ALCOHOLIC_DRINK', 50000.00),
            ('Rượu Vang Đỏ Đà Lạt', 'PRODUCT', 'ALCOHOLIC_DRINK', 350000.00),

            -- Nhóm Dịch vụ phát sinh -> Thuộc loại SERVICE
            ('Dịch vụ Giặt là (kg)', 'SERVICE', 'LAUNDRY', 50000.00),
            ('Xe đưa đón sân bay (4 chỗ)', 'SERVICE', 'TRANSPORT', 350000.00),
            ('Thuê xe máy tay ga (Ngày)', 'SERVICE', 'RENTAL', 150000.00),
            ('Massage Trị Liệu Body (60p)', 'SERVICE', 'SPA', 500000.00),
            ('Trang trí phòng trăng mật', 'SERVICE', 'OTHER', 1000000.00)
    ) AS c(name, menu_type, tax_code, base_price)
    RETURNING id, menu_type
),
inserted_products AS (
    -- Nhồi ID của các món PRODUCT vào bảng catalog_items
    INSERT INTO catalog_items (id, stock_quantity)
    SELECT id, 100 -- Setup số lượng tồn kho ảo ban đầu
    FROM inserted_menus 
    WHERE menu_type = 'PRODUCT'
)
-- Nhồi ID của các món SERVICE vào bảng services
INSERT INTO services (id, pricing_type)
SELECT id, 'PER_USE' -- Mặc định là tính phí theo từng lần sử dụng
FROM inserted_menus 
WHERE menu_type = 'SERVICE';


-- ==============================================================================
-- LỚP 3: CẤU HÌNH LOẠI PHÒNG CHI TIẾT TẠI TỪNG KHÁCH SẠN
-- Bảng: hotel_room_types (Trái tim của hệ thống Inventory & Pricing)
-- ==============================================================================

INSERT INTO hotel_room_types (
    hotel_id, room_type_id, tax_category_id, -- ĐÃ BỔ SUNG KHÓA NGOẠI THUẾ
    standard_adults, standard_children, 
    max_adults, max_children, max_infants, max_total_guests, 
    max_beds, extra_beds, -- ĐÃ ĐỔI TÊN THÀNH extra_beds
    base_price, total_quantity, status
)
VALUES 
    -- ========================================================================
    -- 1. VIETTEL LUXURY HÀ NỘI (HẠNG 5 SAO)
    -- ========================================================================
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội'), 
        (SELECT id FROM room_types WHERE code = 'DLX'),
        (SELECT id FROM tax_categories LIMIT 1), -- Tự động gán Nhóm Thuế (Bạn có thể đổi WHERE code cụ thể)
        2, 0, 2, 1, 1, 3, 1, 1, 2500000.00, 20, 'ACTIVE'
    ),
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội'), 
        (SELECT id FROM room_types WHERE code = 'STE'),
        (SELECT id FROM tax_categories LIMIT 1),
        2, 0, 3, 2, 1, 4, 1, 1, 4500000.00, 10, 'ACTIVE'
    ),
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội'), 
        (SELECT id FROM room_types WHERE code = 'EXE'),
        (SELECT id FROM tax_categories LIMIT 1),
        2, 0, 2, 1, 1, 3, 1, 0, 10000000.00, 2, 'ACTIVE' 
    ),

    -- ========================================================================
    -- 2. VIETTEL GRAND ĐÀ NẴNG (HẠNG 4 SAO)
    -- ========================================================================
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng'), 
        (SELECT id FROM room_types WHERE code = 'SUP'),
        (SELECT id FROM tax_categories LIMIT 1),
        2, 0, 2, 1, 1, 3, 1, 1, 1200000.00, 30, 'ACTIVE'
    ),
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng'), 
        (SELECT id FROM room_types WHERE code = 'DLX'),
        (SELECT id FROM tax_categories LIMIT 1),
        2, 0, 3, 2, 1, 4, 2, 0, 1800000.00, 15, 'ACTIVE'
    ),
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng'), 
        (SELECT id FROM room_types WHERE code = 'FAM'),
        (SELECT id FROM tax_categories LIMIT 1),
        4, 0, 4, 2, 2, 6, 2, 1, 3000000.00, 5, 'ACTIVE' 
    ),

    -- ========================================================================
    -- 3. VIETTEL BOUTIQUE SAPA (HẠNG 3 SAO)
    -- ========================================================================
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Boutique Sapa'), 
        (SELECT id FROM room_types WHERE code = 'STD'),
        (SELECT id FROM tax_categories LIMIT 1),
        2, 0, 2, 1, 1, 2, 1, 0, 600000.00, 20, 'ACTIVE' 
    ),
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Boutique Sapa'), 
        (SELECT id FROM room_types WHERE code = 'SUP'),
        (SELECT id FROM tax_categories LIMIT 1),
        2, 0, 3, 1, 1, 4, 2, 0, 900000.00, 15, 'ACTIVE'
    )

ON CONFLICT (hotel_id, room_type_id) DO UPDATE 
SET 
    tax_category_id = EXCLUDED.tax_category_id,
    standard_adults = EXCLUDED.standard_adults,
    max_adults = EXCLUDED.max_adults,
    max_total_guests = EXCLUDED.max_total_guests,
    extra_beds = EXCLUDED.extra_beds, -- ĐÃ ĐỔI TÊN
    base_price = EXCLUDED.base_price,
    total_quantity = EXCLUDED.total_quantity,
    updated_at = CURRENT_TIMESTAMP;