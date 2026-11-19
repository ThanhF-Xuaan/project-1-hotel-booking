-- ==============================================================================
-- Incremental Migration: Đồng bộ số tiền ROOM_RATE và chi tiết phụ thu Folio Ledger
-- ChangeSet ID: 20261119-140000-sync-folio-room-rate-and-surcharge-breakdown
-- Author: utc-hotel-team
-- ==============================================================================

-- 1. Cập nhật amount của các bút toán Tiền phòng (ROOM_RATE) thành Gross Amount (Đã gồm Phí DV & VAT)
UPDATE folio_transactions
SET amount = subtotal + COALESCE(service_fee_amount, 0) + COALESCE(vat_amount, 0)
WHERE category = 'ROOM_RATE';

-- 2. Đồng bộ lại unit_price, subtotal, phí DV, VAT từ bảng booking_charges cho các bút toán BOOKING_CHARGE (như LATE_CHECKOUT)
UPDATE folio_transactions ft
SET 
    quantity = bc.quantity,
    unit_price = bc.unit_price,
    subtotal = bc.subtotal,
    service_fee_rate = bc.service_fee_rate,
    service_fee_amount = bc.service_fee_amount,
    vat_rate = bc.vat_rate,
    vat_amount = bc.vat_amount,
    amount = bc.total_amount
FROM booking_charges bc
WHERE ft.source_type = 'BOOKING_CHARGE' AND ft.source_id = bc.id;

-- 3. Cập nhật lại số dư total_debits, total_credits và balance trong bảng folios
UPDATE folios f
SET 
    total_debits = COALESCE((SELECT SUM(amount) FROM folio_transactions WHERE folio_id = f.id AND transaction_type = 'DEBIT'), 0),
    total_credits = COALESCE((SELECT SUM(amount) FROM folio_transactions WHERE folio_id = f.id AND transaction_type = 'CREDIT'), 0);

UPDATE folios
SET balance = total_debits - total_credits;

