-- ==============================================================================
-- INCREMENTAL MIGRATION: 20261025-140000-create-group-booking-schema.sql
-- Module: Khách đoàn (GIT) & Doanh nghiệp (SRS Section 1.4.9, FR-GRP-01 -> FR-GRP-06)
-- Tuân thủ: Luật Giao dịch điện tử 2023, NĐ 123/2020/NĐ-CP, NĐ 13/2023/NĐ-CP,
--           Luật Kế toán 2015, Bộ luật Dân sự 2015, Luật Thương mại 2005.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. BẢNG GROUP_CONTRACTS (Hợp đồng Khách đoàn & Doanh nghiệp)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS group_contracts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_number VARCHAR(50) NOT NULL UNIQUE,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    sales_staff_id INT NOT NULL REFERENCES staffs(id),

    -- Thông tin đoàn và đại diện pháp lý (Luật Thương mại 2005 & NĐ 123/2020/NĐ-CP)
    group_name VARCHAR(255) NOT NULL,
    company_tax_code VARCHAR(50) NOT NULL,
    company_legal_representative VARCHAR(150),
    signer_name VARCHAR(150),
    signer_title VARCHAR(100),

    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_rooms INT NOT NULL,
    total_guests INT NOT NULL DEFAULT 1,

    -- Giá trị tài chính & Bóc tách thuế phí (Luật Thuế GTGT & NĐ 123/2020/NĐ-CP)
    subtotal_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 5.00,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    vat_rate NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    vat_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_contract_value NUMERIC(15,2) NOT NULL DEFAULT 0.00,

    -- Điều khoản đặt cọc 50% (Điều 328 BLDS 2015 & SRS FR-GRP-01)
    deposit_rate NUMERIC(5,2) NOT NULL DEFAULT 50.00,
    deposit_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    deposit_paid_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    deposit_due_date TIMESTAMPTZ NOT NULL,
    deposit_paid_at TIMESTAMPTZ,

    -- Hạn chốt danh sách & Thanh toán cuối (SRS FR-GRP-03, SQ-04, SQ-05)
    cutoff_date TIMESTAMPTZ NOT NULL,
    final_payment_due_date TIMESTAMPTZ NOT NULL,
    lock_checkin BOOLEAN NOT NULL DEFAULT FALSE,

    -- Điều khoản pháp lý & Trạng thái
    governing_law VARCHAR(100) NOT NULL DEFAULT 'VIETNAMESE_LAW',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    cancellation_reason TEXT,
    special_requests TEXT,

    -- BaseEntity
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_group_contract_status CHECK (
        status IN ('DRAFT', 'TENTATIVE', 'DEPOSIT_PAID', 'ROOMING_LIST_ATTACHED', 'IN_HOUSE', 'SETTLED', 'COMPLETED', 'CANCELLED_NO_DEPOSIT', 'CANCELLED')
    ),
    CONSTRAINT chk_group_contract_dates CHECK (check_in_date < check_out_date),
    CONSTRAINT chk_group_contract_min_rooms CHECK (total_rooms >= 6),
    CONSTRAINT chk_group_contract_deposit_rate CHECK (deposit_rate >= 50.00)
);

CREATE INDEX IF NOT EXISTS idx_group_contracts_hotel ON group_contracts(hotel_id);
CREATE INDEX IF NOT EXISTS idx_group_contracts_company ON group_contracts(company_id);
CREATE INDEX IF NOT EXISTS idx_group_contracts_staff ON group_contracts(sales_staff_id);
CREATE INDEX IF NOT EXISTS idx_group_contracts_status ON group_contracts(status, is_deleted);
CREATE INDEX IF NOT EXISTS idx_group_contracts_dates ON group_contracts(check_in_date, check_out_date);
CREATE INDEX IF NOT EXISTS idx_group_contracts_cutoff ON group_contracts(cutoff_date, status);
CREATE INDEX IF NOT EXISTS idx_group_contracts_deposit_due ON group_contracts(deposit_due_date, status);

