-- ==============================================================================
-- LỚP 3: CẤU HÌNH GIÁ ĐỘNG (DYNAMIC PRICING)
-- Bảng: pricing_rules (Phụ thu theo mùa vụ/ngày lễ)
-- ==============================================================================

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
    30.00, -- Phụ thu 30% ngày lễ
    hc.date,
    hc.date,
    'ACTIVE'
FROM hotels h
JOIN hotel_room_types hrt ON h.id = hrt.hotel_id
JOIN room_types rt ON hrt.room_type_id = rt.id
CROSS JOIN holiday_calendars hc
WHERE 
    -- Áp dụng cho các phòng "Sang trọng" trở lên vào dịp lễ
    rt.code IN ('DLX', 'STE', 'EXE', 'FAM')
    AND hc.date IN ('2026-09-02', '2027-02-06', '2027-02-07', '2027-02-08')
ON CONFLICT DO NOTHING;

-- Thêm phụ thu cuối tuần cho phòng Superior (SUP) và Deluxe (DLX) tại Đà Nẵng
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
    15.00, -- Phụ thu 15% cuối tuần
    '2026-07-01',
    '2027-06-30',
    'ACTIVE'
FROM hotels h
JOIN hotel_room_types hrt ON h.id = hrt.hotel_id
JOIN room_types rt ON hrt.room_type_id = rt.id
WHERE h.name = 'Viettel Grand Đà Nẵng' 
  AND rt.code IN ('SUP', 'DLX')
ON CONFLICT DO NOTHING;











-- ==============================================================================
-- LỚP 3: CẤU HÌNH CHIẾN LƯỢC GIẢM GIÁ (PROMOTION ENGINE)
-- Bảng: discount_rules
-- ==============================================================================

-- 1. GIẢM GIÁ LONG_STAY (Từ 3 đêm trở lên)
-- Map với DiscountCondition(minNights=3, ...)
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

-- 2. GIẢM GIÁ ĐẶT SỚM (EARLY_BIRD)
-- Map với DiscountCondition(minAdvanceBookingDays=30, ...)
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

-- 3. CHIẾN DỊCH FLASH SALE VỚI PROMO CODE
-- Map với DiscountCondition(promoCode="SUMMER2026", ...)
INSERT INTO discount_rules (
    hotel_room_type_id, campaign_id, rule_type, discount_type, discount_value, 
    start_date, end_date, conditions, status
)
SELECT 
    hrt.id, 
    (SELECT id FROM campaigns WHERE name = 'Flash Sale Chào Hè 2026'),
    'SPECIAL_CAMPAIGN', 'FIXED', 200000.00, 
    '2026-06-01', '2026-08-31',
    '{"promoCode": "SUMMER2026"}'::jsonb, 'ACTIVE'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Luxury Hà Nội'
ON CONFLICT DO NOTHING;









INSERT INTO surcharge_rules (hotel_room_type_id, age_policy_id, rule_type, conditions, adjustment_type, adjustment_value, start_date, end_date)
SELECT 
    hrt.id, 
    -- Lấy ID chính sách của Trẻ em thuộc về đúng Khách sạn đó
    (SELECT id FROM hotel_age_policies hap WHERE hap.hotel_id = h.id AND hap.guest_type = 'CHILD' LIMIT 1), 
    'EXTRA_PERSON', 
    NULL, 
    'FIXED', 300000.00, 
    '2026-06-01', '2027-06-30'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Luxury Hà Nội';


-- =========================================================================
-- 2. Phụ thu giường phụ (EXTRA_BED)
-- =========================================================================
INSERT INTO surcharge_rules (hotel_room_type_id, age_policy_id, rule_type, conditions, adjustment_type, adjustment_value, start_date, end_date)
SELECT 
    hrt.id, 
    NULL, 
    'EXTRA_BED', 
    NULL, 
    'FIXED', 1000000.00, 
    '2026-06-01', '2027-06-30'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Luxury Hà Nội';


-- =========================================================================
-- 3. Phụ thu sớm giờ (EARLY_CHECKIN) - UPDATE ĐA MỨC VÀO ĐÂY NÀY!
-- =========================================================================
INSERT INTO surcharge_rules (hotel_room_type_id, age_policy_id, rule_type, conditions, adjustment_type, adjustment_value, start_date, end_date)
SELECT 
    hrt.id, 
    NULL, 
    'EARLY_CHECKIN', 
    -- JSONB Đa mức (Time Tiers) xịn sò
    '{
      "time_tiers": [
        {
          "up_to_hours": 4.0,
          "adjustment_type": "PERCENT",
          "adjustment_value": 30.00
        },
        {
          "up_to_hours": 8.0,
          "adjustment_type": "PERCENT",
          "adjustment_value": 50.00
        },
        {
          "up_to_hours": null, 
          "adjustment_type": "PERCENT",
          "adjustment_value": 100.00
        }
      ]
    }'::jsonb, 
    'PERCENT', 0, 
    '2026-06-01', '2027-06-30'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Grand Đà Nẵng';


-- =========================================================================
-- 4. Phụ thu trễ giờ (LATE_CHECKOUT)
-- =========================================================================
INSERT INTO surcharge_rules (hotel_room_type_id, age_policy_id, rule_type, conditions, adjustment_type, adjustment_value, start_date, end_date)
SELECT 
    hrt.id, 
    NULL, 
    'LATE_CHECKOUT', 
    -- Khách out muộn 3 tiếng chém 30%, muộn 6 tiếng chém nửa ngày, muộn hơn 1 ngày
    '{
      "time_tiers": [
        {
          "up_to_hours": 3.0,
          "adjustment_type": "PERCENT",
          "adjustment_value": 30.00
        },
        {
          "up_to_hours": 6.0,
          "adjustment_type": "PERCENT",
          "adjustment_value": 50.00
        },
        {
          "up_to_hours": null,
          "adjustment_type": "PERCENT",
          "adjustment_value": 100.00
        }
      ]
    }'::jsonb, 
    'PERCENT', 0, 
    '2026-06-01', '2027-06-30'
FROM hotel_room_types hrt
JOIN hotels h ON hrt.hotel_id = h.id
WHERE h.name = 'Viettel Grand Đà Nẵng';