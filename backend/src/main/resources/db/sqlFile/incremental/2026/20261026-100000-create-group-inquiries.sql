-- ==============================================================================
-- Liquibase Migration: Create Group Inquiries and Inquiry Room Types
-- Date: 2026-10-26 10:00:00
-- Author: member2
-- Description: Tao bang group_inquiries va group_inquiry_room_types tiep nhan yeu cau dat phong doan
-- ==============================================================================

CREATE TABLE IF NOT EXISTS group_inquiries (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inquiry_number          VARCHAR(50) NOT NULL UNIQUE,
    hotel_id                SMALLINT NOT NULL REFERENCES hotels(id),
    company_name            VARCHAR(255) NOT NULL,
    tax_code                VARCHAR(50),
    contact_name            VARCHAR(150) NOT NULL,
    contact_email           VARCHAR(150) NOT NULL,
    contact_phone           VARCHAR(50) NOT NULL,
    check_in_date           DATE NOT NULL,
    check_out_date          DATE NOT NULL,
    total_rooms             INTEGER NOT NULL,
    total_guests            INTEGER NOT NULL DEFAULT 1,
    estimated_budget        NUMERIC(15, 2),
    special_requests        TEXT,
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sales_staff_id          INT REFERENCES staffs(id),
    converted_contract_id   BIGINT REFERENCES group_contracts(id),
    rejection_reason        VARCHAR(500),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ,

    CONSTRAINT chk_group_inquiry_status CHECK (status IN ('PENDING', 'CONTACTED', 'QUOTED', 'CONVERTED', 'REJECTED')),
    CONSTRAINT chk_group_inquiry_dates CHECK (check_out_date > check_in_date),
    CONSTRAINT chk_group_inquiry_rooms CHECK (total_rooms >= 1)
);

CREATE INDEX IF NOT EXISTS idx_group_inquiries_hotel_status ON group_inquiries (hotel_id, status) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_group_inquiries_number ON group_inquiries (inquiry_number);
CREATE INDEX IF NOT EXISTS idx_group_inquiries_check_in ON group_inquiries (hotel_id, check_in_date);

CREATE TABLE IF NOT EXISTS group_inquiry_room_types (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inquiry_id                  BIGINT NOT NULL REFERENCES group_inquiries(id) ON DELETE CASCADE,
    hotel_room_type_id          INT NOT NULL REFERENCES hotel_room_types(id),
    requested_rooms             INTEGER NOT NULL,
    estimated_rate_per_night    NUMERIC(15, 2),
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ,

    CONSTRAINT chk_inquiry_room_count CHECK (requested_rooms >= 1)
);

CREATE INDEX IF NOT EXISTS idx_inquiry_room_types_inquiry ON group_inquiry_room_types (inquiry_id) WHERE is_deleted = FALSE;
