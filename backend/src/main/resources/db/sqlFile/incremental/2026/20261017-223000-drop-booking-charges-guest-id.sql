-- ==============================================================================
-- INCREMENTAL MIGRATION: DROP BOOKING_GUEST_ID FROM BOOKING_CHARGES
-- Bỏ cột booking_guest_id vì phụ phí thuộc cấp BookingRoom, không phụ thuộc guest lẻ
-- ==============================================================================

ALTER TABLE booking_charges DROP COLUMN IF EXISTS booking_guest_id;
