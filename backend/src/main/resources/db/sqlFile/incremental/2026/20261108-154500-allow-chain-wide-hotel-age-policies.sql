-- Cho phep hotel_id NULL dai dien cho Chinh sach Mac dinh Toan Chuoi (Fallback Hierarchy)
ALTER TABLE hotel_age_policies ALTER COLUMN hotel_id DROP NOT NULL;

-- Partial unique index cho Chinh sach Toan Chuoi (hotel_id IS NULL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_chain_age_policies_active 
    ON hotel_age_policies (guest_type) 
    WHERE hotel_id IS NULL AND is_deleted = FALSE;

-- Khoi tao 3 quy chuan do tuoi mac dinh cho toan chuoi neu chua ton tai
INSERT INTO hotel_age_policies (hotel_id, guest_type, min_age, max_age, status, is_deleted)
SELECT NULL, v.guest_type, v.min_age, v.max_age, 'ACTIVE', FALSE
FROM (
    VALUES 
        ('INFANT', 0::SMALLINT, 5::SMALLINT),
        ('CHILD', 6::SMALLINT, 11::SMALLINT),
        ('ADULT', 12::SMALLINT, 100::SMALLINT)
) AS v(guest_type, min_age, max_age)
WHERE NOT EXISTS (
    SELECT 1 FROM hotel_age_policies 
    WHERE hotel_id IS NULL AND guest_type = v.guest_type AND is_deleted = FALSE
);
