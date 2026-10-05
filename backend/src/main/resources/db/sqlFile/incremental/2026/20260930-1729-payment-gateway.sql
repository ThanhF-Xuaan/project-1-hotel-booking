-- =============================================================================
-- MIGRATION: Thêm 3 cột gateway cho thanh toán online (VNPay / MoMo)
-- Bảng payments ĐÃ CÓ SẴN (không thêm mới): transaction_reference,
--   paid_at, payment_provider, payment_method (CHECK constraint)
-- =============================================================================

ALTER TABLE payments ADD COLUMN gateway_txn_id VARCHAR(100) UNIQUE;

ALTER TABLE payments ADD COLUMN gateway_response_code VARCHAR(50);

ALTER TABLE payments ADD COLUMN raw_callback_payload JSONB;
