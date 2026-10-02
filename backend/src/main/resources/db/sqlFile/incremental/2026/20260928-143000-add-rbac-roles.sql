-- ==============================================================================
-- INCREMENTAL MIGRATION: BỔ SUNG ROLES & PERMISSIONS THEO SRS MỤC 1.2.1.1
-- ==============================================================================

-- 1. Thêm các Quyền Hạn (Permissions) mới nếu chưa tồn tại
INSERT INTO permissions (action, resource, status)
VALUES 
    ('VIEW', 'PAYMENT', 'ACTIVE'),
    ('CREATE', 'PAYMENT', 'ACTIVE'),
    ('REFUND', 'PAYMENT', 'ACTIVE'),
    ('VIEW', 'INVOICE', 'ACTIVE'),
    ('CREATE', 'INVOICE', 'ACTIVE'),
    ('EXECUTE', 'NIGHT_AUDIT', 'ACTIVE'),
    ('VIEW', 'NIGHT_AUDIT', 'ACTIVE'),
    ('VIEW', 'COMPANY', 'ACTIVE'),
    ('CREATE', 'COMPANY', 'ACTIVE'),
    ('UPDATE', 'COMPANY', 'ACTIVE'),
    ('VIEW', 'MENU', 'ACTIVE'),
    ('CREATE', 'MENU', 'ACTIVE'),
    ('UPDATE', 'MENU', 'ACTIVE'),
    ('UPDATE', 'SERVICE_ORDER', 'ACTIVE'),
    ('VIEW', 'ROOM_INSTANCE', 'ACTIVE'),
    ('VIEW', 'ROLE', 'ACTIVE')
ON CONFLICT (action, resource) DO NOTHING;

-- 2. Thêm 5 Vai Trò (Roles) mới vào bảng roles
INSERT INTO roles (code, name, status)
VALUES 
    ('CHAIN_EXECUTIVE', 'Ban Điều hành Chuỗi', 'ACTIVE'),
    ('SALES_GROUP', 'Kinh doanh Đoàn & Doanh nghiệp', 'ACTIVE'),
    ('F_AND_B', 'Bộ phận Ẩm thực & Dịch vụ (F&B)', 'ACTIVE'),
    ('ENGINEERING', 'Kỹ thuật & Bảo trì', 'ACTIVE'),
    ('FINANCE', 'Tài chính & Kế toán', 'ACTIVE')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name, status = EXCLUDED.status, is_deleted = false;

-- 3. Xóa role CUSTOMER nếu tồn tại (vì khách hàng chỉ dùng public API)
DELETE FROM roles WHERE code = 'CUSTOMER';

-- 4. Phân Quyền Chi Tiết Cho Các Roles Mới và Cập Nhật Roles Hiện Hữu
DO $$
DECLARE
    v_chain_exec_id SMALLINT;
    v_sales_id SMALLINT;
    v_fnb_id SMALLINT;
    v_eng_id SMALLINT;
    v_finance_id SMALLINT;
    v_chain_admin_id SMALLINT;
    v_region_mgr_id SMALLINT;
    v_property_mgr_id SMALLINT;
    v_reception_id SMALLINT;
