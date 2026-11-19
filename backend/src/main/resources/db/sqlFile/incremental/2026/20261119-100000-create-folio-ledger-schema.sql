-- ==============================================================================
-- ChangeSet: 20261119-100000-create-folio-ledger-schema
-- Description: Khởi tạo mô hình Sổ cái Bút toán kép (Double-Entry Folio Ledger)
--              theo chuẩn PMS quốc tế (Oracle Opera PMS, Cloudbeds).
--              Tách riêng Sổ cái chi tiêu lưu trú (folios & folio_transactions)
--              với Hợp đồng đặt phòng (bookings).
--              Bảo toàn 100% dữ liệu gốc và thực hiện Idempotent Backfill tự động.
-- ==============================================================================

-- 1. Bảng folios: Quản lý Sổ cái phòng (Guest Folio)
CREATE TABLE IF NOT EXISTS folios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    booking_room_id BIGINT REFERENCES booking_rooms(id) ON DELETE SET NULL,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    folio_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    total_debits NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_credits NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    closed_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_folio_status CHECK (status IN ('OPEN', 'LOCKED_FOR_SETTLEMENT', 'SETTLED', 'CLOSED', 'VOID'))
);

CREATE INDEX IF NOT EXISTS idx_folios_booking ON folios(booking_id);
CREATE INDEX IF NOT EXISTS idx_folios_room ON folios(booking_room_id);
CREATE INDEX IF NOT EXISTS idx_folios_hotel_status ON folios(hotel_id, status);

-- Trigger sinh mã Folio chống trùng lặp theo Cách 1: FOL-{booking_number}-{room_number/MAIN}
CREATE OR REPLACE FUNCTION generate_folio_number()
RETURNS TRIGGER AS $$
DECLARE
    v_booking_num VARCHAR(50);
    v_room_num VARCHAR(20);
BEGIN
    IF NEW.folio_number IS NULL OR NEW.folio_number = '' THEN
        SELECT booking_number INTO v_booking_num FROM bookings WHERE id = NEW.booking_id;
        
        IF NEW.booking_room_id IS NOT NULL THEN
            SELECT ri.room_number INTO v_room_num 
            FROM booking_rooms br
            LEFT JOIN room_instances ri ON br.room_instance_id = ri.id
            WHERE br.id = NEW.booking_room_id;
        END IF;

        IF v_room_num IS NOT NULL AND v_room_num != '' THEN
            NEW.folio_number := 'FOL-' || v_booking_num || '-' || v_room_num;
        ELSE
            NEW.folio_number := 'FOL-' || v_booking_num || '-MAIN';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_generate_folio_number ON folios;
CREATE TRIGGER trigger_generate_folio_number
BEFORE INSERT ON folios
FOR EACH ROW EXECUTE FUNCTION generate_folio_number();

