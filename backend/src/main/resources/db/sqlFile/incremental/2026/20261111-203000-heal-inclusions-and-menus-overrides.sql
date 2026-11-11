-- Migration: Chuẩn hóa dữ liệu master_item_id trên menus và dọn rác trùng lặp trong hotel_room_type_inclusions

-- 1. Chuẩn hóa liên kết master_item_id trên bảng menus:
-- Đưa các món Buffet Sáng về đúng mã toàn chuỗi CHAIN_BUFFET_BREAKFAST
UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_BUFFET_BREAKFAST' LIMIT 1)
WHERE name ILIKE '%Buffet%' 
  AND (master_item_id IS NULL OR master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%'));

-- Đưa các món Xe đưa đón sân bay về đúng mã toàn chuỗi CHAIN_AIRPORT_SHUTTLE
UPDATE menus
SET master_item_id = (SELECT id FROM master_items WHERE code = 'CHAIN_AIRPORT_SHUTTLE' LIMIT 1)
WHERE name ILIKE '%s_n bay%' 
  AND (master_item_id IS NULL OR master_item_id IN (SELECT id FROM master_items WHERE code LIKE 'MASTER_%'));

-- 2. Dọn rác hotel_room_type_inclusions bị gán chéo menu của khách sạn khác (do bug CROSS JOIN cũ trong dev-seed)
DELETE FROM hotel_room_type_inclusions i
USING menus m, hotel_room_types hrt
WHERE i.menu_id = m.id 
  AND i.hotel_room_type_id = hrt.id 
  AND m.hotel_id <> hrt.hotel_id;

-- 3. Xóa các inclusion trùng lặp cùng một menu_id trên cùng một hạng phòng (chỉ giữ lại 1 bản ghi đầu tiên)
DELETE FROM hotel_room_type_inclusions i
WHERE i.id NOT IN (
    SELECT MIN(sub.id)
    FROM hotel_room_type_inclusions sub
    WHERE sub.is_deleted = false AND sub.menu_id IS NOT NULL
    GROUP BY sub.hotel_room_type_id, sub.menu_id
) AND i.menu_id IS NOT NULL;

-- 4. Xóa các inclusion trùng lặp cùng một master_item_id trên cùng một hạng phòng (chỉ giữ lại 1 bản ghi đầu tiên)
DELETE FROM hotel_room_type_inclusions i
WHERE i.id NOT IN (
    SELECT MIN(sub.id)
    FROM hotel_room_type_inclusions sub
    WHERE sub.is_deleted = false AND sub.master_item_id IS NOT NULL
    GROUP BY sub.hotel_room_type_id, sub.master_item_id
) AND i.master_item_id IS NOT NULL;

-- 5. Xóa inclusion dạng master_item_id nếu phòng đó ĐÃ CÓ menu inclusion tương ứng với master_item_id đó (ưu tiên bản ghi cơ sở)
DELETE FROM hotel_room_type_inclusions i
WHERE i.master_item_id IS NOT NULL
  AND EXISTS (
      SELECT 1 
      FROM hotel_room_type_inclusions existing_menu_inc
      JOIN menus m ON existing_menu_inc.menu_id = m.id
      WHERE existing_menu_inc.hotel_room_type_id = i.hotel_room_type_id
        AND existing_menu_inc.is_deleted = false
        AND m.master_item_id = i.master_item_id
  );
