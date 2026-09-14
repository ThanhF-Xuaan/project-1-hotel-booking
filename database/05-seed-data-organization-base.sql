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
-- LỚP 3: MA TRẬN PHÂN QUYỀN (Role Permissions Mapping)
-- Phản ánh đúng kiến trúc: CHAIN -> REGION -> PROPERTY -> EMPLOYEE
-- ==============================================================================

-- Viết khối DO để tự động map Quyền cho các Role (Tránh lỗi sai ID và Conflict)
DO $$
DECLARE
    v_chain_admin_id SMALLINT;
    v_region_manager_id SMALLINT;
    v_property_manager_id SMALLINT;
    v_reception_id SMALLINT;
    v_hk_id SMALLINT;
BEGIN
    -- Lấy ID của các vai trò đã được định nghĩa
    SELECT id INTO v_chain_admin_id FROM roles WHERE code = 'CHAIN_ADMIN';
    SELECT id INTO v_region_manager_id FROM roles WHERE code = 'REGION_MANAGER';
    SELECT id INTO v_property_manager_id FROM roles WHERE code = 'PROPERTY_MANAGER';
    SELECT id INTO v_reception_id FROM roles WHERE code = 'RECEPTIONIST';
    SELECT id INTO v_hk_id FROM roles WHERE code = 'HOUSEKEEPING';

    -- 1. Tầng CHAIN (Chain Admin): Có quyền thiết lập, tạo Property và Manager các cấp
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_chain_admin_id, id FROM permissions 
    WHERE resource IN ('CHAIN_DASHBOARD', 'PROPERTY', 'REGION_MANAGER_ACCOUNT', 'PROPERTY_MANAGER_ACCOUNT', 'PRICING', 'INVENTORY')
    ON CONFLICT DO NOTHING;

    -- 2. Tầng REGION (Region Manager): Xem báo cáo vùng và điều phối khách sạn trong Vùng
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_region_manager_id, id FROM permissions 
    WHERE resource IN ('REGION_DASHBOARD', 'PROPERTY_DASHBOARD', 'PROPERTY_MANAGER_ACCOUNT', 'PRICING', 'INVENTORY')
    ON CONFLICT DO NOTHING;

    -- 3. Tầng PROPERTY (Property Manager): Quản trị 1 Khách sạn cụ thể
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_property_manager_id, id FROM permissions 
    WHERE resource IN ('PROPERTY_DASHBOARD', 'STAFF', 'BOOKING', 'INVENTORY', 'GUEST', 'SERVICE_ORDER')
    ON CONFLICT DO NOTHING;

    -- 4. Tầng EMPLOYEE - Lễ tân (Receptionist): Đặt phòng, Giao dịch
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_reception_id, id FROM permissions 
    WHERE resource IN ('BOOKING', 'GUEST', 'SERVICE_ORDER') 
       OR (action = 'UPDATE' AND resource = 'ROOM_STATUS')
    ON CONFLICT DO NOTHING;

    -- 5. Tầng EMPLOYEE - Buồng phòng (Housekeeping): Chỉ làm nhiệm vụ Dọn phòng & Báo hỏng
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_hk_id, id FROM permissions 
    WHERE resource IN ('ROOM_STATUS', 'MAINTENANCE_TICKET')
    ON CONFLICT DO NOTHING;
END $$;