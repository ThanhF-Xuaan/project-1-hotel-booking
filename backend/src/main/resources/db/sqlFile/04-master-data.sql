-- ==============================================================================
-- 2. DỮ LIỆU MẪU (SEED DATA) ĐỂ TEST API
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 2.1 SEED QUYỀN HẠN (PERMISSIONS)
-- ------------------------------------------------------------------------------
INSERT INTO permissions (action, resource, status)
VALUES 
    -- 1. Tầng CHAIN (Chuỗi)
    ('VIEW', 'CHAIN_DASHBOARD', 'ACTIVE'),    -- Xem báo cáo tổng lực toàn chuỗi
    ('CREATE', 'PROPERTY', 'ACTIVE'),         -- Mở khách sạn mới
    ('CREATE', 'REGION_MANAGER_ACCOUNT', 'ACTIVE'), -- (MỚI) Tạo tài khoản GĐ Vùng
    ('CREATE', 'PROPERTY_MANAGER_ACCOUNT', 'ACTIVE'), -- Tạo tài khoản GĐ Chi nhánh
    
    -- 2. Tầng REGION (Vùng)
    ('VIEW', 'REGION_DASHBOARD', 'ACTIVE'),   -- (MỚI) Xem báo cáo các KS thuộc Vùng
    
    -- 3. Tầng PROPERTY (Khách sạn Cơ sở)
    ('VIEW', 'PROPERTY_DASHBOARD', 'ACTIVE'), -- Xem doanh thu, lấp đầy của riêng 1 khách sạn
    ('VIEW', 'STAFF', 'ACTIVE'),              -- Xem nhân sự trong KS
    ('CREATE', 'STAFF', 'ACTIVE'),            -- Tuyển thêm nhân viên (Lễ tân, Buồng phòng)
    ('UPDATE', 'STAFF', 'ACTIVE'),
    
    -- 4. Tầng EMPLOYEE / DEPARTMENT (Lễ Tân / Sales / HK / F&B)
    ('VIEW', 'BOOKING', 'ACTIVE'),         
    ('CREATE', 'BOOKING', 'ACTIVE'),       
    ('UPDATE', 'BOOKING', 'ACTIVE'),       
    ('CANCEL', 'BOOKING', 'ACTIVE'),       
    
    ('VIEW', 'SERVICE_ORDER', 'ACTIVE'),   
    ('CREATE', 'SERVICE_ORDER', 'ACTIVE'), 
    
    ('UPDATE', 'ROOM_STATUS', 'ACTIVE'),      -- Dọn phòng: DIRTY -> CLEANING -> READY
    ('CREATE', 'MAINTENANCE_TICKET', 'ACTIVE'),-- Báo hỏng thiết bị
    
    ('VIEW', 'INVENTORY', 'ACTIVE'),       
    ('UPDATE', 'INVENTORY', 'ACTIVE'),     
    ('VIEW', 'PRICING', 'ACTIVE'),         
    ('UPDATE', 'PRICING', 'ACTIVE'),       
    
    ('VIEW', 'GUEST', 'ACTIVE'),         
    ('CREATE', 'GUEST', 'ACTIVE'),       
    ('UPDATE', 'GUEST', 'ACTIVE');       


-- ------------------------------------------------------------------------------
-- 2.2 SEED VAI TRÒ (ROLES) VÀ GÁN QUYỀN (ROLE_PERMISSIONS)
-- ------------------------------------------------------------------------------
INSERT INTO roles (code, name, status)
VALUES 
    ('CHAIN_ADMIN', 'Quản trị viên Toàn Chuỗi', 'ACTIVE'),
    ('REGION_MANAGER', 'Giám đốc Vùng', 'ACTIVE'), -- (MỚI THÊM)
    ('PROPERTY_MANAGER', 'Tổng Quản lý Khách sạn', 'ACTIVE'),
    ('RECEPTIONIST', 'Lễ tân Tiền sảnh', 'ACTIVE'),
    ('HOUSEKEEPING', 'Nhân viên Buồng phòng', 'ACTIVE');

-- Viết khối DO để tự động map Quyền cho các Role một cách chính xác
DO $$
DECLARE
    v_chain_admin_id SMALLINT;
    v_region_manager_id SMALLINT;
    v_property_manager_id SMALLINT;
    v_reception_id SMALLINT;
    v_hk_id SMALLINT;
BEGIN
    SELECT id INTO v_chain_admin_id FROM roles WHERE code = 'CHAIN_ADMIN';
    SELECT id INTO v_region_manager_id FROM roles WHERE code = 'REGION_MANAGER';
    SELECT id INTO v_property_manager_id FROM roles WHERE code = 'PROPERTY_MANAGER';
    SELECT id INTO v_reception_id FROM roles WHERE code = 'RECEPTIONIST';
    SELECT id INTO v_hk_id FROM roles WHERE code = 'HOUSEKEEPING';

    -- 1. Chain Admin: Ôm toàn bộ quyền Quản trị hệ thống, Báo cáo chuỗi
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_chain_admin_id, id FROM permissions 
    WHERE resource IN ('CHAIN_DASHBOARD', 'PROPERTY', 'REGION_MANAGER_ACCOUNT', 'PROPERTY_MANAGER_ACCOUNT', 'PRICING', 'INVENTORY')
    ON CONFLICT DO NOTHING;

    -- 2. Region Manager (MỚI): Xem báo cáo vùng, và điều phối các Property Manager
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_region_manager_id, id FROM permissions 
    WHERE resource IN ('REGION_DASHBOARD', 'PROPERTY_DASHBOARD', 'PROPERTY_MANAGER_ACCOUNT', 'PRICING', 'INVENTORY')
    ON CONFLICT DO NOTHING;

    -- 3. Property Manager: Ôm báo cáo cơ sở, quản lý nhân viên cấp dưới, inventory, booking
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_property_manager_id, id FROM permissions 
    WHERE resource IN ('PROPERTY_DASHBOARD', 'STAFF', 'BOOKING', 'INVENTORY', 'GUEST', 'SERVICE_ORDER')
    ON CONFLICT DO NOTHING;

    -- 4. Receptionist: Phụ trách Đặt phòng, Check-in, Dịch vụ, CRM
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_reception_id, id FROM permissions 
    WHERE resource IN ('BOOKING', 'GUEST', 'SERVICE_ORDER') OR (action = 'UPDATE' AND resource = 'ROOM_STATUS')
    ON CONFLICT DO NOTHING;

    -- 5. Housekeeping: Chỉ được đổi trạng thái phòng và báo hỏng
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_hk_id, id FROM permissions 
    WHERE resource IN ('ROOM_STATUS', 'MAINTENANCE_TICKET')
    ON CONFLICT DO NOTHING;
END $$;




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