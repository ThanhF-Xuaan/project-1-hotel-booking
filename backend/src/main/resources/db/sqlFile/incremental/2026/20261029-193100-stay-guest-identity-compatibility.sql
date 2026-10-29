-- Accept either complete modern or complete legacy identity; adults still require identity.
ALTER TABLE stay_guests DROP CONSTRAINT chk_adult_requires_identity;
ALTER TABLE stay_guests ADD CONSTRAINT chk_adult_requires_identity CHECK (
 guest_type <> 'ADULT'
 OR (document_type IS NOT NULL AND document_number IS NOT NULL AND btrim(document_number) <> '')
 OR (identity_type IS NOT NULL AND identity_number IS NOT NULL AND identity_number <> '')
);
