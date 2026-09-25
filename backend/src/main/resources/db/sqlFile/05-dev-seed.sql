-- ==============================================================================
-- LỚP 2: ORGANIZATION BASE (Thực thể Doanh nghiệp & Cơ cấu tổ chức)
-- Bảng: regions, departments, hotels
-- ==============================================================================

-- 1. SEED REGIONS (Các Vùng Quản Lý)
INSERT INTO regions (code, name, description, status) VALUES 
    ('NORTH', 'Miền Bắc', 'Khu vực các tỉnh phía Bắc', 'ACTIVE'),
    ('CENTRAL', 'Miền Trung', 'Khu vực các tỉnh miền Trung & Tây Nguyên', 'ACTIVE'),
    ('SOUTH', 'Miền Nam', 'Khu vực các tỉnh phía Nam', 'ACTIVE');

-- 2. SEED DEPARTMENTS (Phòng Ban Tiêu Chuẩn)
INSERT INTO departments (code, name, status) VALUES 
    ('FRONT_OFFICE', 'Lễ tân Tiền sảnh', 'ACTIVE'),
    ('HOUSEKEEPING', 'Buồng phòng', 'ACTIVE'),
    ('FNB', 'Ẩm thực (F&B)', 'ACTIVE'),
    ('MAINTENANCE', 'Bảo trì Kỹ thuật', 'ACTIVE'),
    ('SALES', 'Kinh doanh & Tiếp thị', 'ACTIVE');

-- 3. SEED HOTELS (Khách sạn Cơ sở)
-- Sử dụng Subquery để lấy ID của Vùng (region_id) gán cho Khách sạn
INSERT INTO hotels (region_id, name, address, phone, check_in_time, check_out_time, service_fee_percent, status)
VALUES 
    -- 1. Hạng 5 Sao ở Miền Bắc (Chỉ bán phòng xịn: Deluxe, Suite, Executive)
    ((SELECT id FROM regions WHERE code = 'NORTH'), 'Viettel Luxury Hà Nội', 'Tòa nhà Viettel, Nam Từ Liêm, Hà Nội', '02466668888', '14:00:00', '12:00:00', 5.00, 'ACTIVE'),
    
    -- 2. Hạng 4 Sao ở Miền Trung (Bán phòng tầm trung: Superior, Deluxe)
    ((SELECT id FROM regions WHERE code = 'CENTRAL'), 'Viettel Grand Đà Nẵng', 'Số 2 Nguyễn Hữu Thọ, Đà Nẵng', '02366668888', '14:00:00', '12:00:00', 5.00, 'ACTIVE'),
    
    -- 3. Hạng 3 Sao ở Miền Bắc (Chỉ bán phòng cơ bản: Standard, Superior, không có Suite)
    ((SELECT id FROM regions WHERE code = 'NORTH'), 'Viettel Boutique Sapa', 'Thị trấn Sapa, Lào Cai', '02146668888', '14:00:00', '12:00:00', 5.00, 'ACTIVE');



-- ==============================================================================
-- LỚP 2.2: CHÍNH SÁCH ĐỘ TUỔI TẠI TỪNG KHÁCH SẠN
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
-- LỚP 3.2: CẤU HÌNH LOẠI PHÒNG CHI TIẾT TẠI TỪNG KHÁCH SẠN
-- Bảng: hotel_room_types (Trái tim của hệ thống Inventory & Pricing)
-- ==============================================================================

