-- ==============================================================================
-- LỚP 3: CẤU HÌNH CHI TIẾT LOẠI PHÒNG (GIƯỜNG)
-- Bảng: hotel_room_type_beds
-- ==============================================================================

INSERT INTO hotel_room_type_beds (hotel_room_type_id, room_bed_id, quantity)
VALUES 
    -- ========================================================================
    -- 1. VIETTEL LUXURY HÀ NỘI (HẠNG 5 SAO)
    -- ========================================================================
    -- DLX: 1 Giường King
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội') AND room_type_id = (SELECT id FROM room_types WHERE code = 'DLX')),
        (SELECT id FROM room_beds WHERE name = 'King Size Bed (Giường cỡ đại)'),
        1
    ),
    -- STE: 1 Giường Super King
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội') AND room_type_id = (SELECT id FROM room_types WHERE code = 'STE')),
        (SELECT id FROM room_beds WHERE name = 'Super King Size Bed (Giường siêu lớn)'),
        1
    ),
    -- EXE: 1 Giường Super King
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Luxury Hà Nội') AND room_type_id = (SELECT id FROM room_types WHERE code = 'EXE')),
        (SELECT id FROM room_beds WHERE name = 'Super King Size Bed (Giường siêu lớn)'),
        1
    ),

    -- ========================================================================
    -- 2. VIETTEL GRAND ĐÀ NẴNG (HẠNG 4 SAO)
    -- ========================================================================
    -- SUP: 1 Giường Double
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng') AND room_type_id = (SELECT id FROM room_types WHERE code = 'SUP')),
        (SELECT id FROM room_beds WHERE name = 'Double Bed (Giường đôi tiêu chuẩn)'),
        1
    ),
    -- DLX (Twin Setup): 2 Giường Single (Vì lúc nãy max_beds = 2)
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng') AND room_type_id = (SELECT id FROM room_types WHERE code = 'DLX')),
        (SELECT id FROM room_beds WHERE name = 'Single Bed (Giường đơn)'),
        2
    ),
    -- FAM: 2 Giường Queen Size (Dành cho gia đình 4 người lớn)
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Grand Đà Nẵng') AND room_type_id = (SELECT id FROM room_types WHERE code = 'FAM')),
        (SELECT id FROM room_beds WHERE name = 'Queen Size Bed (Giường đôi lớn)'),
        2
    ),

    -- ========================================================================
    -- 3. VIETTEL BOUTIQUE SAPA (HẠNG 3 SAO)
    -- ========================================================================
    -- STD: 1 Giường Double
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Boutique Sapa') AND room_type_id = (SELECT id FROM room_types WHERE code = 'STD')),
        (SELECT id FROM room_beds WHERE name = 'Double Bed (Giường đôi tiêu chuẩn)'),
        1
    ),
    -- SUP (Twin Setup): 2 Giường Single
    (
        (SELECT id FROM hotel_room_types WHERE hotel_id = (SELECT id FROM hotels WHERE name = 'Viettel Boutique Sapa') AND room_type_id = (SELECT id FROM room_types WHERE code = 'SUP')),
        (SELECT id FROM room_beds WHERE name = 'Single Bed (Giường đơn)'),
        2
    )

ON CONFLICT (hotel_room_type_id, room_bed_id) DO UPDATE 
SET quantity = EXCLUDED.quantity;










-- ==============================================================================
-- LỚP 3: CẤU HÌNH CHI TIẾT LOẠI PHÒNG (TIỆN ÍCH / VIEW)
-- Bảng: hotel_room_type_features
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. VIETTEL LUXURY HÀ NỘI (HẠNG 5 SAO) - View Thành phố, Tiện nghi đỉnh cao
-- ------------------------------------------------------------------------------
-- Phòng Deluxe (DLX)
INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Luxury Hà Nội' AND rt.code = 'DLX'
  AND rf.code IN ('CITY_VIEW', 'SHOWER', 'AIR_CONDITIONING', 'SAFE_BOX', 'HAIR_DRYER', 'FREE_WIFI', 'SMART_TV')
ON CONFLICT DO NOTHING;

-- Phòng Suite (STE)
INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Luxury Hà Nội' AND rt.code = 'STE'
  AND rf.code IN ('CITY_VIEW', 'BATHTUB', 'SHOWER', 'BALCONY', 'AIR_CONDITIONING', 'SOUNDPROOF', 'SAFE_BOX', 'HAIR_DRYER', 'FREE_WIFI', 'SMART_TV')
ON CONFLICT DO NOTHING;

