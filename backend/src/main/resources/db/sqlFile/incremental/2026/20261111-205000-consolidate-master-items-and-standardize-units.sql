-- Migration: Hợp nhất toàn bộ Master Items nhân bản lịch sử và Chuẩn hóa Đơn Vị Tính toàn chuỗi

-- 1. Chuẩn hóa các Master Items gốc thành danh mục chuỗi chuẩn hóa (Canonical Chain Standards)
UPDATE master_items SET unit = 'CHAI' WHERE code = 'CHAIN_MINERAL_WATER';
UPDATE master_items SET unit = 'LON' WHERE code = 'CHAIN_COCA_COLA';
UPDATE master_items SET unit = 'LON' WHERE code = 'CHAIN_HEINEKEN';
UPDATE master_items SET unit = 'CHAI' WHERE code = 'CHAIN_DALAT_WINE';
UPDATE master_items SET unit = 'KG' WHERE code = 'CHAIN_LAUNDRY';
UPDATE master_items SET unit = 'NGAY' WHERE code = 'CHAIN_MOTORBIKE_RENTAL';
UPDATE master_items SET unit = 'LUOT' WHERE code = 'CHAIN_BODY_MASSAGE';
UPDATE master_items SET unit = 'GOI' WHERE code = 'CHAIN_HONEYMOON_DECOR';
UPDATE master_items SET unit = 'PHAN' WHERE code = 'CHAIN_BUFFET_BREAKFAST';
UPDATE master_items SET unit = 'CHUYEN' WHERE code = 'CHAIN_AIRPORT_SHUTTLE';

-- 2. Cập nhật lại toàn bộ master_item_id của bảng menus trên mọi khách sạn về các Master Items chuẩn
UPDATE menus SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_BUFFET_BREAKFAST' LIMIT 1) WHERE name ILIKE '%Buffet%';
UPDATE menus SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_MINERAL_WATER' LIMIT 1) WHERE name ILIKE '%Lavie%';
UPDATE menus SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_COCA_COLA' LIMIT 1) WHERE name ILIKE '%Coca%';
UPDATE menus SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_HEINEKEN' LIMIT 1) WHERE name ILIKE '%Heineken%';
UPDATE menus SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_DALAT_WINE' LIMIT 1) WHERE name ILIKE '%Vang%';
UPDATE menus SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_LAUNDRY' LIMIT 1) WHERE name ILIKE '%Gi_t l_%';
UPDATE menus SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_AIRPORT_SHUTTLE' LIMIT 1) WHERE name ILIKE '%s_n bay%';
UPDATE menus SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_MOTORBIKE_RENTAL' LIMIT 1) WHERE name ILIKE '%xe m_y%';
UPDATE menus SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_BODY_MASSAGE' LIMIT 1) WHERE name ILIKE '%Massage%';
UPDATE menus SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_HONEYMOON_DECOR' LIMIT 1) WHERE name ILIKE '%tr_ng m_t%';

-- 3. Cập nhật các bản ghi trong hotel_room_type_inclusions nếu đang trỏ tới các master_item_id nhân bản cũ
UPDATE hotel_room_type_inclusions SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_BUFFET_BREAKFAST' LIMIT 1)
WHERE master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%' AND name ILIKE '%Buffet%');

UPDATE hotel_room_type_inclusions SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_MINERAL_WATER' LIMIT 1)
WHERE master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%' AND name ILIKE '%Lavie%');

UPDATE hotel_room_type_inclusions SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_COCA_COLA' LIMIT 1)
WHERE master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%' AND name ILIKE '%Coca%');

UPDATE hotel_room_type_inclusions SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_HEINEKEN' LIMIT 1)
WHERE master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%' AND name ILIKE '%Heineken%');

UPDATE hotel_room_type_inclusions SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_DALAT_WINE' LIMIT 1)
WHERE master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%' AND name ILIKE '%Vang%');

UPDATE hotel_room_type_inclusions SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_LAUNDRY' LIMIT 1)
WHERE master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%' AND name ILIKE '%Gi_t l_%');

UPDATE hotel_room_type_inclusions SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_AIRPORT_SHUTTLE' LIMIT 1)
WHERE master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%' AND name ILIKE '%s_n bay%');

UPDATE hotel_room_type_inclusions SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_MOTORBIKE_RENTAL' LIMIT 1)
WHERE master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%' AND name ILIKE '%xe m_y%');

UPDATE hotel_room_type_inclusions SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_BODY_MASSAGE' LIMIT 1)
WHERE master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%' AND name ILIKE '%Massage%');

UPDATE hotel_room_type_inclusions SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_HONEYMOON_DECOR' LIMIT 1)
WHERE master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%' AND name ILIKE '%tr_ng m_t%');

-- 4. Xóa các inclusion dạng master_item_id nếu khách sạn sở hữu hạng phòng đó ĐÃ CÓ món menu tương ứng trong thực đơn
DELETE FROM hotel_room_type_inclusions i
WHERE i.master_item_id IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM hotel_room_types hrt
      JOIN menus m ON m.hotel_id = hrt.hotel_id
      WHERE hrt.id = i.hotel_room_type_id
        AND m.master_item_id = i.master_item_id
        AND m.is_deleted = false
  );

-- 5. Xóa bỏ an toàn các bản ghi Master Items nhân bản rác (MASTER_*)
DELETE FROM master_items 
WHERE code LIKE 'MASTER_%';
