-- ==============================================================================
-- LỚP 3.6: CHÍNH SÁCH ĐỘ TUỔI TẠI TỪNG KHÁCH SẠN
-- Bảng: hotel_age_policies (Chính sách độ tuổi áp dụng cho Phụ phí)
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
        ('INFANT', 0, 5),   -- Trẻ sơ sinh/Em bé: 0 đến 5 tuổi (Thường free)
        ('CHILD', 6, 11),   -- Trẻ em: 6 đến 11 tuổi (Tính phụ thu trẻ em)
        ('ADULT', 12, 99)   -- Người lớn: Từ 12 tuổi trở lên (Tính phụ thu người lớn)
) AS p(guest_type, min_age, max_age)
ON CONFLICT (hotel_id, guest_type) DO UPDATE 
SET 
    min_age = EXCLUDED.min_age,
    max_age = EXCLUDED.max_age,
    updated_at = CURRENT_TIMESTAMP;


-- ==============================================================================
-- LỚP 3.7: CẤU HÌNH GIÁ ĐỘNG (DYNAMIC PRICING RULES)
-- Bảng: pricing_rules (Phụ thu theo mùa vụ/ngày lễ)
-- ==============================================================================

-- 1. Phụ thu Lễ Tết: Tăng 30% giá phòng vào các ngày Lễ tại Hà Nội và Đà Nẵng
INSERT INTO pricing_rules (
    hotel_room_type_id, 
    holiday_calendar_id, 
    rule_type, 
    adjustment_type, 
    adjustment_value, 
    start_date, 
    end_date, 
    status
)
SELECT 
    hrt.id,
    hc.id,
    'HOLIDAY',
    'PERCENT',
    30.00, 
    hc.date,
    hc.date,
    'ACTIVE'
FROM hotels h
JOIN hotel_room_types hrt ON h.id = hrt.hotel_id
JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN holiday_calendars hc
WHERE 
    h.name IN ('Viettel Luxury Hà Nội', 'Viettel Grand Đà Nẵng')
    AND rt.code IN ('DLX', 'STE', 'EXE', 'FAM')
    AND hc.date IN ('2026-09-02', '2027-02-06', '2027-02-07', '2027-02-08')
ON CONFLICT DO NOTHING;

-- 2. Phụ thu Cuối tuần: Tăng 15% vào cuối tuần tại Sapa
INSERT INTO pricing_rules (
    hotel_room_type_id, 
    rule_type, 
    adjustment_type, 
    adjustment_value, 
    start_date, 
    end_date, 
    status
)
SELECT 
    hrt.id,
    'WEEKEND',
    'PERCENT',
    15.00, 
    '2026-06-01',
    '2027-06-30',
    'ACTIVE'
FROM hotels h
JOIN hotel_room_types hrt ON h.id = hrt.hotel_id
JOIN room_types rt ON hrt.room_type_id = rt.id
WHERE h.name = 'Viettel Boutique Sapa' 
  AND rt.code IN ('STD', 'SUP')
ON CONFLICT DO NOTHING;


-- ==============================================================================
-- LỚP 3.8: CẤU HÌNH CHIẾN DỊCH VÀ GIẢM GIÁ (CAMPAIGNS & DISCOUNTS)
-- ==============================================================================

-- 1. Khởi tạo Campaign Mùa Hè 2026 tại Hà Nội
INSERT INTO campaigns (hotel_id, name, description, start_date, end_date, status)
SELECT 
    h.id, 'Flash Sale Chào Hè 2026', 'Ưu đãi kích cầu hè', '2026-06-01', '2026-08-31', 'ACTIVE'
FROM hotels h WHERE h.name = 'Viettel Luxury Hà Nội'
ON CONFLICT DO NOTHING;

-- 2. Giảm giá Long Stay (Lưu trú dài ngày)
INSERT INTO discount_rules (
    hotel_room_type_id, rule_type, discount_type, discount_value, 
    start_date, end_date, conditions, status
)
SELECT 
    hrt.id, 'LONG_STAY', 'PERCENT', 10.00, 
    '2026-06-01', '2027-06-30',
    '{"minNights": 3}'::jsonb, 'ACTIVE'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Luxury Hà Nội'
ON CONFLICT DO NOTHING;