-- Phòng Executive (EXE) - Có tất cả mọi thứ, bao gồm Netflix & Bàn ủi
INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Luxury Hà Nội' AND rt.code = 'EXE'
  AND rf.code IN ('CITY_VIEW', 'BATHTUB', 'SHOWER', 'BALCONY', 'AIR_CONDITIONING', 'SOUNDPROOF', 'SAFE_BOX', 'HAIR_DRYER', 'IRONING', 'FREE_WIFI', 'SMART_TV', 'NETFLIX')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------------------------
-- 2. VIETTEL GRAND ĐÀ NẴNG (HẠNG 4 SAO) - Đặc sản View Biển
-- ------------------------------------------------------------------------------
-- Phòng Superior (SUP)
INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Grand Đà Nẵng' AND rt.code = 'SUP'
  AND rf.code IN ('CITY_VIEW', 'SHOWER', 'AIR_CONDITIONING', 'HAIR_DRYER', 'FREE_WIFI', 'SMART_TV')
ON CONFLICT DO NOTHING;

-- Phòng Deluxe (DLX) & Family (FAM) - Hướng Biển & Ban công
INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Grand Đà Nẵng' AND rt.code IN ('DLX', 'FAM')
  AND rf.code IN ('SEA_VIEW', 'BALCONY', 'SHOWER', 'AIR_CONDITIONING', 'SAFE_BOX', 'HAIR_DRYER', 'FREE_WIFI', 'SMART_TV')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------------------------
-- 3. VIETTEL BOUTIQUE SAPA (HẠNG 3 SAO) - View Sân vườn/Núi, Tiện nghi cơ bản
-- ------------------------------------------------------------------------------
-- Cả 2 phòng STD và SUP
INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN room_features rf
WHERE h.name = 'Viettel Boutique Sapa' AND rt.code IN ('STD', 'SUP')
  AND rf.code IN ('GARDEN_VIEW', 'SHOWER', 'AIR_CONDITIONING', 'HAIR_DRYER', 'FREE_WIFI')
ON CONFLICT DO NOTHING;













-- ==============================================================================
-- LỚP 3.2: CẤU HÌNH GÓI DỊCH VỤ TẶNG KÈM/BÁN KÈM THEO LOẠI PHÒNG (BÁN LÚC BOOKING)
-- Bảng: hotel_room_type_catalog_items
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. MẶC ĐỊNH BAO GỒM BUFFET SÁNG & NƯỚC SUỐI 
-- => Loại: BẮT BUỘC (MANDATORY), Tính giá: THEO ĐÊM (PER_NIGHT), Giá: Tặng kèm 0đ
-- (Dành cho toàn bộ phòng của KS 5 Sao và 4 Sao)
-- ------------------------------------------------------------------------------
INSERT INTO hotel_room_type_catalog_items (hotel_room_type_id, catalog_item_id, item_usage, pricing_type, price)
SELECT hrt.id, ci.id, 'MANDATORY', 'PER_NIGHT', 0.00 
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
JOIN catalog_items ci ON ci.hotel_id = h.id
WHERE h.name IN ('Viettel Luxury Hà Nội', 'Viettel Grand Đà Nẵng')
  AND ci.name IN ('Buffet Sáng Tiêu Chuẩn', 'Nước Suối Lavie 500ml')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------------------------
-- 2. ĐẶC QUYỀN PHÒNG TỔNG THỐNG (EXE) TẠI HÀ NỘI
-- => Loại: BẮT BUỘC (MANDATORY), Tính giá: 1 LẦN/KỲ NGHỈ (PER_STAY), Giá: Tặng kèm 0đ
-- ------------------------------------------------------------------------------
INSERT INTO hotel_room_type_catalog_items (hotel_room_type_id, catalog_item_id, item_usage, pricing_type, price)
SELECT hrt.id, ci.id, 'MANDATORY', 'PER_STAY', 0.00 
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
JOIN room_types rt ON hrt.room_type_id = rt.id
JOIN catalog_items ci ON ci.hotel_id = h.id
WHERE h.name = 'Viettel Luxury Hà Nội' AND rt.code = 'EXE'
  AND ci.name = 'Rượu Vang Đỏ Đà Lạt'
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------------------------
-- 3. CẤU HÌNH CHO VIETTEL BOUTIQUE SAPA (3 SAO) - CHIẾN LƯỢC ÉP GIÁ
-- Phân cấp rõ ràng giữa hạng phòng bình dân và cao cấp
-- ------------------------------------------------------------------------------
-- A. Phòng STD (Standard): Khách muốn ăn Buffet thì phải mua thêm (OPTIONAL) theo ngày (PER_NIGHT) -> Giá 250k
INSERT INTO hotel_room_type_catalog_items (hotel_room_type_id, catalog_item_id, item_usage, pricing_type, price)
SELECT hrt.id, ci.id, 'OPTIONAL', 'PER_NIGHT', ci.base_price 
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id 
JOIN room_types rt ON hrt.room_type_id = rt.id 
JOIN catalog_items ci ON ci.hotel_id = h.id
WHERE h.name = 'Viettel Boutique Sapa' AND rt.code = 'STD' AND ci.name = 'Buffet Sáng Tiêu Chuẩn'
ON CONFLICT DO NOTHING;

