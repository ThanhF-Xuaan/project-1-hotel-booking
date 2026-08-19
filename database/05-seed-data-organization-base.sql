-- ==============================================================================
-- LỚP 2: ORGANIZATION BASE (Thực thể Doanh nghiệp)
-- Bảng: hotels (Phân 3 hạng: Luxury 5*, Grand 4*, Boutique 3*)
-- ==============================================================================

INSERT INTO hotels (name, address, phone, check_in_time, check_out_time, service_fee_percent, status)
VALUES 
    -- 1. Hạng 5 Sao (Chỉ bán phòng xịn: Deluxe, Suite, Executive)
    ('Viettel Luxury Hà Nội', 'Tòa nhà Viettel, Nam Từ Liêm, Hà Nội', '02466668888', '14:00:00', '12:00:00', 5.00, 'ACTIVE'),
    
    -- 2. Hạng 4 Sao (Bán phòng tầm trung: Superior, Deluxe)
    ('Viettel Grand Đà Nẵng', 'Số 2 Nguyễn Hữu Thọ, Đà Nẵng', '02366668888', '14:00:00', '12:00:00', 5.00, 'ACTIVE'),
    
    -- 3. Hạng 3 Sao (Chỉ bán phòng cơ bản: Standard, Superior, không có Suite)
    ('Viettel Boutique Sapa', 'Thị trấn Sapa, Lào Cai', '02146668888', '14:00:00', '12:00:00', 5.00, 'ACTIVE');



-- ==============================================================================
-- Bảng: role_permissions (Ma trận Phân quyền)
-- ==============================================================================

-- 1. SUPER_ADMIN: Vị thần của hệ thống -> Bơm toàn bộ quyền cho role này
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r CROSS JOIN permissions p 
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- 2. RECEPTIONIST (Lễ tân): Được thao tác Booking, Guest, Service Order, và xem thông tin
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'RECEPTIONIST'
  AND (
    (p.resource = 'DASHBOARD' AND p.action = 'VIEW') OR
    (p.resource = 'BOOKING') OR                          -- Full quyền với Đặt phòng
    (p.resource = 'GUEST') OR                            -- Full quyền với Khách hàng
    (p.resource = 'SERVICE_ORDER') OR                    -- Full quyền POS gọi đồ ăn
    (p.resource = 'INVENTORY' AND p.action = 'VIEW') OR  -- Chỉ được XEM phòng
    (p.resource = 'PRICING' AND p.action = 'VIEW')       -- Chỉ được XEM giá
  )
ON CONFLICT DO NOTHING;

-- 3. HOTEL_MANAGER (Quản lý chi nhánh): Như Lễ tân nhưng được quyền Sửa phòng và Xem Báo cáo
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'HOTEL_MANAGER'
  AND (
    (p.resource = 'DASHBOARD' AND p.action = 'VIEW') OR
    (p.resource = 'REPORTS' AND p.action = 'VIEW') OR
    (p.resource = 'STAFF' AND p.action IN ('VIEW', 'UPDATE')) OR -- Quản lý nhân viên của mình
    (p.resource = 'BOOKING') OR
    (p.resource = 'GUEST') OR
    (p.resource = 'SERVICE_ORDER') OR
    (p.resource = 'INVENTORY' AND p.action IN ('VIEW', 'UPDATE')) OR -- Được đổi trạng thái phòng
    (p.resource = 'PRICING' AND p.action = 'VIEW')
  )
ON CONFLICT DO NOTHING;

-- 4. HOUSEKEEPING (Buồng phòng): Nhiệm vụ duy nhất là Đổi trạng thái phòng (Dọn dẹp -> Sẵn sàng)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'HOUSEKEEPING'
  AND (
    (p.resource = 'DASHBOARD' AND p.action = 'VIEW') OR
    (p.resource = 'INVENTORY' AND p.action = 'UPDATE')
  )
ON CONFLICT DO NOTHING;

