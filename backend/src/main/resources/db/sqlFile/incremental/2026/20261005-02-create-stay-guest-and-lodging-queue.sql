CREATE TABLE IF NOT EXISTS stay_guests (
    id BIGSERIAL PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    booking_id BIGINT REFERENCES bookings(id),
    room_id INT REFERENCES room_instances(id),
    room_number VARCHAR(20) NOT NULL,
    full_name VARCHAR(250) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(20),
    nationality VARCHAR(100) NOT NULL DEFAULT 'Việt Nam',
    document_type VARCHAR(20) NOT NULL DEFAULT 'CCCD',
    document_number VARCHAR(50) NOT NULL,
    permanent_address TEXT,
    current_address TEXT,
    check_in_time TIMESTAMP WITH TIME ZONE NOT NULL,
    expected_check_out_time TIMESTAMP WITH TIME ZONE NOT NULL,
    actual_check_out_time TIMESTAMP WITH TIME ZONE,
    reason_for_stay VARCHAR(150) DEFAULT 'Du lịch',
    document_image_url TEXT,
    image_purged_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(50),
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS lodging_queues (
    id BIGSERIAL PRIMARY KEY,
    stay_guest_id BIGINT NOT NULL REFERENCES stay_guests(id),
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    exported_at TIMESTAMP WITH TIME ZONE,
    batch_reference VARCHAR(100),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(50),
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_stay_guests_hotel_checkin ON stay_guests(hotel_id, check_in_time);
CREATE INDEX IF NOT EXISTS idx_stay_guests_document ON stay_guests(document_number, is_deleted);
CREATE INDEX IF NOT EXISTS idx_lodging_queues_status_hotel ON lodging_queues(hotel_id, status, is_deleted);
CREATE INDEX IF NOT EXISTS idx_lodging_queues_stay_guest ON lodging_queues(stay_guest_id);

-- Thêm quyền RBAC cho Lodging Queue & Export
INSERT INTO roles_permissions (role_name, permission) VALUES
('ROLE_CHAIN_ADMIN', 'ROLE_LODGING_VIEW'),
('ROLE_CHAIN_ADMIN', 'ROLE_LODGING_MANAGE'),
('ROLE_CHAIN_ADMIN', 'ROLE_LODGING_EXPORT'),
('ROLE_PROPERTY_MANAGER', 'ROLE_LODGING_VIEW'),
('ROLE_PROPERTY_MANAGER', 'ROLE_LODGING_MANAGE'),
('ROLE_PROPERTY_MANAGER', 'ROLE_LODGING_EXPORT'),
('ROLE_RECEPTIONIST', 'ROLE_LODGING_VIEW'),
('ROLE_RECEPTIONIST', 'ROLE_LODGING_MANAGE'),
('ROLE_RECEPTIONIST', 'ROLE_LODGING_EXPORT')
ON CONFLICT DO NOTHING;
