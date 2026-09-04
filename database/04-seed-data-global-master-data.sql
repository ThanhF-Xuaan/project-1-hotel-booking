-- ==============================================================================
-- 2. DỮ LIỆU MẪU (SEED DATA) ĐỂ TEST API
-- ==============================================================================

INSERT INTO roles (code, name, status)
VALUES 
    ('SUPER_ADMIN', 'Quản trị viên hệ thống', 'ACTIVE'),
    ('HOTEL_MANAGER', 'Quản lý khách sạn', 'ACTIVE'),
    ('RECEPTIONIST', 'Nhân viên Lễ tân', 'ACTIVE'),
    ('HOUSEKEEPING', 'Nhân viên Buồng phòng', 'ACTIVE');



INSERT INTO permissions (action, resource, status)
VALUES 
    -- 1. Phân hệ Quản trị trực quan (Admin Dashboard)
    ('VIEW', 'DASHBOARD', 'ACTIVE'),       -- Xem Timeline Ma trận phòng
    ('VIEW', 'REPORTS', 'ACTIVE'),         -- Xem Báo cáo tỷ lệ lấp đầy

    -- 2. Phân hệ Xử lý Giao dịch & Vận hành (Booking & Operations - Nhánh 3 & 4)
    ('VIEW', 'BOOKING', 'ACTIVE'),         -- Xem danh sách đặt phòng
    ('CREATE', 'BOOKING', 'ACTIVE'),       -- Lễ tân đặt phòng trực tiếp tại quầy (Walk-in)
    ('UPDATE', 'BOOKING', 'ACTIVE'),       -- Thao tác Check-in, Check-out, Đổi phòng
    ('CANCEL', 'BOOKING', 'ACTIVE'),       -- Hủy phòng, đánh dấu No-show

    -- 3. Phân hệ POS & Dịch vụ phát sinh (Điểm cộng)
    ('CREATE', 'SERVICE_ORDER', 'ACTIVE'), -- Gọi món, thêm phụ phí
    ('VIEW', 'SERVICE_ORDER', 'ACTIVE'),   -- Xem danh sách order dịch vụ

    -- 4. Phân hệ Quản lý Tài nguyên (Inventory - Nhánh 1 & 2)
    ('VIEW', 'INVENTORY', 'ACTIVE'),       -- Xem danh sách phòng, tiện ích
    ('CREATE', 'INVENTORY', 'ACTIVE'),     -- Tạo mới loại phòng, phòng vật lý
    ('UPDATE', 'INVENTORY', 'ACTIVE'),     -- Sửa trạng thái phòng (Bảo trì, dọn dẹp)
    ('DELETE', 'INVENTORY', 'ACTIVE'),   -- Xóa/Đóng phòng

    -- 5. Phân hệ Tính Giá Động (Pricing Engine - Nhánh 1)
    ('VIEW', 'PRICING', 'ACTIVE'),         -- Xem cấu hình giá, ngày lễ
    ('UPDATE', 'PRICING', 'ACTIVE'),       -- Thay đổi rule tăng/giảm giá
    ('CREATE', 'PRICING', 'ACTIVE'),     -- Thêm mới luật giá
    ('DELETE', 'PRICING', 'ACTIVE'),     -- Tạm ngưng/Xóa luật giá

    -- 6. Phân hệ Quản lý Nhân sự (IAM)
    ('VIEW', 'STAFF', 'ACTIVE'),           -- Xem danh sách nhân viên
    ('CREATE', 'STAFF', 'ACTIVE'),         -- Tạo tài khoản nhân viên mới
    ('UPDATE', 'STAFF', 'ACTIVE'),          -- Phân quyền, đổi mật khẩu

    -- Thiếu toàn bộ cụm CRM (Quản lý Hồ sơ khách hàng)
    ('VIEW', 'GUEST', 'ACTIVE'),         -- Tra cứu khách cũ
    ('CREATE', 'GUEST', 'ACTIVE'),       -- Tạo hồ sơ khách mới
    ('UPDATE', 'GUEST', 'ACTIVE');        -- Sửa thông tin khách




-- ==============================================================================
-- LỚP 1: GLOBAL MASTER DATA
-- Bảng: room_types (Danh mục Hạng phòng chuẩn)
-- ==============================================================================

INSERT INTO room_types (code, name, status)
VALUES 
    ('STD', 'Standard Room (Phòng Tiêu chuẩn)', 'ACTIVE'),
    ('SUP', 'Superior Room (Phòng Cao cấp)', 'ACTIVE'),
    ('DLX', 'Deluxe Room (Phòng Sang trọng)', 'ACTIVE'),
    ('STE', 'Suite (Phòng Căn hộ/Thương gia)', 'ACTIVE'),
    ('EXE', 'Executive Suite (Phòng Tổng thống/Đặc biệt)', 'ACTIVE'),
    ('FAM', 'Family Room (Phòng Gia đình)', 'ACTIVE');



