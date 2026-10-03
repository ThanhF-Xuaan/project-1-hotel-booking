DO $$
DECLARE
    v_chain_admin_id SMALLINT;
    v_region_manager_id SMALLINT;
    v_property_manager_id SMALLINT;
    v_reception_id SMALLINT;
    v_hk_id SMALLINT;
BEGIN
    -- Láº¥y ID cá»§a cÃ¡c vai trÃ² Ä‘Ã£ Ä‘Æ°á»£c Ä‘á»‹nh nghÄ©a
    SELECT id INTO v_chain_admin_id FROM roles WHERE code = 'CHAIN_ADMIN';
    SELECT id INTO v_region_manager_id FROM roles WHERE code = 'REGION_MANAGER';
    SELECT id INTO v_property_manager_id FROM roles WHERE code = 'PROPERTY_MANAGER';
    SELECT id INTO v_reception_id FROM roles WHERE code = 'RECEPTIONIST';
    SELECT id INTO v_hk_id FROM roles WHERE code = 'HOUSEKEEPING';

    -- 1. Táº§ng CHAIN (Chain Admin): CÃ³ quyá»n thiáº¿t láº­p, táº¡o Property vÃ  Manager cÃ¡c cáº¥p
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_chain_admin_id, id FROM permissions 
    WHERE resource IN ('CHAIN_DASHBOARD', 'PROPERTY', 'REGION_MANAGER_ACCOUNT', 'PROPERTY_MANAGER_ACCOUNT', 'PRICING', 'INVENTORY')
    ON CONFLICT DO NOTHING;

    -- 2. Táº§ng REGION (Region Manager): Xem bÃ¡o cÃ¡o vÃ¹ng vÃ  Ä‘iá»u phá»‘i khÃ¡ch sáº¡n trong VÃ¹ng
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_region_manager_id, id FROM permissions 
    WHERE resource IN ('REGION_DASHBOARD', 'PROPERTY_DASHBOARD', 'PROPERTY_MANAGER_ACCOUNT', 'PRICING', 'INVENTORY')
    ON CONFLICT DO NOTHING;

    -- 3. Táº§ng PROPERTY (Property Manager): Quáº£n trá»‹ 1 KhÃ¡ch sáº¡n cá»¥ thá»ƒ
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_property_manager_id, id FROM permissions 
    WHERE resource IN ('PROPERTY_DASHBOARD', 'STAFF', 'BOOKING', 'INVENTORY', 'GUEST', 'SERVICE_ORDER')
    ON CONFLICT DO NOTHING;

    -- 4. Táº§ng EMPLOYEE - Lá»… tÃ¢n (Receptionist): Äáº·t phÃ²ng, Giao dá»‹ch
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_reception_id, id FROM permissions 
    WHERE resource IN ('BOOKING', 'GUEST', 'SERVICE_ORDER') 
       OR (action = 'UPDATE' AND resource = 'ROOM_STATUS')
    ON CONFLICT DO NOTHING;

    -- 5. Táº§ng EMPLOYEE - Buá»“ng phÃ²ng (Housekeeping): Chá»‰ lÃ m nhiá»‡m vá»¥ Dá»n phÃ²ng & BÃ¡o há»ng
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT v_hk_id, id FROM permissions 
    WHERE resource IN ('ROOM_STATUS', 'MAINTENANCE_TICKET')
    ON CONFLICT DO NOTHING;
END $$;
