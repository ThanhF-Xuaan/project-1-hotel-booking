-- The current StayGuest DTO/entity owns full_name, not the legacy split-name fields.
-- Preserve old columns and existing data for legacy readers; keep full_name mandatory.
ALTER TABLE stay_guests
    ALTER COLUMN first_name DROP NOT NULL,
    ALTER COLUMN last_name DROP NOT NULL,
    ALTER COLUMN guest_type DROP NOT NULL;
