-- =============================================================================
-- MIGRATION: Cập nhật CHECK constraint chk_booking_charge_type cho bảng booking_charges
-- Bổ sung giá trị 'SERVICE' cho enum BookingChargeType (F&B, Giặt là, POS dịch vụ phòng)
-- =============================================================================

ALTER TABLE booking_charges DROP CONSTRAINT IF EXISTS chk_booking_charge_type;

ALTER TABLE booking_charges ADD CONSTRAINT chk_booking_charge_type
    CHECK (charge_type IN ('EARLY_CHECKIN', 'LATE_CHECKOUT', 'PENALTY', 'SERVICE', 'OTHER'));