INSERT INTO hotel_room_types (
    hotel_id, room_type_id, tax_category_id, 
    standard_adults, standard_children, 
    max_adults, max_children, max_infants, max_total_guests, 
    max_beds, extra_beds, 
    base_price, total_quantity, status
)
VALUES 
    -- ========================================================================
    -- 1. VIETTEL LUXURY HÀ NỘI (HẠNG 5 SAO)
    -- ========================================================================
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội'), 
        (SELECT id FROM room_types WHERE code = 'DLX'),
        (SELECT id FROM tax_categories WHERE category_code = 'ROOM' LIMIT 1),
        2, 0, 2, 1, 1, 3, 1, 1, 2500000.00, 20, 'ACTIVE'
    ),
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội'), 
        (SELECT id FROM room_types WHERE code = 'STE'),
        (SELECT id FROM tax_categories WHERE category_code = 'ROOM' LIMIT 1),
        2, 0, 3, 2, 1, 4, 1, 1, 4500000.00, 10, 'ACTIVE'
    ),
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội'), 
        (SELECT id FROM room_types WHERE code = 'EXE'),
        (SELECT id FROM tax_categories WHERE category_code = 'ROOM' LIMIT 1),
        2, 0, 2, 1, 1, 3, 1, 0, 10000000.00, 2, 'ACTIVE' 
    ),

    -- ========================================================================
    -- 2. VIETTEL GRAND ĐÀ NẴNG (HẠNG 4 SAO)
    -- ========================================================================
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng'), 
        (SELECT id FROM room_types WHERE code = 'SUP'),
        (SELECT id FROM tax_categories WHERE category_code = 'ROOM' LIMIT 1),
        2, 0, 2, 1, 1, 3, 1, 1, 1200000.00, 30, 'ACTIVE'
    ),
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng'), 
        (SELECT id FROM room_types WHERE code = 'DLX'),
        (SELECT id FROM tax_categories WHERE category_code = 'ROOM' LIMIT 1),
        2, 0, 3, 2, 1, 4, 2, 0, 1800000.00, 15, 'ACTIVE'
    ),
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng'), 
        (SELECT id FROM room_types WHERE code = 'FAM'),
        (SELECT id FROM tax_categories WHERE category_code = 'ROOM' LIMIT 1),
        4, 0, 4, 2, 2, 6, 2, 1, 3000000.00, 5, 'ACTIVE' 
    ),

    -- ========================================================================
    -- 3. VIETTEL BOUTIQUE SAPA (HẠNG 3 SAO)
    -- ========================================================================
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Boutique Sapa'), 
        (SELECT id FROM room_types WHERE code = 'STD'),
        (SELECT id FROM tax_categories WHERE category_code = 'ROOM' LIMIT 1),
        2, 0, 2, 1, 1, 2, 1, 0, 600000.00, 20, 'ACTIVE' 
    ),
    (
        (SELECT id FROM hotels WHERE name = 'Viettel Boutique Sapa'), 
        (SELECT id FROM room_types WHERE code = 'SUP'),
        (SELECT id FROM tax_categories WHERE category_code = 'ROOM' LIMIT 1),
        2, 0, 3, 1, 1, 4, 2, 0, 900000.00, 15, 'ACTIVE'
    )

ON CONFLICT (hotel_id, room_type_id) DO UPDATE 
SET 
    tax_category_id = EXCLUDED.tax_category_id,
    standard_adults = EXCLUDED.standard_adults,
    max_adults = EXCLUDED.max_adults,
    max_total_guests = EXCLUDED.max_total_guests,
    extra_beds = EXCLUDED.extra_beds, 
    base_price = EXCLUDED.base_price,
    total_quantity = EXCLUDED.total_quantity,
    updated_at = CURRENT_TIMESTAMP;
-- ==============================================================================
-- LỚP 3.2: CẤU HÌNH CHI TIẾT LOẠI PHÒNG (GIƯỜNG)
-- Bảng: hotel_room_type_beds
-- ==============================================================================

INSERT INTO hotel_room_type_beds (hotel_room_type_id, room_bed_id, base_quantity)
VALUES 
    -- ========================================================================
    -- 1. VIETTEL LUXURY HÀ NỘI (HẠNG 5 SAO)
    -- ========================================================================
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội') AND room_type_id = (SELECT id FROM room_types WHERE code = 'DLX')),
        (SELECT id FROM room_beds WHERE name = 'King Size Bed (Giường cỡ đại)'),
        1
    ),
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội') AND room_type_id = (SELECT id FROM room_types WHERE code = 'STE')),
        (SELECT id FROM room_beds WHERE name = 'Super King Size Bed (Giường siêu lớn)'),
        1
    ),
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội') AND room_type_id = (SELECT id FROM room_types WHERE code = 'EXE')),
        (SELECT id FROM room_beds WHERE name = 'Super King Size Bed (Giường siêu lớn)'),
        1
    ),

    -- ========================================================================
    -- 2. VIETTEL GRAND ĐÀ NẴNG (HẠNG 4 SAO)
    -- ========================================================================
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng') AND room_type_id = (SELECT id FROM room_types WHERE code = 'SUP')),
        (SELECT id FROM room_beds WHERE name = 'Double Bed (Giường đôi tiêu chuẩn)'),
        1
    ),
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng') AND room_type_id = (SELECT id FROM room_types WHERE code = 'DLX')),
        (SELECT id FROM room_beds WHERE name = 'Single Bed (Giường đơn)'),
        2
    ),
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng') AND room_type_id = (SELECT id FROM room_types WHERE code = 'FAM')),
        (SELECT id FROM room_beds WHERE name = 'Queen Size Bed (Giường đôi lớn)'),
        2
    ),

    -- ========================================================================
    -- 3. VIETTEL BOUTIQUE SAPA (HẠNG 3 SAO)
    -- ========================================================================
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Boutique Sapa') AND room_type_id = (SELECT id FROM room_types WHERE code = 'STD')),
        (SELECT id FROM room_beds WHERE name = 'Double Bed (Giường đôi tiêu chuẩn)'),
        1
    ),
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Boutique Sapa') AND room_type_id = (SELECT id FROM room_types WHERE code = 'SUP')),
        (SELECT id FROM room_beds WHERE name = 'Single Bed (Giường đơn)'),
        2
    )

