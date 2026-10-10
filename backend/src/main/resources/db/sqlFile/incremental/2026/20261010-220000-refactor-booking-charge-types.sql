-- ==============================================================================
-- ChangeSet: 20261010-220000-refactor-booking-charge-types
-- Description: Cập nhật ràng buộc charge_type cho booking_charges:
--              chỉ lưu các khoản phụ thu phòng & phí phạt (PENALTY, EARLY_CHECKIN,
--              LATE_CHECKOUT, EXTRA_PERSON, EXTRA_BED, OTHER).
-- ==============================================================================

-- 1. Chuyển đổi dữ liệu cũ (nếu có bản ghi 'SERVICE' trước đó sang 'OTHER')
UPDATE booking_charges 
SET charge_type = 'OTHER' 
WHERE charge_type = 'SERVICE';

-- 2. Cập nhật lại ràng buộc CHECK trên bảng booking_charges
ALTER TABLE booking_charges DROP CONSTRAINT IF EXISTS chk_booking_charge_type;

ALTER TABLE booking_charges ADD CONSTRAINT chk_booking_charge_type CHECK (
    charge_type IN ('EARLY_CHECKIN', 'LATE_CHECKOUT', 'EXTRA_PERSON', 'EXTRA_BED', 'PENALTY', 'OTHER')
);
