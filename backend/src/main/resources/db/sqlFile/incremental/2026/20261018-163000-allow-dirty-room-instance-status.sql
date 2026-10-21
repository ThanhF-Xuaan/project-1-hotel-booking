-- Cho phép trạng thái 'DIRTY' trong chk_room_instance_status để hỗ trợ chuyển phòng sang DIRTY ngay sau khi check-out
ALTER TABLE room_instances DROP CONSTRAINT IF EXISTS chk_room_instance_status;
ALTER TABLE room_instances ADD CONSTRAINT chk_room_instance_status
    CHECK (current_status IN ('READY', 'OCCUPIED', 'DIRTY', 'CLEANING', 'MAINTENANCE'));

-- Bổ sung quy tắc phụ thu mẫu cho Early Check-in và Late Check-out có cấu hình theo từng hạng phòng (hotel_room_types)
-- Dữ liệu này được kích hoạt cho các hạng phòng mẫu nếu chưa có cấu hình
INSERT INTO surcharge_rules (
    hotel_room_type_id,
    rule_type,
    pricing_type,
    conditions,
    adjustment_type,
    adjustment_value,
    start_date,
    end_date,
    status
)
SELECT 
    hrt.id,
    'LATE_CHECKOUT',
    'PER_STAY',
    '{"standard_time": "12:00", "tiers": [{"from_time": "12:00", "to_time": "15:00", "adjustment_type": "PERCENT", "adjustment_value": 30.0, "description": "Trả phòng từ 12:00 đến 15:00 (Phụ thu 30% theo cấu hình hạng phòng)"}, {"from_time": "15:00", "to_time": "18:00", "adjustment_type": "PERCENT", "adjustment_value": 50.0, "description": "Trả phòng từ 15:00 đến 18:00 (Phụ thu 50% theo cấu hình hạng phòng)"}, {"from_time": "18:00", "to_time": "23:59", "adjustment_type": "PERCENT", "adjustment_value": 100.0, "description": "Trả phòng sau 18:00 (Phụ thu 100% theo cấu hình hạng phòng)"}]}'::jsonb,
    'PERCENT',
    30.00,
    CURRENT_DATE - INTERVAL '30 days',
    CURRENT_DATE + INTERVAL '365 days',
    'ACTIVE'
FROM hotel_room_types hrt
WHERE NOT EXISTS (
    SELECT 1 FROM surcharge_rules sr 
    WHERE sr.hotel_room_type_id = hrt.id 
      AND sr.rule_type = 'LATE_CHECKOUT'
      AND sr.is_deleted = FALSE
);

INSERT INTO surcharge_rules (
    hotel_room_type_id,
    rule_type,
    pricing_type,
    conditions,
    adjustment_type,
    adjustment_value,
    start_date,
    end_date,
    status
)
SELECT 
    hrt.id,
    'EARLY_CHECKIN',
    'PER_STAY',
    '{"standard_time": "14:00", "tiers": [{"from_time": "00:00", "to_time": "06:00", "adjustment_type": "PERCENT", "adjustment_value": 100.0, "description": "Nhận phòng trước 06:00 (Phụ thu 100% theo cấu hình hạng phòng)"}, {"from_time": "06:00", "to_time": "09:00", "adjustment_type": "PERCENT", "adjustment_value": 50.0, "description": "Nhận phòng từ 06:00 đến 09:00 (Phụ thu 50% theo cấu hình hạng phòng)"}, {"from_time": "09:00", "to_time": "14:00", "adjustment_type": "PERCENT", "adjustment_value": 30.0, "description": "Nhận phòng từ 09:00 đến 14:00 (Phụ thu 30% theo cấu hình hạng phòng)"}]}'::jsonb,
    'PERCENT',
    30.00,
    CURRENT_DATE - INTERVAL '30 days',
    CURRENT_DATE + INTERVAL '365 days',
    'ACTIVE'
FROM hotel_room_types hrt
WHERE NOT EXISTS (
    SELECT 1 FROM surcharge_rules sr 
    WHERE sr.hotel_room_type_id = hrt.id 
      AND sr.rule_type = 'EARLY_CHECKIN'
      AND sr.is_deleted = FALSE
);