-- 3. Giảm giá Early Bird (Đặt sớm 30 ngày)
INSERT INTO discount_rules (
    hotel_room_type_id, rule_type, discount_type, discount_value, 
    start_date, end_date, conditions, status
)
SELECT 
    hrt.id, 'EARLY_BIRD', 'PERCENT', 15.00, 
    '2026-06-01', '2027-06-30',
    '{"minAdvanceBookingDays": 30}'::jsonb, 'ACTIVE'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Luxury Hà Nội'
ON CONFLICT DO NOTHING;

-- 4. Mã giảm giá (Promo Code) gắn vào Campaign Mùa Hè
INSERT INTO discount_rules (
    hotel_room_type_id, campaign_id, rule_type, discount_type, discount_value, 
    start_date, end_date, conditions, status
)
SELECT 
    hrt.id, 
    (SELECT id FROM campaigns WHERE name = 'Flash Sale Chào Hè 2026' LIMIT 1),
    'SPECIAL_CAMPAIGN', 'FIXED', 200000.00, 
    '2026-06-01', '2026-08-31',
    '{"promoCode": "SUMMER2026"}'::jsonb, 'ACTIVE'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Luxury Hà Nội'
ON CONFLICT DO NOTHING;


-- ==============================================================================
-- LỚP 3.9: CẤU HÌNH PHỤ PHÍ KHÁC (SURCHARGES - THÊM NGƯỜI/GIƯỜNG/SỚM/TRỄ)
-- ==============================================================================

-- 1. Phụ thu thêm Người lớn & Trẻ em (Dựa trên Policy Độ tuổi) tại Hà Nội
INSERT INTO surcharge_rules (hotel_room_type_id, age_policy_id, rule_type, conditions, adjustment_type, adjustment_value, start_date, end_date)
SELECT 
    hrt.id, 
    hap.id, 
    'EXTRA_PERSON', 
    '{}'::jsonb, 
    'FIXED', 
    CASE WHEN hap.guest_type = 'CHILD' THEN 300000.00 ELSE 600000.00 END, 
    '2026-06-01', '2027-06-30'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
JOIN hotel_age_policies hap ON hap.hotel_id = h.id
WHERE h.name = 'Viettel Luxury Hà Nội' 
  AND hap.guest_type IN ('CHILD', 'ADULT');

-- 2. Phụ thu giường phụ (EXTRA_BED) tại Đà Nẵng
-- (Giường phụ thường có giá cao hơn vì tốn không gian và setup đồ vải)
INSERT INTO surcharge_rules (hotel_room_type_id, age_policy_id, rule_type, conditions, adjustment_type, adjustment_value, start_date, end_date)
SELECT 
    hrt.id, 
    NULL, 
    'EXTRA_BED', 
    '{}'::jsonb, 
    'FIXED', 1000000.00, 
    '2026-06-01', '2027-06-30'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Grand Đà Nẵng';

-- 3. Phụ thu nhận phòng sớm (EARLY_CHECKIN) Đa mức theo JSON Tiers
INSERT INTO surcharge_rules (hotel_room_type_id, age_policy_id, rule_type, conditions, adjustment_type, adjustment_value, start_date, end_date)
SELECT 
    hrt.id, 
    NULL, 
    'EARLY_CHECKIN', 
    '{
      "time_tiers": [
        { "up_to_hours": 4.0, "adjustment_type": "PERCENT", "adjustment_value": 30.00 },
        { "up_to_hours": 8.0, "adjustment_type": "PERCENT", "adjustment_value": 50.00 },
        { "up_to_hours": null, "adjustment_type": "PERCENT", "adjustment_value": 100.00 }
      ]
    }'::jsonb, 
    'PERCENT', 0, 
    '2026-06-01', '2027-06-30'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Grand Đà Nẵng';

-- 4. Phụ thu trả phòng trễ (LATE_CHECKOUT) Đa mức theo JSON Tiers
INSERT INTO surcharge_rules (hotel_room_type_id, age_policy_id, rule_type, conditions, adjustment_type, adjustment_value, start_date, end_date)
SELECT 
    hrt.id, 
    NULL, 
    'LATE_CHECKOUT', 
    '{
      "time_tiers": [
        { "up_to_hours": 3.0, "adjustment_type": "PERCENT", "adjustment_value": 30.00 },
        { "up_to_hours": 6.0, "adjustment_type": "PERCENT", "adjustment_value": 50.00 },
        { "up_to_hours": null, "adjustment_type": "PERCENT", "adjustment_value": 100.00 }
      ]
    }'::jsonb, 
    'PERCENT', 0, 
    '2026-06-01', '2027-06-30'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Grand Đà Nẵng';