ON CONFLICT (hotel_room_type_id, room_bed_id) DO UPDATE 
SET base_quantity = EXCLUDED.base_quantity;


-- ==============================================================================
-- LỚP 3.3: CẤU HÌNH CHI TIẾT LOẠI PHÒNG (TIỆN ÍCH / VIEW)
-- Bảng: hotel_room_type_features
-- ==============================================================================

-- 1. VIETTEL LUXURY HÀ NỘI (HẠNG 5 SAO)
INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Luxury Hà Nội' AND rt.code = 'DLX'
  AND rf.code IN ('CITY_VIEW', 'SHOWER', 'AIR_CONDITIONING', 'SAFE_BOX', 'HAIR_DRYER', 'FREE_WIFI', 'SMART_TV')
ON CONFLICT DO NOTHING;

INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Luxury Hà Nội' AND rt.code = 'STE'
  AND rf.code IN ('CITY_VIEW', 'BATHTUB', 'SHOWER', 'BALCONY', 'AIR_CONDITIONING', 'SOUNDPROOF', 'SAFE_BOX', 'HAIR_DRYER', 'FREE_WIFI', 'SMART_TV')
ON CONFLICT DO NOTHING;

INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Luxury Hà Nội' AND rt.code = 'EXE'
  AND rf.code IN ('CITY_VIEW', 'BATHTUB', 'SHOWER', 'BALCONY', 'AIR_CONDITIONING', 'SOUNDPROOF', 'SAFE_BOX', 'HAIR_DRYER', 'IRONING', 'FREE_WIFI', 'SMART_TV', 'NETFLIX')
ON CONFLICT DO NOTHING;

-- 2. VIETTEL GRAND ĐÀ NẴNG (HẠNG 4 SAO)
INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Grand Đà Nẵng' AND rt.code = 'SUP'
  AND rf.code IN ('CITY_VIEW', 'SHOWER', 'AIR_CONDITIONING', 'HAIR_DRYER', 'FREE_WIFI', 'SMART_TV')
ON CONFLICT DO NOTHING;

INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Grand Đà Nẵng' AND rt.code IN ('DLX', 'FAM')
  AND rf.code IN ('SEA_VIEW', 'BALCONY', 'SHOWER', 'AIR_CONDITIONING', 'SAFE_BOX', 'HAIR_DRYER', 'FREE_WIFI', 'SMART_TV')
ON CONFLICT DO NOTHING;

-- 3. VIETTEL BOUTIQUE SAPA (HẠNG 3 SAO)
INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Boutique Sapa' AND rt.code IN ('STD', 'SUP')
  AND rf.code IN ('GARDEN_VIEW', 'SHOWER', 'AIR_CONDITIONING', 'HAIR_DRYER', 'FREE_WIFI')
ON CONFLICT DO NOTHING;


-- ==============================================================================
-- LỚP 3.4: CẤU HÌNH GÓI DỊCH VỤ TẶNG KÈM THEO HẠNG PHÒNG (INCLUSIONS)
-- Bảng: hotel_room_type_inclusions
-- ==============================================================================

-- 1. TẶNG KÈM BUFFET SÁNG & NƯỚC SUỐI CHO TOÀN BỘ KHÁCH SẠN 4 SAO VÀ 5 SAO
INSERT INTO hotel_room_type_inclusions (hotel_room_type_id, reference_type, reference_id, quantity)
SELECT hrt.id, 'POS_PRODUCT', m.id, 1
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
CROSS JOIN menus m
WHERE h.name IN ('Viettel Luxury Hà Nội', 'Viettel Grand Đà Nẵng')
  AND m.name IN ('Buffet Sáng Tiêu Chuẩn', 'Nước Suối Lavie 500ml')
  AND NOT EXISTS (
      SELECT 1 FROM hotel_room_type_inclusions i 
      WHERE i.hotel_room_type_id = hrt.id AND i.reference_id = m.id
  );

-- 2. TẶNG RƯỢU VANG CHO PHÒNG TỔNG THỐNG (EXE) TẠI HÀ NỘI
INSERT INTO hotel_room_type_inclusions (hotel_room_type_id, reference_type, reference_id, quantity)
SELECT hrt.id, 'POS_PRODUCT', m.id, 1
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN menus m
WHERE h.name = 'Viettel Luxury Hà Nội' AND rt.code = 'EXE'
  AND m.name = 'Rượu Vang Đỏ Đà Lạt'
  AND NOT EXISTS (
      SELECT 1 FROM hotel_room_type_inclusions i 
      WHERE i.hotel_room_type_id = hrt.id AND i.reference_id = m.id
  );