-- B. Phòng SUP (Superior): Chịu đặt phòng giá đắt hơn nên được tặng Buffet (MANDATORY) -> Giá 0đ
INSERT INTO hotel_room_type_catalog_items (hotel_room_type_id, catalog_item_id, item_usage, pricing_type, price)
SELECT hrt.id, ci.id, 'MANDATORY', 'PER_NIGHT', 0.00 
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id 
JOIN room_types rt ON hrt.room_type_id = rt.id 
JOIN catalog_items ci ON ci.hotel_id = h.id
WHERE h.name = 'Viettel Boutique Sapa' AND rt.code = 'SUP' AND ci.name = 'Buffet Sáng Tiêu Chuẩn'
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------------------------
-- 4. CÁC DỊCH VỤ BÁN CHÉO KHÁC (ADD-ONS OPTIONAL ĐỂ LỰA CHỌN KHI ĐẶT PHÒNG)
-- ------------------------------------------------------------------------------
-- A. Cho thuê xe máy tại Sapa (OPTIONAL) -> Áp dụng nhân tiền THEO NGÀY/ĐÊM (PER_NIGHT)
INSERT INTO hotel_room_type_catalog_items (hotel_room_type_id, catalog_item_id, item_usage, pricing_type, price)
SELECT hrt.id, ci.id, 'OPTIONAL', 'PER_NIGHT', ci.base_price 
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id 
JOIN catalog_items ci ON ci.hotel_id = h.id
WHERE h.name = 'Viettel Boutique Sapa' AND ci.name = 'Thuê xe máy tay ga (Ngày)'
ON CONFLICT DO NOTHING;

-- B. Xe đưa đón sân bay tại HN & ĐN (OPTIONAL) -> Áp dụng tính 1 cuốc THEO KỲ NGHỈ (PER_STAY)
INSERT INTO hotel_room_type_catalog_items (hotel_room_type_id, catalog_item_id, item_usage, pricing_type, price)
SELECT hrt.id, ci.id, 'OPTIONAL', 'PER_STAY', ci.base_price 
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id 
JOIN catalog_items ci ON ci.hotel_id = h.id
WHERE h.name IN ('Viettel Luxury Hà Nội', 'Viettel Grand Đà Nẵng') AND ci.name = 'Xe đưa đón sân bay (4 chỗ)'
ON CONFLICT DO NOTHING;

-- C. Trang trí phòng trăng mật (OPTIONAL) -> Áp dụng trang trí 1 LẦN DUY NHẤT (PER_STAY)
INSERT INTO hotel_room_type_catalog_items (hotel_room_type_id, catalog_item_id, item_usage, pricing_type, price)
SELECT hrt.id, ci.id, 'OPTIONAL', 'PER_STAY', 1000000.00 -- Override giá cứng
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id 
JOIN catalog_items ci ON ci.hotel_id = h.id
WHERE ci.name = 'Trang trí phòng trăng mật'
ON CONFLICT DO NOTHING;










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
        ('Viettel Luxury Hà Nội', 'DLX', '102', 'READY'),    -- Đang dọn dẹp (Để test API Housekeeping)
        ('Viettel Luxury Hà Nội', 'DLX', '103', 'READY'),    -- Đang có khách
        ('Viettel Luxury Hà Nội', 'DLX', '104', 'READY'), 
        ('Viettel Luxury Hà Nội', 'DLX', '105', 'READY'), 
        ('Viettel Luxury Hà Nội', 'DLX', '106', 'READY'), 
        -- Tầng 2: Suite
        ('Viettel Luxury Hà Nội', 'STE', '201', 'READY'),
        ('Viettel Luxury Hà Nội', 'STE', '202', 'READY'), -- Đang bảo trì (Test chặn xếp phòng)
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