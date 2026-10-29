-- guest_type is mapped and defaults to ADULT; preserve the original required discriminator.
ALTER TABLE stay_guests ALTER COLUMN guest_type SET NOT NULL;