-- 3. TẶNG BUFFET CHO PHÒNG SUP TẠI SAPA (Phòng STD không được tặng)
INSERT INTO hotel_room_type_inclusions (hotel_room_type_id, reference_type, reference_id, quantity)
SELECT hrt.id, 'POS_PRODUCT', m.id, 1
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id 
JOIN room_types rt ON hrt.room_type_id = rt.id 
CROSS JOIN menus m
WHERE h.name = 'Viettel Boutique Sapa' AND rt.code = 'SUP' AND m.name = 'Buffet Sáng Tiêu Chuẩn'
  AND NOT EXISTS (
      SELECT 1 FROM hotel_room_type_inclusions i 
      WHERE i.hotel_room_type_id = hrt.id AND i.reference_id = m.id
  );


-- ==============================================================================
-- LỚP 3.5: KHO PHÒNG VẬT LÝ (INVENTORY INSTANCES)
-- Bảng: room_instances (Khởi tạo các phòng cụ thể)
-- ==============================================================================

INSERT INTO room_instances (hotel_id, hotel_room_type_id, room_number, current_status)
SELECT 
    h.id, 
    hrt.id, 
    v.room_number, 
    v.current_status
FROM (
    VALUES 
        -- ==========================================================
        -- 1. VIETTEL LUXURY HÀ NỘI (5 SAO)
        -- ==========================================================
        -- Tầng 1: Deluxe
        ('Viettel Luxury Hà Nội', 'DLX', '101', 'READY'),
        ('Viettel Luxury Hà Nội', 'DLX', '102', 'READY'),
        ('Viettel Luxury Hà Nội', 'DLX', '103', 'READY'),
        ('Viettel Luxury Hà Nội', 'DLX', '104', 'READY'), 
        ('Viettel Luxury Hà Nội', 'DLX', '105', 'READY'), 
        ('Viettel Luxury Hà Nội', 'DLX', '106', 'READY'), 
        -- Tầng 2: Suite
        ('Viettel Luxury Hà Nội', 'STE', '201', 'READY'),
        ('Viettel Luxury Hà Nội', 'STE', '202', 'READY'), 
        ('Viettel Luxury Hà Nội', 'STE', '203', 'READY'),
        ('Viettel Luxury Hà Nội', 'STE', '204', 'READY'),
        -- Tầng 3: Executive
        ('Viettel Luxury Hà Nội', 'EXE', '301', 'READY'),
        ('Viettel Luxury Hà Nội', 'EXE', '302', 'READY'),

        -- ==========================================================
        -- 2. VIETTEL GRAND ĐÀ NẴNG (4 SAO)
        -- ==========================================================
        -- Tầng 1: Superior
        ('Viettel Grand Đà Nẵng', 'SUP', '101', 'READY'),
        ('Viettel Grand Đà Nẵng', 'SUP', '102', 'READY'),
        ('Viettel Grand Đà Nẵng', 'SUP', '103', 'READY'),
        -- Tầng 2: Deluxe
        ('Viettel Grand Đà Nẵng', 'DLX', '201', 'READY'),
        ('Viettel Grand Đà Nẵng', 'DLX', '202', 'READY'),
        -- Tầng 3: Family
        ('Viettel Grand Đà Nẵng', 'FAM', '301', 'READY'),

        -- ==========================================================
        -- 3. VIETTEL BOUTIQUE SAPA (3 SAO)
        -- ==========================================================
        -- Tầng 1: Standard
        ('Viettel Boutique Sapa', 'STD', '101', 'READY'),
        ('Viettel Boutique Sapa', 'STD', '102', 'READY'),
        ('Viettel Boutique Sapa', 'STD', '103', 'READY'),
        -- Tầng 2: Superior
        ('Viettel Boutique Sapa', 'SUP', '201', 'READY'),
        ('Viettel Boutique Sapa', 'SUP', '202', 'READY')

) AS v(hotel_name, room_code, room_number, current_status)
JOIN hotels h ON h.name = v.hotel_name
JOIN room_types rt ON rt.code = v.room_code
JOIN hotel_room_types hrt ON hrt.hotel_id = h.id AND hrt.room_type_id = rt.id
ON CONFLICT (hotel_id, room_number) DO UPDATE 
SET 
    current_status = EXCLUDED.current_status,
    updated_at = CURRENT_TIMESTAMP;
