-- Migration: Hợp nhất toàn bộ Master Items nhân bản lịch sử và Chuẩn hóa Đơn Vị Tính toàn chuỗi

-- 1. Chuẩn hóa các Master Items gốc thành danh mục chuỗi chuẩn hóa (Canonical Chain Standards)
UPDATE master_items SET code = 'CHAIN_MINERAL_WATER', name = 'Nước Suối Lavie 500ml', unit = 'CHAI' WHERE id = 2;
UPDATE master_items SET code = 'CHAIN_COCA_COLA', name = 'Nước ngọt Coca Cola', unit = 'LON' WHERE id = 3;
UPDATE master_items SET code = 'CHAIN_HEINEKEN', name = 'Bia Heineken', unit = 'LON' WHERE id = 4;
UPDATE master_items SET code = 'CHAIN_DALAT_WINE', name = 'Rượu Vang Đỏ Đà Lạt', unit = 'CHAI' WHERE id = 5;
UPDATE master_items SET code = 'CHAIN_LAUNDRY', name = 'Dịch vụ Giặt là (kg)', unit = 'KG' WHERE id = 6;
UPDATE master_items SET code = 'CHAIN_MOTORBIKE_RENTAL', name = 'Thuê xe máy tay ga (Ngày)', unit = 'NGAY' WHERE id = 8;
UPDATE master_items SET code = 'CHAIN_BODY_MASSAGE', name = 'Massage Trị Liệu Body (60p)', unit = 'LUOT' WHERE id = 9;
UPDATE master_items SET code = 'CHAIN_HONEYMOON_DECOR', name = 'Trang trí phòng trăng mật', unit = 'GOI' WHERE id = 10;
UPDATE master_items SET code = 'CHAIN_BUFFET_BREAKFAST', unit = 'PHAN' WHERE id = 36;
UPDATE master_items SET code = 'CHAIN_AIRPORT_SHUTTLE', unit = 'CHUYEN' WHERE id = 37;

-- 2. Cập nhật lại toàn bộ master_item_id của bảng menus trên mọi khách sạn về các Master Items chuẩn
UPDATE menus SET master_item_id = 36 WHERE master_item_id IN (1, 11, 21) OR name ILIKE '%Buffet%';
UPDATE menus SET master_item_id = 2 WHERE master_item_id IN (2, 12, 22) OR name ILIKE '%Lavie%';
UPDATE menus SET master_item_id = 3 WHERE master_item_id IN (3, 13, 23) OR name ILIKE '%Coca%';
UPDATE menus SET master_item_id = 4 WHERE master_item_id IN (4, 14, 24) OR name ILIKE '%Heineken%';
UPDATE menus SET master_item_id = 5 WHERE master_item_id IN (5, 15, 25) OR name ILIKE '%Vang%';
UPDATE menus SET master_item_id = 6 WHERE master_item_id IN (6, 16, 26) OR name ILIKE '%Gi_t l_%';
UPDATE menus SET master_item_id = 37 WHERE master_item_id IN (7, 17, 27) OR name ILIKE '%s_n bay%';
UPDATE menus SET master_item_id = 8 WHERE master_item_id IN (8, 18, 28) OR name ILIKE '%xe m_y%';
UPDATE menus SET master_item_id = 9 WHERE master_item_id IN (9, 19, 29) OR name ILIKE '%Massage%';
UPDATE menus SET master_item_id = 10 WHERE master_item_id IN (10, 20, 30) OR name ILIKE '%tr_ng m_t%';

-- 3. Cập nhật các bản ghi trong hotel_room_type_inclusions nếu đang trỏ tới các master_item_id nhân bản
UPDATE hotel_room_type_inclusions SET master_item_id = 36 WHERE master_item_id IN (1, 11, 21);
UPDATE hotel_room_type_inclusions SET master_item_id = 2 WHERE master_item_id IN (12, 22);
UPDATE hotel_room_type_inclusions SET master_item_id = 3 WHERE master_item_id IN (13, 23);
UPDATE hotel_room_type_inclusions SET master_item_id = 4 WHERE master_item_id IN (14, 24);
UPDATE hotel_room_type_inclusions SET master_item_id = 5 WHERE master_item_id IN (15, 25);
UPDATE hotel_room_type_inclusions SET master_item_id = 6 WHERE master_item_id IN (16, 26);
UPDATE hotel_room_type_inclusions SET master_item_id = 37 WHERE master_item_id IN (7, 17, 27);
UPDATE hotel_room_type_inclusions SET master_item_id = 8 WHERE master_item_id IN (18, 28);
UPDATE hotel_room_type_inclusions SET master_item_id = 9 WHERE master_item_id IN (19, 29);
UPDATE hotel_room_type_inclusions SET master_item_id = 10 WHERE master_item_id IN (20, 30);

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

-- 5. Xóa bỏ hoàn toàn các bản ghi Master Items nhân bản rác (IDs 1, 7, 11..30)
DELETE FROM master_items 
WHERE id IN (1, 7, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30);
