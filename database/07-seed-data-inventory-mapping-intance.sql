-- ==============================================================================
-- LỚP 3: CẤU HÌNH CHI TIẾT LOẠI PHÒNG (GIƯỜNG)
-- Bảng: hotel_room_type_beds
-- ==============================================================================

INSERT INTO hotel_room_type_beds (hotel_room_type_id, room_bed_id, base_quantity) -- ĐÃ ĐỔI TÊN THÀNH base_quantity
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
-- LỚP 3: CẤU HÌNH CHI TIẾT LOẠI PHÒNG (TIỆN ÍCH / VIEW)
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
-- LỚP 3.2: CẤU HÌNH GÓI DỊCH VỤ TẶNG KÈM THEO HẠNG PHÒNG (INCLUSIONS)
-- Bảng: hotel_room_type_inclusions (Đã thay thế cho hotel_room_type_catalog_items)
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
-- LỚP 3: KHO PHÒNG VẬT LÝ (INVENTORY INSTANCES)
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
        ('Viettel Luxury Hà Nội', 'DLX', '102', 'READY'),    -- Đang dọn dẹp
        ('Viettel Luxury Hà Nội', 'DLX', '103', 'READY'),    -- Đang có khách
        ('Viettel Luxury Hà Nội', 'DLX', '104', 'READY'), 
        ('Viettel Luxury Hà Nội', 'DLX', '105', 'READY'), 
        ('Viettel Luxury Hà Nội', 'DLX', '106', 'READY'), 
        -- Tầng 2: Suite
        ('Viettel Luxury Hà Nội', 'STE', '201', 'READY'),
        ('Viettel Luxury Hà Nội', 'STE', '202', 'READY'), -- Đang bảo trì
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