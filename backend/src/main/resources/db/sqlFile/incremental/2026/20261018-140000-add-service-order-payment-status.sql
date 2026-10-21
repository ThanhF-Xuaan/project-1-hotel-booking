-- ==============================================================================
-- ChangeSet: 20261018-140000-add-service-order-payment-status
-- Description: Bổ sung hình thức thanh toán (payment_method) và trạng thái thanh toán
--              (payment_status) cho bảng service_orders nhằm phân định rõ ràng giữa
--              đơn ghi nợ phòng (POST_TO_ROOM) và đơn thanh toán tại chỗ (CASH, VNPAY),
--              ngăn chặn triệt để nguy cơ thu tiền 2 lần khi check-out xuất hóa đơn.
-- ==============================================================================

-- 1. Bổ sung cột payment_method
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) NOT NULL DEFAULT 'POST_TO_ROOM';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_service_orders_payment_method'
    ) THEN
        ALTER TABLE service_orders ADD CONSTRAINT chk_service_orders_payment_method
            CHECK (payment_method IN ('POST_TO_ROOM', 'CASH', 'VNPAY'));
    END IF;
END $$;

-- 2. Bổ sung cột payment_status
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_service_orders_payment_status'
    ) THEN
        ALTER TABLE service_orders ADD CONSTRAINT chk_service_orders_payment_status
            CHECK (payment_status IN ('UNPAID', 'PAID'));
    END IF;
END $$;

-- 3. Cập nhật dữ liệu cũ
-- Khách vãng lai (WALK_IN) mặc định đã thanh toán tại quầy
UPDATE service_orders
SET payment_method = 'CASH', payment_status = 'PAID'
WHERE reference_type = 'WALK_IN';

-- Khách lưu trú có phòng mặc định là ghi nợ phòng chờ check-out
UPDATE service_orders
SET payment_method = 'POST_TO_ROOM', payment_status = 'UNPAID'
WHERE reference_type IN ('BOOKING', 'BOOKING_ROOM', 'STAY_GUEST') AND payment_status IS NULL;

-- 4. Tạo Index phục vụ lọc hóa đơn Folio
CREATE INDEX IF NOT EXISTS idx_service_orders_payment ON service_orders(payment_method, payment_status);
