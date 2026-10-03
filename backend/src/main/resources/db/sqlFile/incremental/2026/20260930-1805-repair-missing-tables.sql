-- =============================================================================
-- REPAIR: Tạo 8 bảng bị thiếu trong DB dev (schema lệch do chạy SQL tay)
-- DDL lấy nguyên văn từ database/01-init-schema.sql
-- Chỉ ADD (IF NOT EXISTS) — KHÔNG DROP / TRUNCATE / DELETE
-- Thứ tự: bảng cha đứng trước bảng con
-- =============================================================================

CREATE TABLE IF NOT EXISTS regions (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS departments (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS menus (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id INT REFERENCES hotels(id),
    tax_category_id INT REFERENCES tax_categories(id),
    menu_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_price DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS services (
    id INT PRIMARY KEY REFERENCES menus(id),
    pricing_type VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS room_maintenance_blocks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_instance_id INT NOT NULL REFERENCES room_instances(id),
    block_type VARCHAR(20) NOT NULL DEFAULT 'OOO',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    maintenance_ticket_id BIGINT,
    created_by_staff_id INT REFERENCES staffs(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_block_type CHECK (block_type IN ('OOO', 'OOS')),
    CONSTRAINT chk_block_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_block_dates CHECK (start_date <= end_date)
);

CREATE TABLE IF NOT EXISTS companies (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_code VARCHAR(50) UNIQUE,
    address TEXT,
    contact_name VARCHAR(100),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(150),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS booking_daily_rates (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_room_id BIGINT REFERENCES booking_rooms(id),
    stay_date DATE NOT NULL,
    base_price DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    surcharge_amount DECIMAL(15,2) DEFAULT 0,
    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    tax_category_id INT REFERENCES tax_categories(id),
    vat_percent DECIMAL(5,2) NOT NULL,
    vat_amount DECIMAL(15,2) NOT NULL,
    net_price DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    staff_id INT REFERENCES staffs(id) ON DELETE SET NULL,
    action_type VARCHAR(50) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
