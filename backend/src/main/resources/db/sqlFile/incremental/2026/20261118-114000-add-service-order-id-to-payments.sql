-- ==============================================================================
-- ChangeSet: 20261118-114000-add-service-order-id-to-payments
-- Description: Bổ sung liên kết service_order_id vào bảng payments nhằm hỗ trợ
--              thanh toán trực tuyến (VNPay/MoMo) trực tiếp cho đơn dịch vụ phòng POS
--              thay vì chỉ thanh toán toàn bộ đơn đặt phòng Booking.
-- ==============================================================================

-- 1. Bổ sung cột service_order_id (nullable) trỏ tới service_orders
ALTER TABLE payments ADD COLUMN IF NOT EXISTS service_order_id BIGINT REFERENCES service_orders(id) ON DELETE SET NULL;

-- 2. Đánh Index phục vụ truy vấn tra cứu giao dịch theo đơn dịch vụ
CREATE INDEX IF NOT EXISTS idx_payments_service_order_id ON payments(service_order_id);