-- 2. Bảng folio_transactions: Bút toán chi tiết (DEBIT / CREDIT)
CREATE TABLE IF NOT EXISTS folio_transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    folio_id BIGINT NOT NULL REFERENCES folios(id) ON DELETE CASCADE,
    transaction_type VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id BIGINT,
    description VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    subtotal NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    vat_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    vat_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    amount NUMERIC(15,2) NOT NULL,
    payment_method VARCHAR(50),
    posted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    posted_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_folio_tx_type CHECK (transaction_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_folio_tx_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_folio_tx_folio ON folio_transactions(folio_id);
CREATE INDEX IF NOT EXISTS idx_folio_tx_source ON folio_transactions(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_folio_tx_category ON folio_transactions(category);

-- ==============================================================================
-- 3. DATA BACKFILL SCRIPT (Idempotent - Bảo toàn 100% dữ liệu lịch sử)
-- ==============================================================================

-- Bước 3.1: Khởi tạo Folio cho mọi Booking đã tồn tại theo mã Cách 1
INSERT INTO folios (booking_id, booking_room_id, hotel_id, folio_number, status, created_at, updated_at)
SELECT 
    b.id, 
    br.id, 
    b.hotel_id, 
    'FOL-' || b.booking_number || '-' || COALESCE(ri.room_number, 'MAIN'),
    CASE WHEN b.status = 'COMPLETED' THEN 'CLOSED' ELSE 'OPEN' END,
    b.created_at, 
    b.updated_at
FROM bookings b
LEFT JOIN booking_details bd ON b.id = bd.booking_id
LEFT JOIN booking_rooms br ON bd.id = br.booking_detail_id
LEFT JOIN room_instances ri ON br.room_instance_id = ri.id
ON CONFLICT (folio_number) DO NOTHING;

-- Bước 3.2: Backfill Bút toán Tiền phòng lưu trú (DEBIT - ROOM_RATE)
INSERT INTO folio_transactions (
    folio_id, transaction_type, category, source_type, source_id, description,
    quantity, unit_price, subtotal, service_fee_rate, service_fee_amount, vat_rate, vat_amount, amount, posted_at
)
SELECT 
    f.id, 'DEBIT', 'ROOM_RATE', 'DAILY_RATE', bdr.id, 
    COALESCE('Phòng ' || ri.room_number || ' - Đêm ngày ' || bdr.stay_date, 'Tiền phòng ngày ' || bdr.stay_date),
    1, bdr.base_price, bdr.base_price, bdr.service_fee_rate, bdr.service_fee_amount, bdr.vat_percent, bdr.vat_amount, 
    bdr.net_price, COALESCE(bdr.created_at, f.created_at)
FROM booking_daily_rates bdr
JOIN booking_rooms br ON bdr.booking_room_id = br.id
JOIN folios f ON f.booking_room_id = br.id
LEFT JOIN room_instances ri ON br.room_instance_id = ri.id
WHERE br.status != 'NO_SHOW'
  AND NOT EXISTS (
      SELECT 1 FROM folio_transactions ft
      WHERE ft.source_type = 'DAILY_RATE' AND ft.source_id = bdr.id
  );

-- Bước 3.3: Backfill Bút toán Phụ thu & Phạt (DEBIT - SURCHARGE / PENALTY)
INSERT INTO folio_transactions (
    folio_id, transaction_type, category, source_type, source_id, description,
    quantity, unit_price, subtotal, service_fee_rate, service_fee_amount, vat_rate, vat_amount, amount, posted_at
)
SELECT 
    f.id, 'DEBIT', bc.charge_type, 'BOOKING_CHARGE', bc.id, bc.item_name,
    bc.quantity, bc.unit_price, bc.subtotal, bc.service_fee_rate, bc.service_fee_amount, bc.vat_rate, bc.vat_amount, 
    bc.total_amount, COALESCE(bc.created_at, f.created_at)
FROM booking_charges bc
JOIN folios f ON f.booking_room_id = bc.booking_room_id
WHERE NOT EXISTS (
    SELECT 1 FROM folio_transactions ft
    WHERE ft.source_type = 'BOOKING_CHARGE' AND ft.source_id = bc.id
);

-- Bước 3.4: Backfill Bút toán Đơn dịch vụ HỢP LỆ (DEBIT - SERVICE / PRODUCT)
-- LOẠI BỎ hoàn toàn đơn VNPAY UNPAID (bỏ dở)! CHỈ lấy đơn POST_TO_ROOM hoặc đã PAID
INSERT INTO folio_transactions (
    folio_id, transaction_type, category, source_type, source_id, description,
    quantity, unit_price, subtotal, service_fee_rate, service_fee_amount, vat_rate, vat_amount, amount, payment_method, posted_at
)
SELECT 
    f.id, 'DEBIT', 'SERVICE_ORDER', 'SERVICE_ORDER', so.id, 'Đơn dịch vụ #' || so.order_number,
    1, so.sub_total, so.sub_total, so.service_fee_rate, so.service_fee_amount, 0, so.vat_amount, 
    so.total_amount, so.payment_method, COALESCE(so.issued_at, so.created_at)
FROM service_orders so
JOIN folios f ON (
    (so.reference_type = 'BOOKING' AND so.reference_id = f.booking_id) OR
    (so.reference_type = 'BOOKING_ROOM' AND so.reference_id = f.booking_room_id)
)
WHERE so.status != 'CANCELLED'
  AND (so.payment_method = 'POST_TO_ROOM' OR so.payment_status = 'PAID')
  AND NOT EXISTS (
      SELECT 1 FROM folio_transactions ft
      WHERE ft.source_type = 'SERVICE_ORDER' AND ft.source_id = so.id
  );

-- Bước 3.5: Backfill Bút toán Thanh toán thành công (CREDIT - PAYMENT)
INSERT INTO folio_transactions (
    folio_id, transaction_type, category, source_type, source_id, description,
    quantity, unit_price, subtotal, service_fee_rate, service_fee_amount, vat_rate, vat_amount, amount, payment_method, posted_at
)
SELECT 
    f.id, 'CREDIT', 'PAYMENT', 'PAYMENT', p.id, 
    'Thanh toán ' || p.payment_method || ' (' || COALESCE(p.transaction_reference, p.payment_purpose) || ')',
    1, p.total_amount, p.total_amount, 0, 0, 0, 0, 
    p.total_amount, p.payment_method, COALESCE(p.paid_at, p.created_at)
FROM payments p
JOIN folios f ON p.booking_id = f.booking_id
WHERE p.status = 'SUCCESS'
  AND NOT EXISTS (
      SELECT 1 FROM folio_transactions ft
      WHERE ft.source_type = 'PAYMENT' AND ft.source_id = p.id
  );

-- Bước 3.6: Cập nhật tổng số dư cho các Folio
UPDATE folios f
SET 
    total_debits = COALESCE((SELECT SUM(amount) FROM folio_transactions WHERE folio_id = f.id AND transaction_type = 'DEBIT'), 0),
    total_credits = COALESCE((SELECT SUM(amount) FROM folio_transactions WHERE folio_id = f.id AND transaction_type = 'CREDIT'), 0);

UPDATE folios
SET balance = total_debits - total_credits;

