-- ==============================================================================
-- BẢNG MASTER_FOLIO_ITEM_ADJUSTMENTS (Lưu Vết Điều Chuyển Chi Phí Thủ Công)
-- Đáp ứng Ngoại lệ 2a trong SRS UC-FIN-04, Luật Kế toán 2015 & Luật Giao dịch điện tử 2023
-- ==============================================================================
CREATE TABLE IF NOT EXISTS master_folio_item_adjustments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    master_folio_id BIGINT NOT NULL REFERENCES master_folios(id) ON DELETE CASCADE,
    booking_room_id BIGINT REFERENCES booking_rooms(id) ON DELETE SET NULL,
    charge_category VARCHAR(50) NOT NULL,
    item_type VARCHAR(50) NOT NULL, -- 'ROOM_RATE', 'BOOKING_CHARGE', 'SERVICE_ORDER_DETAIL'
    reference_id BIGINT NOT NULL,   -- ID của BookingDailyRate, BookingCharge, hoặc ServiceOrderDetail
    item_description VARCHAR(255) NOT NULL,
    adjusted_target VARCHAR(30) NOT NULL, -- 'MASTER' hoặc 'SUB_FOLIO'
    amount NUMERIC(15,2) NOT NULL,
    adjustment_reason TEXT NOT NULL,
    adjusted_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_adj_target CHECK (adjusted_target IN ('MASTER', 'SUB_FOLIO'))
);

CREATE INDEX IF NOT EXISTS idx_folio_adj_folio ON master_folio_item_adjustments(master_folio_id);
CREATE INDEX IF NOT EXISTS idx_folio_adj_ref ON master_folio_item_adjustments(item_type, reference_id);
