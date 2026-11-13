-- ==============================================================================
-- BỔ SUNG CÁC VÙNG / THÀNH PHỐ DU LỊCH TRỌNG ĐIỂM VÀ LIÊN KẾT KHÁCH SẠN
-- Bảng: regions, hotels
-- ==============================================================================

-- 1. Bổ sung các Thành phố / Điểm đến du lịch vào bảng regions
-- (Sử dụng WHERE NOT EXISTS để tương thích an toàn với Partial Unique Index uq_regions_code_active)
INSERT INTO regions (code, name, description, status)
SELECT v.code, v.name, v.description, v.status
FROM (VALUES 
    ('HANOI', 'Hà Nội', 'Thủ đô Hà Nội và vùng phụ cận', 'ACTIVE'),
    ('DANANG', 'Đà Nẵng', 'Thành phố Đà Nẵng & miền duyên hải', 'ACTIVE'),
    ('SAPA', 'Sa Pa', 'Thị xã Sa Pa - Tỉnh Lào Cai', 'ACTIVE'),
    ('HCM', 'TP. Hồ Chí Minh', 'Thành phố Hồ Chí Minh', 'ACTIVE')
) AS v(code, name, description, status)
WHERE NOT EXISTS (
    SELECT 1 FROM regions r WHERE r.code = v.code AND r.is_deleted = FALSE
);

-- 2. Cập nhật khách sạn tại Hà Nội
UPDATE hotels 
SET region_id = (SELECT id FROM regions WHERE code = 'HANOI' AND is_deleted = FALSE LIMIT 1) 
WHERE (name ILIKE '%Hà Nội%' OR address ILIKE '%Hà Nội%') 
  AND EXISTS (SELECT 1 FROM regions WHERE code = 'HANOI' AND is_deleted = FALSE);

-- 3. Cập nhật khách sạn tại Đà Nẵng
UPDATE hotels 
SET region_id = (SELECT id FROM regions WHERE code = 'DANANG' AND is_deleted = FALSE LIMIT 1) 
WHERE (name ILIKE '%Đà Nẵng%' OR address ILIKE '%Đà Nẵng%') 
  AND EXISTS (SELECT 1 FROM regions WHERE code = 'DANANG' AND is_deleted = FALSE);

-- 4. Cập nhật khách sạn tại Sa Pa
UPDATE hotels 
SET region_id = (SELECT id FROM regions WHERE code = 'SAPA' AND is_deleted = FALSE LIMIT 1) 
WHERE (name ILIKE '%Sapa%' OR address ILIKE '%Sapa%' OR address ILIKE '%Lào Cai%') 
  AND EXISTS (SELECT 1 FROM regions WHERE code = 'SAPA' AND is_deleted = FALSE);