BEGIN
    SELECT id INTO v_chain_exec_id FROM roles WHERE code = 'CHAIN_EXECUTIVE';
    SELECT id INTO v_sales_id FROM roles WHERE code = 'SALES_GROUP';
    SELECT id INTO v_fnb_id FROM roles WHERE code = 'F_AND_B';
    SELECT id INTO v_eng_id FROM roles WHERE code = 'ENGINEERING';
    SELECT id INTO v_finance_id FROM roles WHERE code = 'FINANCE';
    SELECT id INTO v_chain_admin_id FROM roles WHERE code = 'CHAIN_ADMIN';
    SELECT id INTO v_region_mgr_id FROM roles WHERE code = 'REGION_MANAGER';
    SELECT id INTO v_property_mgr_id FROM roles WHERE code = 'PROPERTY_MANAGER';
    SELECT id INTO v_reception_id FROM roles WHERE code = 'RECEPTIONIST';

    -- 4.1 CHAIN_EXECUTIVE (Chỉ xem báo cáo, doanh thu, dashboards toàn chuỗi và các phân hệ)
    IF v_chain_exec_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_chain_exec_id, id FROM permissions 
        WHERE action = 'VIEW' AND resource IN (
            'CHAIN_DASHBOARD', 'REGION_DASHBOARD', 'PROPERTY_DASHBOARD', 
            'PROPERTY', 'STAFF', 'BOOKING', 'INVENTORY', 'PRICING', 
            'PAYMENT', 'INVOICE', 'NIGHT_AUDIT', 'SERVICE_ORDER', 'ROOM_INSTANCE'
        )
        ON CONFLICT DO NOTHING;
    END IF;

    -- 4.2 SALES_GROUP (Quản lý khách đoàn, đặt phòng, công ty, xem inventory, pricing)
    IF v_sales_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_sales_id, id FROM permissions 
        WHERE (resource IN ('PROPERTY', 'INVENTORY', 'PRICING') AND action = 'VIEW')
           OR (resource IN ('GUEST', 'COMPANY', 'BOOKING') AND action IN ('VIEW', 'CREATE', 'UPDATE'))
        ON CONFLICT DO NOTHING;
    END IF;

    -- 4.3 F_AND_B (Quản lý menu, tạo đơn dịch vụ, thêm phụ phí vào booking)
    IF v_fnb_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_fnb_id, id FROM permissions 
        WHERE resource IN ('MENU', 'SERVICE_ORDER')
           OR (resource = 'BOOKING' AND action IN ('VIEW', 'UPDATE'))
        ON CONFLICT DO NOTHING;
    END IF;

    -- 4.4 ENGINEERING (Xem phòng, bảo trì, báo hỏng thiết bị)
    IF v_eng_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_eng_id, id FROM permissions 
        WHERE resource IN ('ROOM_INSTANCE', 'MAINTENANCE_TICKET')
           OR (resource = 'ROOM_STATUS' AND action IN ('VIEW', 'UPDATE'))
        ON CONFLICT DO NOTHING;
    END IF;

    -- 4.5 FINANCE (Quản lý thanh toán, xuất hóa đơn, chạy Night Audit, xem báo cáo tài chính)
    IF v_finance_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_finance_id, id FROM permissions 
        WHERE resource IN ('PAYMENT', 'INVOICE', 'NIGHT_AUDIT')
           OR (resource IN ('BOOKING', 'SERVICE_ORDER', 'PROPERTY_DASHBOARD', 'PRICING') AND action = 'VIEW')
        ON CONFLICT DO NOTHING;
    END IF;

    -- 4.6 Bổ sung quyền cho CHAIN_ADMIN
    IF v_chain_admin_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_chain_admin_id, id FROM permissions
        ON CONFLICT DO NOTHING;
    END IF;

    -- 4.7 Bổ sung quyền cho REGION_MANAGER
    IF v_region_mgr_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_region_mgr_id, id FROM permissions
        WHERE resource IN ('REGION_DASHBOARD', 'PROPERTY_DASHBOARD', 'PROPERTY', 'STAFF', 'BOOKING', 'INVENTORY', 'PRICING', 'GUEST', 'COMPANY', 'PAYMENT', 'INVOICE', 'NIGHT_AUDIT', 'ROLE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- 4.8 Bổ sung quyền cho PROPERTY_MANAGER
    IF v_property_mgr_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_property_mgr_id, id FROM permissions
        WHERE resource IN ('PROPERTY_DASHBOARD', 'STAFF', 'BOOKING', 'INVENTORY', 'GUEST', 'COMPANY', 'SERVICE_ORDER', 'PAYMENT', 'INVOICE', 'MENU', 'NIGHT_AUDIT', 'ROOM_STATUS', 'MAINTENANCE_TICKET', 'PRICING', 'ROOM_INSTANCE')
        ON CONFLICT DO NOTHING;
    END IF;

    -- 4.9 Bổ sung quyền cho RECEPTIONIST
    IF v_reception_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT v_reception_id, id FROM permissions
        WHERE resource IN ('BOOKING', 'GUEST', 'COMPANY', 'SERVICE_ORDER', 'PAYMENT', 'INVOICE', 'MENU', 'PROPERTY_DASHBOARD', 'INVENTORY', 'PRICING')
           OR (resource = 'ROOM_STATUS' AND action = 'UPDATE')
        ON CONFLICT DO NOTHING;
    END IF;

END $$;
