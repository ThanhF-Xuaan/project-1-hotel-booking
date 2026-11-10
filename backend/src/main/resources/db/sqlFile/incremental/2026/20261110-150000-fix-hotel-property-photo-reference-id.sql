-- Liquibase SQL: Dong bo reference_id = hotel_id cho cac anh khuon vien khach san (HOTEL_PROPERTY_PHOTO)
-- Cac anh khuon vien tai len tu Kho Da phuong tien truoc day bi thieu reference_id (NULL)

-- 1. Cap nhat reference_id = hotel_id cho cac anh khuon vien dang bi NULL
UPDATE media_objects
SET reference_id = hotel_id
WHERE category = 'HOTEL_PROPERTY_PHOTO'
  AND reference_id IS NULL
  AND hotel_id IS NOT NULL;

-- 2. Tu dong kich hoat anh dai dien chinh (is_primary = true) cho cac khach san da co anh khuon vien nhung chua co anh chinh
UPDATE media_objects mo
SET is_primary = true
WHERE mo.id IN (
    SELECT DISTINCT ON (reference_id) id
    FROM media_objects
    WHERE category = 'HOTEL_PROPERTY_PHOTO'
      AND reference_id IS NOT NULL
      AND is_deleted = false
    ORDER BY reference_id, created_at ASC
)
AND NOT EXISTS (
    SELECT 1 FROM media_objects p
    WHERE p.category = 'HOTEL_PROPERTY_PHOTO'
      AND p.reference_id = mo.reference_id
      AND p.is_primary = true
      AND p.is_deleted = false
);