-- ==============================================================================
-- LỚP 1: GLOBAL MASTER DATA
-- Bảng: room_beds (Danh mục Tiêu chuẩn Giường)
-- ==============================================================================

INSERT INTO room_beds (name, size, status)
VALUES 
    ('Single Bed (Giường đơn)', '1.2m x 2.0m', 'ACTIVE'),
    ('Double Bed (Giường đôi tiêu chuẩn)', '1.5m x 2.0m', 'ACTIVE'),
    ('Queen Size Bed (Giường đôi lớn)', '1.6m x 2.0m', 'ACTIVE'),
    ('King Size Bed (Giường cỡ đại)', '1.8m x 2.0m', 'ACTIVE'),
    ('Super King Size Bed (Giường siêu lớn)', '2.0m x 2.2m', 'ACTIVE'),
    ('Extra Bed (Giường phụ kê thêm)', '1.0m x 2.0m', 'ACTIVE');



-- ==============================================================================
-- LỚP 1: GLOBAL MASTER DATA
-- Bảng: room_features (Danh mục Tiện ích/Cơ sở vật chất gắn liền với phòng)
-- ==============================================================================

INSERT INTO room_features (code, name, category, status)
VALUES 
    -- Nhóm VIEW (Tầm nhìn)
    ('SEA_VIEW', 'Hướng biển (Sea View)', 'VIEW', 'ACTIVE'),
    ('CITY_VIEW', 'Hướng thành phố (City View)', 'VIEW', 'ACTIVE'),
    ('GARDEN_VIEW', 'Hướng sân vườn (Garden View)', 'VIEW', 'ACTIVE'),

    -- Nhóm BATHROOM (Phòng tắm)
    ('BATHTUB', 'Bồn tắm nằm', 'BATHROOM', 'ACTIVE'),
    ('SHOWER', 'Vòi hoa sen đứng', 'BATHROOM', 'ACTIVE'),

    -- Nhóm BEDROOM & COMFORT (Phòng ngủ & Sự thoải mái)
    ('BALCONY', 'Ban công riêng', 'BEDROOM', 'ACTIVE'),
    ('AIR_CONDITIONING', 'Điều hòa nhiệt độ', 'COMFORT', 'ACTIVE'),
    ('SOUNDPROOF', 'Phòng cách âm', 'COMFORT', 'ACTIVE'),

    -- Nhóm AMENITY (Tiện nghi vật lý có sẵn)
    ('SAFE_BOX', 'Két sắt an toàn', 'AMENITY', 'ACTIVE'),
    ('HAIR_DRYER', 'Máy sấy tóc', 'AMENITY', 'ACTIVE'),
    ('IRONING', 'Bàn ủi & Cầu là', 'AMENITY', 'ACTIVE'),

    -- Nhóm MEDIA, ENTERTAINMENT & INTERNET (Giải trí & Kết nối)
    ('FREE_WIFI', 'Wi-Fi tốc độ cao', 'INTERNET', 'ACTIVE'),
    ('SMART_TV', 'Smart TV 55 inch', 'MEDIA', 'ACTIVE'),
    ('NETFLIX', 'Tích hợp tài khoản Netflix', 'ENTERTAINMENT', 'ACTIVE');


INSERT INTO tax_categories (
    category_code,
    category_name,
    description
)
VALUES
(
    'ROOM',
    'Room Rental',
    'Accommodation and room rental services'
),
(
    'FOOD',
    'Food Service',
    'Restaurant, buffet and food services'
),
(
    'NON_ALCOHOLIC_DRINK',
    'Non Alcoholic Drink',
    'Mineral water, tea, coffee, juice and non-alcoholic beverages'
),
(
    'SUGARY_DRINK',
    'Sugary Drink',
    'Sugar-sweetened beverages requiring separate tax handling'
),
(
    'ALCOHOLIC_DRINK',
    'Alcoholic Drink',
    'Beer, wine and alcoholic beverages'
),
(
    'RENTAL',
    'Rental Service',
    'Motorbike, bicycle and other rental services'
),
(
    'TRANSPORT',
    'Transport Service',
    'Airport transfer and passenger transportation services'
),
(
    'LAUNDRY',
    'Laundry Service',
    'Laundry and ironing services'
),
(
    'SPA',
    'Spa Service',
    'Spa, massage and wellness services'
),
(
    'OTHER',
    'Other Service',
    'Other hotel services'
);



INSERT INTO vat_rules (
    tax_category_id,
    vat_code,
    vat_name,
    vat_percent,
    start_date,
    end_date
)
SELECT
    tc.id,
    v.vat_code,
    v.vat_name,
    v.vat_percent,
    v.start_date,
    v.end_date
