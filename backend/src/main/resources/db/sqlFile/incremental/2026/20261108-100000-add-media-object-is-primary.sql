-- Liquibase SQL: Bo sung cot is_primary va partial unique index vao bang media_objects
-- Phuc vu quan ly anh dai dien da hinh (Hotel Property, Room Types, Amenities, Banners)

ALTER TABLE media_objects 
ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT FALSE;

-- Dam bao moi thuc the (reference_id theo category) chi co toi da 1 anh dai dien chinh con hieu luc (is_deleted = false)
CREATE UNIQUE INDEX IF NOT EXISTS uq_media_objects_primary_per_ref 
ON media_objects(category, reference_id) 
WHERE is_primary = TRUE AND is_deleted = FALSE;

