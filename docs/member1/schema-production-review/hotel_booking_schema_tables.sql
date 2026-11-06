-- ==============================================================================
-- SMART HOTEL BOOKING SYSTEM - CONSOLIDATED DATABASE SCHEMA (TABLES ONLY)
-- Updated: 2026-11-06. Status: PROPOSED; NOT approved for production.
-- 77 table definitions. Only Module 9-10 aligned with Member 1 repository source.
-- Source HEAD: 63a26ba4c81c92e9892383de29bf76bc96b617b9 (local working tree).
-- Module 1-8 preserved; compatibility with the application remains UNVERIFIED.
-- Fresh-database reference only: NOT an upgrade or a replacement Liquibase baseline.
-- Required separately: pgvector extension, supporting indexes/triggers, master data.
-- See hotel_booking_member1_schema_review.md before use.
-- ==============================================================================

-- ==============================================================================
-- MODULE 1: SYSTEM, IAM, ORGANIZATIONS & INFRASTRUCTURE
-- ==============================================================================

-- 1. roles
CREATE TABLE IF NOT EXISTS roles (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. permissions
CREATE TABLE IF NOT EXISTS permissions (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    module VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. role_permissions
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id SMALLINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id SMALLINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

-- 4. regions
CREATE TABLE IF NOT EXISTS regions (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. departments
CREATE TABLE IF NOT EXISTS departments (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. hotels
CREATE TABLE IF NOT EXISTS hotels (
    id SMALLSERIAL PRIMARY KEY,
    region_id SMALLINT REFERENCES regions(id),
    name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'Việt Nam',
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    star_rating SMALLINT DEFAULT 3 CHECK (star_rating BETWEEN 1 AND 5),
    check_in_time TIME NOT NULL DEFAULT '14:00:00',
    check_out_time TIME NOT NULL DEFAULT '12:00:00',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. staffs
CREATE TABLE IF NOT EXISTS staffs (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT REFERENCES hotels(id),
    department_id SMALLINT REFERENCES departments(id),
    role_id SMALLINT NOT NULL REFERENCES roles(id),
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. keycloak_sync_dead_letters
CREATE TABLE IF NOT EXISTS keycloak_sync_dead_letters (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    username VARCHAR(100),
    email VARCHAR(150),
    payload JSONB NOT NULL,
    failure_reason TEXT NOT NULL,
    error_stacktrace TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'FAILED', 'RESOLVED', 'ABANDONED')),
    last_retry_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. shedlock
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

-- 10. companies
CREATE TABLE IF NOT EXISTS companies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    tax_code VARCHAR(50) UNIQUE,
    address TEXT,
    contact_person VARCHAR(100),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. media_objects
CREATE TABLE IF NOT EXISTS media_objects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    bucket_name VARCHAR(63) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_extension VARCHAR(10),
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0),
    sha256_checksum VARCHAR(64),
    category VARCHAR(50) NOT NULL CHECK (category IN ('IDENTITY_DOCUMENT', 'CONTRACT_ATTACHMENT', 'HOTEL_GALLERY', 'ROOM_GALLERY', 'AVATAR', 'RECEIPT', 'OTHER')),
    access_level VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' CHECK (access_level IN ('PUBLIC', 'INTERNAL', 'PRIVATE', 'CONFIDENTIAL')),
    uploaded_by_user_id VARCHAR(100),
    uploaded_by_role VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('UPLOADING', 'ACTIVE', 'ARCHIVED', 'DELETED', 'QUARANTINED')),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. audit_logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    staff_id INT REFERENCES staffs(id),
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- MODULE 2: TAXES, POLICIES & CALENDARS
-- ==============================================================================

-- 13. tax_categories
CREATE TABLE IF NOT EXISTS tax_categories (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 14. vat_rules
CREATE TABLE IF NOT EXISTS vat_rules (
    id SERIAL PRIMARY KEY,
    tax_category_id SMALLINT NOT NULL REFERENCES tax_categories(id),
    tax_rate NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. hotel_age_policies
CREATE TABLE IF NOT EXISTS hotel_age_policies (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    max_child_age SMALLINT NOT NULL DEFAULT 5,
    min_adult_age SMALLINT NOT NULL DEFAULT 12,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_hotel_age_policy UNIQUE (hotel_id)
);

-- 16. holiday_calendars
CREATE TABLE IF NOT EXISTS holiday_calendars (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT REFERENCES hotels(id),
    holiday_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_hotel_holiday UNIQUE (hotel_id, holiday_date)
);

-- 17. cancellation_policies
CREATE TABLE IF NOT EXISTS cancellation_policies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    free_cancellation_hours INT NOT NULL DEFAULT 24 CHECK (free_cancellation_hours >= 0),
    cancellation_fee_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00 CHECK (cancellation_fee_percent >= 0 AND cancellation_fee_percent <= 100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 18. no_show_policies
CREATE TABLE IF NOT EXISTS no_show_policies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    first_night_penalty BOOLEAN NOT NULL DEFAULT TRUE,
    full_stay_penalty BOOLEAN NOT NULL DEFAULT FALSE,
    penalty_percent NUMERIC(5,2) NOT NULL DEFAULT 100.00 CHECK (penalty_percent >= 0 AND penalty_percent <= 100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 19. property_policy_overrides
CREATE TABLE IF NOT EXISTS property_policy_overrides (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    cancellation_policy_id INT REFERENCES cancellation_policies(id),
    no_show_policy_id INT REFERENCES no_show_policies(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_property_policy_override UNIQUE (hotel_id)
);

-- ==============================================================================
-- MODULE 3: ROOM TYPES, ROOMS & INVENTORY
-- ==============================================================================

-- 20. room_types
CREATE TABLE IF NOT EXISTS room_types (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 21. hotel_room_types
CREATE TABLE IF NOT EXISTS hotel_room_types (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    room_type_id SMALLINT NOT NULL REFERENCES room_types(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    base_occupancy SMALLINT NOT NULL DEFAULT 2,
    max_adults SMALLINT NOT NULL DEFAULT 2,
    max_children SMALLINT NOT NULL DEFAULT 1,
    max_occupancy SMALLINT NOT NULL DEFAULT 3,
    room_size_sqm NUMERIC(5,2),
    default_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    cancellation_policy_id INT REFERENCES cancellation_policies(id) ON DELETE RESTRICT,
    no_show_policy_id INT REFERENCES no_show_policies(id) ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_hotel_room_types_id_hotel UNIQUE (id, hotel_id)
);

-- 22. room_beds
CREATE TABLE IF NOT EXISTS room_beds (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    capacity SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 23. hotel_room_type_beds
CREATE TABLE IF NOT EXISTS hotel_room_type_beds (
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id) ON DELETE CASCADE,
    room_bed_id SMALLINT NOT NULL REFERENCES room_beds(id),
    quantity SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY (hotel_room_type_id, room_bed_id)
);

-- 24. room_features
CREATE TABLE IF NOT EXISTS room_features (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    icon_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 25. hotel_room_type_features
CREATE TABLE IF NOT EXISTS hotel_room_type_features (
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id) ON DELETE CASCADE,
    room_feature_id SMALLINT NOT NULL REFERENCES room_features(id),
    PRIMARY KEY (hotel_room_type_id, room_feature_id)
);

-- 26. room_instances
CREATE TABLE IF NOT EXISTS room_instances (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    room_number VARCHAR(20) NOT NULL,
    floor SMALLINT NOT NULL,
    current_status VARCHAR(50) NOT NULL DEFAULT 'READY',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_hotel_room_number UNIQUE (hotel_id, room_number),
    CONSTRAINT chk_room_instance_status CHECK (current_status IN ('READY', 'OCCUPIED', 'DIRTY', 'CLEANING', 'MAINTENANCE'))
);

-- 27. room_maintenance_blocks
CREATE TABLE IF NOT EXISTS room_maintenance_blocks (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    ooo_rooms INT NOT NULL DEFAULT 0,
    reason TEXT NOT NULL,
    updated_by_staff_id INT REFERENCES staffs(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_room_maintenance_blocks_dates CHECK (start_date <= end_date),
    CONSTRAINT chk_ooo_rooms_non_negative CHECK (ooo_rooms >= 0)
);

-- 28. room_availability
CREATE TABLE IF NOT EXISTS room_availability (
    id BIGSERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    date DATE NOT NULL,
    total_rooms INT NOT NULL,
    booked_rooms INT NOT NULL DEFAULT 0,
    ooo_rooms INT NOT NULL DEFAULT 0,
    available_rooms INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_room_availability UNIQUE (hotel_id, hotel_room_type_id, date),
    CONSTRAINT chk_ooo_rooms_non_negative CHECK (ooo_rooms >= 0)
);

-- 29. room_inventory_holds
CREATE TABLE IF NOT EXISTS room_inventory_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(100) NOT NULL,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    rooms_count INT NOT NULL DEFAULT 1 CHECK (rooms_count > 0),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    hold_token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_hold_dates CHECK (start_date < end_date)
);

-- ==============================================================================
-- MODULE 4: PRICING, SURCHARGES, DISCOUNTS & INCLUSIONS
-- ==============================================================================

-- 30. pricing_rule_types
CREATE TABLE IF NOT EXISTS pricing_rule_types (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 31. discount_rule_types
CREATE TABLE IF NOT EXISTS discount_rule_types (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 32. pricing_rules
CREATE TABLE IF NOT EXISTS pricing_rules (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    hotel_room_type_id INT REFERENCES hotel_room_types(id),
    pricing_rule_type_id SMALLINT NOT NULL REFERENCES pricing_rule_types(id),
    adjustment_type VARCHAR(20) NOT NULL,
    adjustment_value NUMERIC(15,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 33. campaigns
CREATE TABLE IF NOT EXISTS campaigns (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT REFERENCES hotels(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    code VARCHAR(50) UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 34. discount_rules
CREATE TABLE IF NOT EXISTS discount_rules (
    id SERIAL PRIMARY KEY,
    campaign_id INT REFERENCES campaigns(id),
    discount_rule_type_id SMALLINT NOT NULL REFERENCES discount_rule_types(id),
    hotel_room_type_id INT REFERENCES hotel_room_types(id),
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(15,2) NOT NULL,
    min_nights SMALLINT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 35. surcharge_rules
CREATE TABLE IF NOT EXISTS surcharge_rules (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    hotel_room_type_id INT REFERENCES hotel_room_types(id),
    surcharge_type VARCHAR(50) NOT NULL,
    amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 36. hotel_room_type_inclusions
CREATE TABLE IF NOT EXISTS hotel_room_type_inclusions (
    id SERIAL PRIMARY KEY,
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- MODULE 5: MASTER ITEMS, MENUS & VOUCHERS
-- ==============================================================================

-- 37. master_items
CREATE TABLE IF NOT EXISTS master_items (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    default_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    unit VARCHAR(30) NOT NULL DEFAULT 'ITEM',
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_master_items_category CHECK (category IN ('FOOD', 'BEVERAGE', 'SERVICE', 'LAUNDRY', 'SPA', 'TRANSPORT', 'AMENITY', 'OTHER'))
);

-- 38. menus
CREATE TABLE IF NOT EXISTS menus (
    id SERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    master_item_id INT REFERENCES master_items(id),
    price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    stock_quantity INT NOT NULL DEFAULT 0,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 39. vouchers
CREATE TABLE IF NOT EXISTS vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED_AMOUNT')),
    discount_value NUMERIC(15,2) NOT NULL CHECK (discount_value > 0),
    max_discount_amount NUMERIC(15,2),
    min_order_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    total_quantity INT NOT NULL DEFAULT 0 CHECK (total_quantity >= 0),
    used_quantity INT NOT NULL DEFAULT 0 CHECK (used_quantity >= 0),
    max_uses_per_user INT NOT NULL DEFAULT 1 CHECK (max_uses_per_user > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_voucher_dates CHECK (start_date < end_date)
);

-- ==============================================================================
-- MODULE 6: GUESTS, GROUP CONTRACTS & INQUIRIES
-- ==============================================================================

-- 40. guests
CREATE TABLE IF NOT EXISTS guests (
    id BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(100) UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    id_card_number VARCHAR(50) UNIQUE,
    nationality VARCHAR(100) DEFAULT 'Việt Nam',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 41. group_inquiries
CREATE TABLE IF NOT EXISTS group_inquiries (
    id BIGSERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    organizer_name VARCHAR(150) NOT NULL,
    company_name VARCHAR(150),
    contact_phone VARCHAR(20) NOT NULL,
    contact_email VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_expected_rooms INT NOT NULL CHECK (total_expected_rooms > 0),
    estimated_budget NUMERIC(15,2),
    special_requests TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'CONTACTED', 'QUOTED', 'CONTRACTED', 'CANCELLED')),
    assigned_staff_id INT REFERENCES staffs(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_group_inquiries_dates CHECK (start_date < end_date)
);

-- 42. group_inquiry_room_types
CREATE TABLE IF NOT EXISTS group_inquiry_room_types (
    id BIGSERIAL PRIMARY KEY,
    group_inquiry_id BIGINT NOT NULL REFERENCES group_inquiries(id) ON DELETE CASCADE,
    room_type_id SMALLINT NOT NULL REFERENCES room_types(id),
    requested_rooms INT NOT NULL CHECK (requested_rooms > 0),
    expected_rate NUMERIC(15,2)
);

-- 43. group_contracts
CREATE TABLE IF NOT EXISTS group_contracts (
    id BIGSERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    contract_number VARCHAR(100) NOT NULL UNIQUE,
    contract_name VARCHAR(255) NOT NULL,
    company_id INT REFERENCES companies(id),
    contact_person VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    contact_email VARCHAR(100) NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_rooms INT NOT NULL CHECK (total_rooms > 0),
    total_contract_value NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    deposit_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    payment_terms TEXT,
    cancellation_terms TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    sales_staff_id INT REFERENCES staffs(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_group_contract_dates CHECK (check_in_date < check_out_date)
);

-- 44. contract_attachments
CREATE TABLE IF NOT EXISTS contract_attachments (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES group_contracts(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size_bytes BIGINT,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 45. group_room_blocks
CREATE TABLE IF NOT EXISTS group_room_blocks (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES group_contracts(id) ON DELETE CASCADE,
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    agreed_rate NUMERIC(15,2) NOT NULL CHECK (agreed_rate >= 0),
    allocated_rooms INT NOT NULL CHECK (allocated_rooms > 0),
    cutoff_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 46. group_room_block_daily
CREATE TABLE IF NOT EXISTS group_room_block_daily (
    id BIGSERIAL PRIMARY KEY,
    group_room_block_id BIGINT NOT NULL REFERENCES group_room_blocks(id) ON DELETE CASCADE,
    stay_date DATE NOT NULL,
    blocked_rooms INT NOT NULL CHECK (blocked_rooms >= 0),
    picked_up_rooms INT NOT NULL DEFAULT 0 CHECK (picked_up_rooms >= 0),
    CONSTRAINT uq_block_daily UNIQUE (group_room_block_id, stay_date)
);

-- 47. master_folios
CREATE TABLE IF NOT EXISTS master_folios (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES group_contracts(id) ON DELETE CASCADE,
    company_id INT REFERENCES companies(id),
    total_charges NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_payments NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'SETTLED', 'CLOSED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 48. master_folio_item_adjustments
CREATE TABLE IF NOT EXISTS master_folio_item_adjustments (
    id BIGSERIAL PRIMARY KEY,
    master_folio_id BIGINT NOT NULL REFERENCES master_folios(id) ON DELETE CASCADE,
    adjustment_type VARCHAR(50) NOT NULL CHECK (adjustment_type IN ('DISCOUNT', 'SURCHARGE', 'CORRECTION', 'TAX_ADJUSTMENT')),
    amount NUMERIC(15,2) NOT NULL,
    reason TEXT NOT NULL,
    adjusted_by_staff_id INT REFERENCES staffs(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 49. billing_routing_rules
CREATE TABLE IF NOT EXISTS billing_routing_rules (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES group_contracts(id) ON DELETE CASCADE,
    master_folio_id BIGINT NOT NULL REFERENCES master_folios(id) ON DELETE CASCADE,
    charge_category VARCHAR(50) NOT NULL CHECK (charge_category IN ('ROOM_ONLY', 'ALL_CHARGES', 'ROOM_AND_BREAKFAST', 'INCIDENTALS_ONLY')),
    routing_target VARCHAR(50) NOT NULL CHECK (routing_target IN ('MASTER_FOLIO', 'INDIVIDUAL_GUEST')),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- MODULE 7: BOOKINGS, RESERVATIONS & STAYS
-- ==============================================================================

-- 50. bookings
CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    guest_id BIGINT NOT NULL REFERENCES guests(id),
    contract_id BIGINT REFERENCES group_contracts(id),
    booking_code VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    final_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 51. booking_details
CREATE TABLE IF NOT EXISTS booking_details (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    cancellation_policy_id INT REFERENCES cancellation_policies(id) ON DELETE RESTRICT,
    no_show_policy_id INT REFERENCES no_show_policies(id) ON DELETE RESTRICT,
    cancellation_policy_snapshot JSONB,
    no_show_policy_snapshot JSONB,
    quantity SMALLINT NOT NULL DEFAULT 1,
    price_per_night NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 52. booking_rooms
CREATE TABLE IF NOT EXISTS booking_rooms (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    room_instance_id INT REFERENCES room_instances(id),
    check_in_time TIMESTAMP WITH TIME ZONE,
    check_out_time TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'RESERVED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 53. booking_guests
CREATE TABLE IF NOT EXISTS booking_guests (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    guest_id BIGINT NOT NULL REFERENCES guests(id),
    full_name VARCHAR(250),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_booking_guest UNIQUE (booking_id, guest_id)
);

-- 54. stay_guests
CREATE TABLE IF NOT EXISTS stay_guests (
    id BIGSERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    booking_id BIGINT REFERENCES bookings(id) ON DELETE SET NULL,
    booking_room_id BIGINT REFERENCES booking_rooms(id) ON DELETE SET NULL,
    room_id INT REFERENCES room_instances(id) ON DELETE SET NULL,
    room_number VARCHAR(20),
    full_name VARCHAR(250) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    guest_type VARCHAR(20) NOT NULL DEFAULT 'ADULT',
    date_of_birth DATE,
    gender VARCHAR(20),
    nationality VARCHAR(100) NOT NULL DEFAULT 'Việt Nam',
    document_type VARCHAR(20) NOT NULL DEFAULT 'CCCD',
    document_number VARCHAR(50),
    identity_type VARCHAR(50),
    identity_number VARCHAR(50),
    permanent_address TEXT,
    current_address TEXT,
    check_in_time TIMESTAMP WITH TIME ZONE,
    expected_check_out_time TIMESTAMP WITH TIME ZONE,
    actual_check_out_time TIMESTAMP WITH TIME ZONE,
    reason_for_stay VARCHAR(150) DEFAULT 'Du lịch',
    document_image_url TEXT,
    image_purged_at TIMESTAMP WITH TIME ZONE,
    active_dedup_key bytea,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_stay_guest_active_dedup_length CHECK (active_dedup_key IS NULL OR octet_length(active_dedup_key) = 32),
    CONSTRAINT chk_adult_requires_identity CHECK (
        guest_type <> 'ADULT'
        OR (document_type IS NOT NULL AND document_number IS NOT NULL AND btrim(document_number) <> '')
        OR (identity_type IS NOT NULL AND identity_number IS NOT NULL AND identity_number <> '')
    )
);

-- 55. lodging_queues
CREATE TABLE IF NOT EXISTS lodging_queues (
    id BIGSERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    stay_guest_id BIGINT NOT NULL REFERENCES stay_guests(id) ON DELETE CASCADE,
    report_date DATE NOT NULL,
    submission_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (submission_status IN ('PENDING', 'SENT', 'ACCEPTED', 'REJECTED')),
    police_confirmation_code VARCHAR(100),
    response_payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP WITH TIME ZONE,
    submitted_by_staff_id INT REFERENCES staffs(id)
);

-- 56. booking_daily_rates
CREATE TABLE IF NOT EXISTS booking_daily_rates (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    booking_room_id BIGINT REFERENCES booking_rooms(id) ON DELETE CASCADE,
    stay_date DATE NOT NULL,
    base_rate NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    adjusted_rate NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 57. room_slots
CREATE TABLE IF NOT EXISTS room_slots (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    room_instance_id INT REFERENCES room_instances(id),
    slot_date DATE NOT NULL,
    price NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 58. room_swap_records
CREATE TABLE IF NOT EXISTS room_swap_records (
    id BIGSERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    old_room_id INT NOT NULL REFERENCES room_instances(id),
    new_room_id INT NOT NULL REFERENCES room_instances(id),
    reason TEXT NOT NULL,
    swapped_by_staff_id INT REFERENCES staffs(id),
    price_difference NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    swap_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 59. voucher_usages
CREATE TABLE IF NOT EXISTS voucher_usages (
    id BIGSERIAL PRIMARY KEY,
    voucher_id BIGINT NOT NULL REFERENCES vouchers(id) ON DELETE CASCADE,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    guest_id BIGINT REFERENCES guests(id),
    discount_applied NUMERIC(15,2) NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- MODULE 8: SERVICE ORDERS, CHARGES, PAYMENTS & BILLING
-- ==============================================================================

-- 60. service_orders
CREATE TABLE IF NOT EXISTS service_orders (
    id BIGSERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    reference_id BIGINT,
    reference_type VARCHAR(50) NOT NULL DEFAULT 'BOOKING' CHECK (reference_type IN ('BOOKING', 'WALK_IN', 'EVENT', 'STAFF')),
    staff_id INT REFERENCES staffs(id),
    total_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    order_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) NOT NULL DEFAULT 'POST_TO_ROOM' CHECK (payment_method IN ('POST_TO_ROOM', 'CASH', 'VNPAY')),
    payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID', 'PAID')),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 61. service_order_details
CREATE TABLE IF NOT EXISTS service_order_details (
    id BIGSERIAL PRIMARY KEY,
    service_order_id BIGINT NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    menu_id INT NOT NULL REFERENCES menus(id),
    quantity SMALLINT NOT NULL DEFAULT 1,
    unit_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 62. booking_charges
CREATE TABLE IF NOT EXISTS booking_charges (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    booking_room_id BIGINT REFERENCES booking_rooms(id),
    charge_type VARCHAR(50) NOT NULL,
    amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_booking_charge_type CHECK (
        charge_type IN ('EARLY_CHECKIN', 'LATE_CHECKOUT', 'EXTRA_PERSON', 'EXTRA_BED', 'PENALTY', 'OTHER')
    )
);

-- 63. payments
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    payment_method VARCHAR(50) NOT NULL,
    payment_provider VARCHAR(50),
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_reference VARCHAR(100) UNIQUE,
    gateway_txn_id VARCHAR(100) UNIQUE,
    gateway_response_code VARCHAR(50),
    raw_callback_payload JSONB,
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 64. transactions
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    transaction_type VARCHAR(50) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    gateway_response JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 65. invoices
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    guest_id BIGINT NOT NULL REFERENCES guests(id),
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    issue_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    final_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'ISSUED',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 66. invoice_details
CREATE TABLE IF NOT EXISTS invoice_details (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    quantity SMALLINT NOT NULL DEFAULT 1,
    unit_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_price NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 67. night_audits
CREATE TABLE IF NOT EXISTS night_audits (
    id BIGSERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    business_date DATE NOT NULL,
    total_rooms_occupied INT NOT NULL DEFAULT 0,
    total_rooms_available INT NOT NULL DEFAULT 0,
    total_room_revenue NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_service_revenue NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_surcharge_revenue NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_payment_collected NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    audit_status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED' CHECK (audit_status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    performed_by_staff_id INT REFERENCES staffs(id),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    CONSTRAINT uq_hotel_business_date UNIQUE (hotel_id, business_date)
);

-- ==============================================================================
-- MODULE 9: UTILITIES & HR/PAYROLL (TASKS T04, T05)
-- ==============================================================================

-- 68. utility_meters
-- Source: 20261003-150000-create-utility-tables.sql
CREATE TABLE utility_meters (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id        SMALLINT NOT NULL REFERENCES hotels(id),
    meter_code      VARCHAR(50) NOT NULL,
    meter_type      VARCHAR(20) NOT NULL CHECK (meter_type IN ('ELECTRICITY', 'WATER')),
    location_label  VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 69. utility_readings
-- Source: 20261003-150000-create-utility-tables.sql
CREATE TABLE utility_readings (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meter_id        INT NOT NULL REFERENCES utility_meters(id),
    reading_date    DATE NOT NULL,
    reading_value   NUMERIC(12,3) NOT NULL CHECK (reading_value >= 0),
    is_meter_reset  BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_by     INT NOT NULL REFERENCES staffs(id),
    updated_by      INT REFERENCES staffs(id),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 70. payroll_summary
-- Source: 20261004-120000-create-payroll-summary.sql
CREATE TABLE payroll_summary (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    staff_id        INT NOT NULL REFERENCES staffs(id),
    period_month    INT NOT NULL,
    period_year     INT NOT NULL,
    base_salary     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    bonus           NUMERIC(15, 2) NOT NULL DEFAULT 0,
    deductions      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    net_salary      NUMERIC(15, 2) NOT NULL DEFAULT 0,

    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- MODULE 10: PERSONALIZATION & RAG KNOWLEDGE BASE (TASKS U14, T12)
-- U14: local synthetic-only contract; real customer identity is NOT approved.
-- RAG: requires pgvector installed separately; datasource deployment remains local-only.
-- ==============================================================================

-- 71. personalization_account_bindings
-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE TABLE personalization_account_bindings (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    issuer VARCHAR(512) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    guest_id BIGINT NOT NULL REFERENCES booking_guests(id) ON DELETE RESTRICT,
    binding_version BIGINT NOT NULL CHECK (binding_version > 0),
    source VARCHAR(50) NOT NULL DEFAULT 'SYNTHETIC_FIXTURE'
        CHECK (source = 'SYNTHETIC_FIXTURE'),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'REVOKED')),
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_personalization_binding_issuer_nonblank CHECK (btrim(issuer) <> ''),
    CONSTRAINT chk_personalization_binding_subject_nonblank CHECK (btrim(subject) <> ''),
    CONSTRAINT chk_personalization_binding_lifecycle CHECK (
        (status = 'ACTIVE' AND revoked_at IS NULL)
        OR (status = 'REVOKED' AND revoked_at IS NOT NULL)
    ),
    CONSTRAINT uq_personalization_binding_subject_version UNIQUE (issuer, subject, binding_version),
    CONSTRAINT uq_personalization_binding_id_version UNIQUE (id, binding_version)
);

-- 72. personalization_consents
-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE TABLE personalization_consents (
    binding_id BIGINT NOT NULL REFERENCES personalization_account_bindings(id) ON DELETE RESTRICT,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id) ON DELETE RESTRICT,
    purpose VARCHAR(50) NOT NULL CHECK (purpose = 'ROOM_TYPE_RECOMMENDATION'),
    granted BOOLEAN NOT NULL,
    revision BIGINT NOT NULL CHECK (revision >= 1),
    policy_version VARCHAR(100),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_personalization_consent_policy_nonblank
        CHECK (policy_version IS NULL OR btrim(policy_version) <> ''),
    CONSTRAINT chk_personalization_consent_grant_policy
        CHECK (NOT granted OR policy_version IS NOT NULL),
    CONSTRAINT pk_personalization_consents PRIMARY KEY (binding_id, hotel_id, purpose)
);

-- 73. personalization_consent_events
-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE TABLE personalization_consent_events (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    binding_id BIGINT NOT NULL,
    binding_version BIGINT NOT NULL CHECK (binding_version > 0),
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id) ON DELETE RESTRICT,
    purpose VARCHAR(50) NOT NULL CHECK (purpose = 'ROOM_TYPE_RECOMMENDATION'),
    revision BIGINT NOT NULL CHECK (revision >= 1),
    granted BOOLEAN NOT NULL,
    policy_version VARCHAR(100),
    event_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_personalization_event_policy_nonblank
        CHECK (policy_version IS NULL OR btrim(policy_version) <> ''),
    CONSTRAINT chk_personalization_event_grant_policy
        CHECK (NOT granted OR policy_version IS NOT NULL),
    CONSTRAINT fk_personalization_event_binding_version
        FOREIGN KEY (binding_id, binding_version)
        REFERENCES personalization_account_bindings(id, binding_version) ON DELETE RESTRICT,
    CONSTRAINT uq_personalization_consent_event_revision
        UNIQUE (binding_id, hotel_id, purpose, revision)
);

-- 74. rag_store_config
-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE TABLE rag_store_config (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    embedding_model_id VARCHAR(128) NOT NULL,
    embedding_dimension INTEGER NOT NULL CHECK (embedding_dimension BETWEEN 1 AND 16000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(embedding_model_id, embedding_dimension)
);

-- 75. rag_sources
-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE TABLE rag_sources (
    id BIGSERIAL PRIMARY KEY,
    scope_type VARCHAR(32) NOT NULL CHECK (scope_type IN ('HOTEL_SCOPED', 'CHAIN_PUBLIC')),
    hotel_id SMALLINT REFERENCES hotels(id),
    hotel_key SMALLINT GENERATED ALWAYS AS (coalesce(hotel_id, 0)) STORED,
    source_id VARCHAR(128) NOT NULL CHECK (source_id ~ '^[A-Z0-9][A-Z0-9._:-]{0,127}$'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((scope_type = 'HOTEL_SCOPED' AND hotel_id IS NOT NULL AND hotel_id > 0) OR (scope_type = 'CHAIN_PUBLIC' AND hotel_id IS NULL)),
    UNIQUE(scope_type, hotel_key, source_id)
);

-- 76. rag_document_versions
-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE TABLE rag_document_versions (
    id UUID PRIMARY KEY,
    source_key BIGINT NOT NULL REFERENCES rag_sources(id),
    version INTEGER NOT NULL CHECK (version > 0),
    status VARCHAR(32) NOT NULL CHECK (status IN ('STAGED', 'ACTIVE', 'SUPERSEDED', 'ROLLED_BACK')),
    embedding_model_id VARCHAR(128) NOT NULL,
    embedding_dimension INTEGER NOT NULL CHECK (embedding_dimension BETWEEN 1 AND 16000),
    content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    effective_from DATE,
    effective_to DATE,
    payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object' AND octet_length(payload::text) <= 270336),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    activated_at TIMESTAMPTZ,
    CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from),
    FOREIGN KEY(embedding_model_id, embedding_dimension) REFERENCES rag_store_config(embedding_model_id, embedding_dimension),
    UNIQUE(source_key, version),
    UNIQUE(id, embedding_dimension)
);

-- 77. rag_chunks
-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE TABLE rag_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    embedding_dimension INTEGER NOT NULL,
    embedding vector NOT NULL,
    payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object' AND octet_length(payload::text) <= 34816),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY(document_id, embedding_dimension) REFERENCES rag_document_versions(id, embedding_dimension) ON DELETE CASCADE,
    CHECK (vector_dims(embedding) = embedding_dimension),
    UNIQUE(document_id, chunk_index)
);