FROM (
    VALUES
        (
            'ROOM',
            'ROOM_VAT_8_2025',
            'VAT 8% for accommodation services',
            8.00,
            DATE '2025-07-01',
            DATE '2026-12-31'
        ),
        (
            'FOOD',
            'FOOD_VAT_8_2025',
            'VAT 8% for food services',
            8.00,
            DATE '2025-07-01',
            DATE '2026-12-31'
        ),
        (
            'NON_ALCOHOLIC_DRINK',
            'NON_ALCOHOLIC_DRINK_VAT_8_2025',
            'VAT 8% for non alcoholic drinks',
            8.00,
            DATE '2025-07-01',
            DATE '2026-12-31'
        ),
        (
            'SUGARY_DRINK',
            'SUGARY_DRINK_VAT_10_2025',
            'VAT 10% for sugary drinks',
            10.00,
            DATE '2025-07-01',
            DATE '2026-12-31'
        ),
        (
            'ALCOHOLIC_DRINK',
            'ALCOHOLIC_DRINK_VAT_10',
            'VAT 10% for alcoholic drinks',
            10.00,
            DATE '2025-01-01',
            NULL
        ),
        (
            'RENTAL',
            'RENTAL_VAT_8_2025',
            'VAT 8% for rental services',
            8.00,
            DATE '2025-07-01',
            DATE '2026-12-31'
        ),
        (
            'TRANSPORT',
            'TRANSPORT_VAT_8_2025',
            'VAT 8% for transportation services',
            8.00,
            DATE '2025-07-01',
            DATE '2026-12-31'
        ),
        (
            'LAUNDRY',
            'LAUNDRY_VAT_8_2025',
            'VAT 8% for laundry services',
            8.00,
            DATE '2025-07-01',
            DATE '2026-12-31'
        ),
        (
            'SPA',
            'SPA_VAT_8_2025',
            'VAT 8% for spa services',
            8.00,
            DATE '2025-07-01',
            DATE '2026-12-31'
        ),
        (
            'OTHER',
            'OTHER_VAT_8_2025',
            'Default VAT for other services',
            8.00,
            DATE '2025-07-01',
            DATE '2026-12-31'
        )
) AS v(
    category_code,
    vat_code,
    vat_name,
    vat_percent,
    start_date,
    end_date
)
JOIN tax_categories tc
    ON tc.category_code = v.category_code;




INSERT INTO pricing_rule_types (code, display_name, priority, status)
VALUES 
    ('HOLIDAY', 'Phụ thu ngày Lễ/Tết (Ưu tiên cao nhất)', 100, 'ACTIVE'),
    ('PEAK_SEASON', 'Phụ thu mùa cao điểm (Du lịch/Hè)', 80, 'ACTIVE'),
    ('WEEKEND', 'Phụ thu cuối tuần (Thứ 6, Thứ 7)', 50, 'ACTIVE');




INSERT INTO discount_rule_types (code, display_name, priority, status)
VALUES 
    ('LONG_STAY', 'Giảm giá lưu trú dài ngày (Từ 3 đêm trở lên)', 100, 'ACTIVE'),
    ('SPECIAL_CAMPAIGN', 'Giảm giá theo chiến dịch đặc biệt (Flash Sale)', 90, 'ACTIVE'),
    ('EARLY_BIRD', 'Giảm giá đặt phòng sớm (Trước 30 ngày)', 80, 'ACTIVE'),
    ('LAST_MINUTE', 'Giảm giá giờ chót (Xả phòng trống trong ngày)', 50, 'ACTIVE');


INSERT INTO holiday_calendars (name, date, description, status)
VALUES 
    -- Nửa cuối năm 2026
    ('Quốc khánh 2026', '2026-09-02', 'Lễ Quốc khánh Việt Nam', 'ACTIVE'),
    ('Giáng Sinh 2026', '2026-12-24', 'Lễ Giáng Sinh (Đẩy giá mùa du lịch cuối năm)', 'ACTIVE'),

    -- Nửa đầu năm 2027
    ('Tết Dương lịch 2027', '2027-01-01', 'Nghỉ Tết Dương lịch', 'ACTIVE'),
    ('Tết Nguyên Đán (Mùng 1) 2027', '2027-02-06', 'Nghỉ Tết Âm lịch (Đinh Mùi)', 'ACTIVE'),
    ('Tết Nguyên Đán (Mùng 2) 2027', '2027-02-07', 'Nghỉ Tết Âm lịch (Đinh Mùi)', 'ACTIVE'),
    ('Tết Nguyên Đán (Mùng 3) 2027', '2027-02-08', 'Nghỉ Tết Âm lịch (Đinh Mùi)', 'ACTIVE'),
    ('Giỗ tổ Hùng Vương 2027', '2027-04-16', 'Nghỉ lễ Giỗ tổ (10/3 Âm lịch)', 'ACTIVE'),
    ('Giải phóng miền Nam 2027', '2027-04-30', 'Kỷ niệm Ngày Giải phóng miền Nam', 'ACTIVE'),
    ('Quốc tế Lao động 2027', '2027-05-01', 'Ngày Quốc tế Lao động', 'ACTIVE');