-- Sequence & Trigger tự động sinh contract_number: GCT-YYYYMMDD-XXXX
CREATE SEQUENCE IF NOT EXISTS contract_number_seq START 1;

CREATE OR REPLACE FUNCTION generate_group_contract_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.contract_number IS NULL OR NEW.contract_number = '' THEN
        NEW.contract_number := 'GCT-' 
                            || to_char(CURRENT_DATE, 'YYYYMMDD') 
                            || '-' 
                            || LPAD(nextval('contract_number_seq')::text, 4, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_generate_group_contract_number
BEFORE INSERT ON group_contracts
FOR EACH ROW
EXECUTE FUNCTION generate_group_contract_number();


-- ------------------------------------------------------------------------------
-- 2. BẢNG CONTRACT_ATTACHMENTS (Quản lý File Hợp Đồng Số & Xác Thực Pháp Lý)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS contract_attachments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES group_contracts(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL DEFAULT 'CONTRACT_MAIN',
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,

    -- Toàn vẹn thông điệp dữ liệu & Chữ ký số (Luật Giao dịch điện tử 2023)
    sha256_checksum VARCHAR(64) NOT NULL,
    is_digitally_signed BOOLEAN NOT NULL DEFAULT FALSE,
    signature_algorithm VARCHAR(50),
    certificate_serial VARCHAR(100),
    signed_by_subject VARCHAR(255),
    signing_timestamp TIMESTAMPTZ,

    version_number INT NOT NULL DEFAULT 1,
    uploaded_by_staff_id INT NOT NULL REFERENCES staffs(id),
    retention_years INT NOT NULL DEFAULT 10, -- Tuân thủ Điều 41 Luật Kế toán 2015

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_contract_attachment_size CHECK (file_size_bytes > 0 AND file_size_bytes <= 15728640),
    CONSTRAINT chk_contract_attachment_type CHECK (
        file_type IN ('CONTRACT_MAIN', 'AMENDMENT', 'PAYMENT_RECEIPT', 'ROOMING_LIST_ORIGINAL')
    )
);

CREATE INDEX IF NOT EXISTS idx_contract_attachments_contract ON contract_attachments(contract_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_contract_attachments_checksum ON contract_attachments(sha256_checksum);


-- ------------------------------------------------------------------------------
-- 3. BẢNG GROUP_ROOM_BLOCKS (Quỹ Phòng Khách Đoàn)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS group_room_blocks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES group_contracts(id) ON DELETE CASCADE,
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    agreed_rate NUMERIC(15,2) NOT NULL,
    total_blocked_rooms INT NOT NULL,
    allocated_rooms INT NOT NULL DEFAULT 0,
    released_rooms INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'TENTATIVE',

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_group_room_block_qty CHECK (total_blocked_rooms > 0),
    CONSTRAINT chk_group_room_block_rate CHECK (agreed_rate >= 0),
    CONSTRAINT chk_group_room_block_status CHECK (status IN ('TENTATIVE', 'DEFINITE', 'RELEASED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_group_room_blocks_contract ON group_room_blocks(contract_id);
CREATE INDEX IF NOT EXISTS idx_group_room_blocks_room_type ON group_room_blocks(hotel_room_type_id);
CREATE INDEX IF NOT EXISTS idx_group_room_blocks_status ON group_room_blocks(status, is_deleted);


-- ------------------------------------------------------------------------------
-- 4. BẢNG GROUP_ROOM_BLOCK_DAILY (Chi Tiết Quỹ Phòng Theo Từng Ngày Lưu Trú)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS group_room_block_daily (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_block_id BIGINT NOT NULL REFERENCES group_room_blocks(id) ON DELETE CASCADE,
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    stay_date DATE NOT NULL,
    blocked_quantity INT NOT NULL,
    picked_up_quantity INT NOT NULL DEFAULT 0,
    released_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_group_room_block_daily UNIQUE (room_block_id, stay_date),
    CONSTRAINT chk_room_block_daily_qty CHECK (blocked_quantity >= 0),
    CONSTRAINT chk_room_block_daily_status CHECK (status IN ('ACTIVE', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_group_room_block_daily_stay ON group_room_block_daily(hotel_room_type_id, stay_date, status);


-- ------------------------------------------------------------------------------
-- 5. BẢNG MASTER_FOLIOS (Sổ Cái Doanh Thu Tổng Hợp Đoàn & Hóa Đơn GTGT B2B)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS master_folios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    folio_number VARCHAR(50) NOT NULL UNIQUE,
    contract_id BIGINT NOT NULL REFERENCES group_contracts(id),
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),

    -- Dữ liệu pháp nhân xuất hóa đơn điện tử GTGT (NĐ 123/2020/NĐ-CP & TT 78/2021/TT-BTC)
    buyer_legal_name VARCHAR(255) NOT NULL,
    buyer_tax_code VARCHAR(50) NOT NULL,
    buyer_address TEXT NOT NULL,
    buyer_email_for_invoice VARCHAR(150),

    -- Hạch toán tài chính
    subtotal_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_vat_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    deposit_credited NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_paid NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,

    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    closed_at TIMESTAMPTZ,

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_master_folio_status CHECK (status IN ('OPEN', 'LOCKED_FOR_SETTLEMENT', 'SETTLED', 'CLOSED'))
);

CREATE INDEX IF NOT EXISTS idx_master_folios_contract ON master_folios(contract_id);
CREATE INDEX IF NOT EXISTS idx_master_folios_company ON master_folios(company_id);
CREATE INDEX IF NOT EXISTS idx_master_folios_hotel ON master_folios(hotel_id);
CREATE INDEX IF NOT EXISTS idx_master_folios_status ON master_folios(status, is_deleted);

-- Sequence & Trigger tự động sinh folio_number: MF-YYYYMMDD-XXXX
CREATE SEQUENCE IF NOT EXISTS master_folio_number_seq START 1;

CREATE OR REPLACE FUNCTION generate_master_folio_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.folio_number IS NULL OR NEW.folio_number = '' THEN
        NEW.folio_number := 'MF-' 
                         || to_char(CURRENT_DATE, 'YYYYMMDD') 
                         || '-' 
                         || LPAD(nextval('master_folio_number_seq')::text, 4, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_generate_master_folio_number
BEFORE INSERT ON master_folios
FOR EACH ROW
EXECUTE FUNCTION generate_master_folio_number();


-- ------------------------------------------------------------------------------
-- 6. BẢNG BILLING_ROUTING_RULES (Quy Tắc Định Tuyến Chi Phí Đoàn Tự Động)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS billing_routing_rules (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    master_folio_id BIGINT NOT NULL REFERENCES master_folios(id) ON DELETE CASCADE,
    charge_category VARCHAR(50) NOT NULL,
    route_target VARCHAR(30) NOT NULL DEFAULT 'MASTER',
    max_covered_amount NUMERIC(15,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_routing_charge_cat CHECK (
        charge_category IN ('ROOM_RATE', 'BREAKFAST', 'BANQUET', 'LAUNDRY', 'MINIBAR', 'ALL', 'CUSTOM')
    ),
    CONSTRAINT chk_routing_target CHECK (route_target IN ('MASTER', 'SUB_FOLIO'))
);

CREATE INDEX IF NOT EXISTS idx_billing_routing_rules_folio ON billing_routing_rules(master_folio_id, is_active);


-- ------------------------------------------------------------------------------
-- 7. MỞ RỘNG BẢNG BOOKINGS (Liên Kết An Toàn Với Group Contract)
-- Không làm ảnh hưởng luồng khách lẻ (FIT) - contract_id cho phép NULL.
-- ------------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'bookings' AND column_name = 'contract_id'
    ) THEN
        ALTER TABLE bookings ADD COLUMN contract_id BIGINT NULL REFERENCES group_contracts(id);
        CREATE INDEX idx_bookings_contract_id ON bookings(contract_id);
    END IF;
END $$;
