-- ==============================================================================
-- SEED SAMPLE VOUCHERS (Mã giảm giá & Khuyến mại mẫu cho hệ thống)
-- Bảng: vouchers
-- ==============================================================================

-- 1. SUMMER2026 (Toàn chuỗi - Giảm 15% tiền phòng)
INSERT INTO vouchers (
    hotel_id, voucher_code, title, description,
    discount_type, discount_value, apply_scope, min_order_amount, max_discount_amount,
    total_quantity, used_quantity, max_usage_per_user,
    valid_from, valid_to, status, is_deleted
)
SELECT 
    NULL, 'SUMMER2026', 'Ưu Đãi Nghỉ Dưỡng Chào Hè 2026', 'Giảm 15% tiền phòng áp dụng cho toàn bộ các khách sạn trong chuỗi Hotel Grand',
    'PERCENT', 15.00, 'ROOM_ONLY', 0, 500000.00,
    200, 12, 1,
    CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '90 days', 'ACTIVE', FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM vouchers WHERE voucher_code = 'SUMMER2026' AND is_deleted = FALSE
);

-- 2. WELCOME10 (Toàn chuỗi - Giảm 10% tiền phòng cho khách hàng mới)
INSERT INTO vouchers (
    hotel_id, voucher_code, title, description,
    discount_type, discount_value, apply_scope, min_order_amount, max_discount_amount,
    total_quantity, used_quantity, max_usage_per_user,
    valid_from, valid_to, status, is_deleted
)
SELECT 
    NULL, 'WELCOME10', 'Tri Ân Khách Hàng Mới', 'Giảm 10% tiền phòng cho lần đặt phòng đầu tiên tại hệ thống',
    'PERCENT', 10.00, 'ROOM_ONLY', 0, 300000.00,
    500, 45, 1,
    CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP + INTERVAL '365 days', 'ACTIVE', FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM vouchers WHERE voucher_code = 'WELCOME10' AND is_deleted = FALSE
);

-- 3. GRAND500 (Toàn chuỗi - Giảm cố định 500.000đ toàn đơn)
INSERT INTO vouchers (
    hotel_id, voucher_code, title, description,
    discount_type, discount_value, apply_scope, min_order_amount, max_discount_amount,
    total_quantity, used_quantity, max_usage_per_user,
    valid_from, valid_to, status, is_deleted
)
SELECT 
    NULL, 'GRAND500', 'Voucher Giảm 500.000đ Toàn Đơn', 'Giảm trực tiếp 500.000đ cho đơn đặt phòng từ 3.000.000đ trở lên',
    'FIXED', 500000.00, 'TOTAL_BOOKING', 3000000.00, NULL,
    100, 8, 1,
    CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '60 days', 'ACTIVE', FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM vouchers WHERE voucher_code = 'GRAND500' AND is_deleted = FALSE
);

-- 4. HANOIVIP (Cơ sở Hà Nội - Giảm 20% tiền phòng)
INSERT INTO vouchers (
    hotel_id, voucher_code, title, description,
    discount_type, discount_value, apply_scope, min_order_amount, max_discount_amount,
    total_quantity, used_quantity, max_usage_per_user,
    valid_from, valid_to, status, is_deleted
)
SELECT 
    (SELECT id FROM hotels WHERE name ILIKE '%Hà Nội%' LIMIT 1),
    'HANOIVIP', 'Ưu Đãi Đặc Quyền Cơ Sở Grand Hà Nội', 'Giảm 20% tiền phòng cho khách hàng lưu trú tại Viettel Luxury Hà Nội',
    'PERCENT', 20.00, 'ROOM_ONLY', 1500000.00, 1000000.00,
    50, 3, 1,
    CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '45 days', 'ACTIVE', FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM vouchers WHERE voucher_code = 'HANOIVIP' AND is_deleted = FALSE
);

