-- ==============================================================================
-- ACCEPTANCE FIXTURES FOR SMART HOTEL BOOKING (SANDBOX)
-- Designed for standalone verification on database: hotel_acceptance_db
-- ==============================================================================

-- 1. REGIONS
INSERT INTO regions (code, name, description, status) VALUES
    ('NORTH', 'Miá»n Báº¯c', 'Khu vá»±c phÃ­a Báº¯c (HÃ  Ná»™i, Sapa)', 'ACTIVE'),
    ('CENTRAL', 'Miá»n Trung', 'Khu vá»±c miá»n Trung (ÄÃ  Náºµng, Huáº¿)', 'ACTIVE'),
    ('SOUTH', 'Miá»n Nam', 'Khu vá»±c phÃ­a Nam (TP.HCM, PhÃº Quá»‘c)', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- 2. DEPARTMENTS
INSERT INTO departments (code, name, status) VALUES
    ('FRONT_OFFICE', 'Lá»… tÃ¢n Tiá»n sáº£nh', 'ACTIVE'),
    ('HOUSEKEEPING', 'Buá»“ng phÃ²ng', 'ACTIVE'),
    ('ENGINEERING', 'Ká»¹ thuáº­t & Báº£o trÃ¬', 'ACTIVE'),
    ('FINANCE', 'TÃ i chÃ­nh & Káº¿ toÃ¡n', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- 3. HOTELS (2 acceptance hotels)
INSERT INTO hotels (id, region_id, name, address, phone, check_in_time, check_out_time, service_fee_percent, status)
OVERRIDING SYSTEM VALUE
VALUES
    (1, (SELECT id FROM regions WHERE code = 'NORTH'), 'Acceptance Luxury Hanoi (Hà Nội)', '100 Hoan Kiem, Ha Noi (Hà Nội)', '02411112222', '14:00:00', '12:00:00', 5.00, 'ACTIVE'),
    (2, (SELECT id FROM regions WHERE code = 'CENTRAL'), 'Acceptance Grand Da Nang (Đà Nẵng)', '200 Vo Nguyen Giap, Da Nang (Đà Nẵng)', '02361112222', '14:00:00', '12:00:00', 5.00, 'ACTIVE')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    address = EXCLUDED.address,
    phone = EXCLUDED.phone,
    service_fee_percent = EXCLUDED.service_fee_percent,
    status = EXCLUDED.status;

SELECT setval('hotels_id_seq', (SELECT GREATEST(MAX(id), 2) FROM hotels));

-- 4. HOTEL AGE POLICIES
INSERT INTO hotel_age_policies (hotel_id, guest_type, min_age, max_age)
SELECT h.id, p.guest_type, p.min_age, p.max_age
FROM hotels h
CROSS JOIN (
    VALUES
        ('INFANT', 0, 5),
        ('CHILD', 6, 11),
        ('ADULT', 12, 99)
) AS p(guest_type, min_age, max_age)
WHERE h.id IN (1, 2)
ON CONFLICT (hotel_id, guest_type) DO UPDATE SET
    min_age = EXCLUDED.min_age,
    max_age = EXCLUDED.max_age;

-- 5. TAX CATEGORY
INSERT INTO tax_categories (category_code, category_name, description)
VALUES ('ROOM', 'Room Rental', 'Dá»‹ch vá»¥ lÆ°u trÃº')
ON CONFLICT (category_code) DO NOTHING;

-- 6. HOTEL ROOM TYPES
INSERT INTO hotel_room_types (
    id, hotel_id, room_type_id, tax_category_id,
    standard_adults, standard_children,
    max_adults, max_children, max_infants, max_total_guests,
    max_beds, extra_beds,
    base_price, total_quantity, status
)
OVERRIDING SYSTEM VALUE
VALUES
    -- Hotel 1 (Hanoi)
    (1, 1, (SELECT id FROM room_types WHERE code = 'STD'), (SELECT id FROM tax_categories WHERE category_code = 'ROOM'),
     2, 0, 2, 1, 1, 3, 1, 1, 1000000.00, 10, 'ACTIVE'),
    (2, 1, (SELECT id FROM room_types WHERE code = 'DLX'), (SELECT id FROM tax_categories WHERE category_code = 'ROOM'),
     2, 0, 3, 2, 1, 4, 1, 1, 2000000.00, 10, 'ACTIVE'),
    (3, 1, (SELECT id FROM room_types WHERE code = 'STE'), (SELECT id FROM tax_categories WHERE category_code = 'ROOM'),
     2, 0, 4, 2, 1, 5, 1, 1, 3500000.00, 5, 'ACTIVE'),
    -- Hotel 2 (Da Nang)
    (4, 2, (SELECT id FROM room_types WHERE code = 'SUP'), (SELECT id FROM tax_categories WHERE category_code = 'ROOM'),
     2, 0, 2, 1, 1, 3, 1, 1, 1200000.00, 10, 'ACTIVE'),
    (5, 2, (SELECT id FROM room_types WHERE code = 'DLX'), (SELECT id FROM tax_categories WHERE category_code = 'ROOM'),
     2, 0, 3, 2, 1, 4, 2, 0, 2200000.00, 10, 'ACTIVE')
ON CONFLICT (id) DO UPDATE SET
    base_price = EXCLUDED.base_price,
    total_quantity = EXCLUDED.total_quantity,
    status = EXCLUDED.status;

SELECT setval('hotel_room_types_id_seq', (SELECT GREATEST(MAX(id), 5) FROM hotel_room_types));

-- 7. HOTEL ROOM TYPE FEATURES
INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id
FROM hotel_room_types hrt
JOIN room_features rf ON rf.code IN ('FREE_WIFI', 'AIR_CONDITIONING', 'SMART_TV', 'SHOWER')
WHERE hrt.id IN (1, 2, 3, 4, 5)
ON CONFLICT DO NOTHING;

INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hrt.id, rf.id
FROM hotel_room_types hrt
JOIN room_features rf ON rf.code IN ('BATHTUB', 'CITY_VIEW', 'BALCONY')
WHERE hrt.id IN (2, 3, 5)
ON CONFLICT DO NOTHING;

-- 8. ROOM INSTANCES (Physical rooms)
INSERT INTO room_instances (hotel_id, hotel_room_type_id, room_number, current_status)
VALUES
    (1, 1, '101', 'READY'),
    (1, 1, '102', 'READY'),
    (1, 2, '201', 'READY'),
    (1, 2, '202', 'READY'),
    (1, 3, '301', 'READY'),
    (2, 4, '101', 'READY'),
    (2, 5, '201', 'READY')
ON CONFLICT (hotel_id, room_number) DO UPDATE SET
    current_status = EXCLUDED.current_status;

-- 9. NIGHTLY ROOM AVAILABILITY (60 days from CURRENT_DATE)
INSERT INTO room_availability (hotel_room_type_id, date, total_rooms, booked_rooms, locked_rooms, ooo_rooms, version)
SELECT
    hrt.id,
    CURRENT_DATE + s.i,
    hrt.total_quantity,
    0,
    0,
    0,
    0
FROM hotel_room_types hrt
CROSS JOIN generate_series(0, 59) AS s(i)
WHERE hrt.id IN (1, 2, 3, 4, 5)
ON CONFLICT (hotel_room_type_id, date) DO UPDATE SET
    total_rooms = EXCLUDED.total_rooms,
    booked_rooms = 0,
    locked_rooms = 0,
    ooo_rooms = 0;

-- 10. STAFFS MAPPED TO KEYCLOAK USERS
INSERT INTO staffs (
    id, keycloak_id, role_id, scope_type, scope_entity_id, department_id,
    username, email, phone, first_name, last_name, full_name, status
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '20000000-0000-0000-0000-000000000001', (SELECT id FROM roles WHERE code = 'CHAIN_ADMIN'),
     'CHAIN', NULL, NULL,
     'admin', 'admin@hotel.utc.edu.vn', '0901000001', 'Super', 'Admin', 'Super Admin', 'ACTIVE'),
    (2, '20000000-0000-0000-0000-000000000002', (SELECT id FROM roles WHERE code = 'REGION_MANAGER'),
     'REGION', (SELECT id FROM regions WHERE code = 'NORTH'), NULL,
     'region_mgr', 'region@hotel.utc.edu.vn', '0901000002', 'Region', 'Manager', 'Region Manager', 'ACTIVE'),
    (3, '20000000-0000-0000-0000-000000000003', (SELECT id FROM roles WHERE code = 'PROPERTY_MANAGER'),
     'PROPERTY', 1, NULL,
     'manager_h1', 'manager.h1@hotel.utc.edu.vn', '0901000003', 'Manager', 'Hotel 1', 'Manager Hotel 1', 'ACTIVE'),
    (4, '20000000-0000-0000-0000-000000000004', (SELECT id FROM roles WHERE code = 'PROPERTY_MANAGER'),
     'PROPERTY', 2, NULL,
     'manager_h2', 'manager.h2@hotel.utc.edu.vn', '0901000004', 'Manager', 'Hotel 2', 'Manager Hotel 2', 'ACTIVE'),
    (5, '20000000-0000-0000-0000-000000000005', (SELECT id FROM roles WHERE code = 'RECEPTIONIST'),
     'PROPERTY', 1, (SELECT id FROM departments WHERE code = 'FRONT_OFFICE'),
     'reception_h1', 'reception.h1@hotel.utc.edu.vn', '0901000005', 'Reception', 'Hotel 1', 'Reception Hotel 1', 'ACTIVE')
ON CONFLICT (keycloak_id) DO UPDATE SET
    role_id = EXCLUDED.role_id,
    scope_type = EXCLUDED.scope_type,
    scope_entity_id = EXCLUDED.scope_entity_id,
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    status = EXCLUDED.status;

SELECT setval('staffs_id_seq', (SELECT GREATEST(MAX(id), 5) FROM staffs));

-- 11. UTILITY METERS & READINGS (Task T04)
INSERT INTO utility_meters (id, hotel_id, meter_code, meter_type, location_label, is_deleted)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 'ELEC-H1-01', 'ELECTRICITY', 'Main Power Room Hanoi', FALSE),
    (2, 1, 'WAT-H1-01', 'WATER', 'Main Water Pump Hanoi', FALSE),
    (3, 2, 'ELEC-H2-01', 'ELECTRICITY', 'Main Power Room Danang', FALSE)
ON CONFLICT (id) DO UPDATE SET
    meter_code = EXCLUDED.meter_code,
    meter_type = EXCLUDED.meter_type;

SELECT setval('utility_meters_id_seq', (SELECT GREATEST(MAX(id), 3) FROM utility_meters));

INSERT INTO utility_readings (meter_id, reading_date, reading_value, is_meter_reset, recorded_by, is_deleted)
VALUES
    (1, CURRENT_DATE - 1, 1500.500, FALSE, 3, FALSE),
    (1, CURRENT_DATE, 1580.200, FALSE, 3, FALSE),
    (2, CURRENT_DATE - 1, 300.000, FALSE, 3, FALSE),
    (2, CURRENT_DATE, 315.500, FALSE, 3, FALSE)
ON CONFLICT (meter_id, reading_date) WHERE is_deleted = FALSE DO UPDATE SET
    reading_value = EXCLUDED.reading_value;

-- 12. PAYROLL SUMMARY (Task T05)
INSERT INTO payroll_summary (staff_id, period_month, period_year, base_salary, bonus, deductions, net_salary, is_deleted)
VALUES
    (3, EXTRACT(MONTH FROM CURRENT_DATE - INTERVAL '1 month')::INT, EXTRACT(YEAR FROM CURRENT_DATE - INTERVAL '1 month')::INT, 15000000.00, 2000000.00, 500000.00, 16500000.00, FALSE),
    (5, EXTRACT(MONTH FROM CURRENT_DATE - INTERVAL '1 month')::INT, EXTRACT(YEAR FROM CURRENT_DATE - INTERVAL '1 month')::INT, 8000000.00, 1000000.00, 200000.00, 8800000.00, FALSE)
ON CONFLICT DO NOTHING;

-- 13. GUESTS, BOOKINGS & STAY GUESTS (Task T05 / Lodging)
INSERT INTO booking_guests (id, public_id, phone, email, identity_type, identity_number, nationality)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '30000000-0000-0000-0000-000000000001', '0912345678', 'guest1@acceptance.utc.edu.vn', 'CCCD', '001090012345', 'Vietnam')
ON CONFLICT (id) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('booking_guests', 'id'),
    (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM booking_guests)
);

INSERT INTO bookings (id, hotel_id, guest_id, booking_type, booking_number, subtotal_amount, total_amount, status)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 1, 'FIT', 'BK-ACCEPT-001', 2000000.00, 2100000.00, 'CONFIRMED')
ON CONFLICT (id) DO NOTHING;

SELECT setval('bookings_id_seq', (SELECT GREATEST(MAX(id), 1) FROM bookings));

INSERT INTO booking_details (id, booking_id, hotel_room_type_id, room_type_name, quantity, check_in_date, check_out_date)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 1, 'Standard Room (PhÃ²ng TiÃªu chuáº©n)', 1, CURRENT_DATE - 1, CURRENT_DATE + 2)
ON CONFLICT (id) DO NOTHING;

SELECT setval('booking_details_id_seq', (SELECT GREATEST(MAX(id), 1) FROM booking_details));

INSERT INTO booking_rooms (id, booking_detail_id, room_instance_id, adult_count, child_count, guest_count, status)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, (SELECT id FROM room_instances WHERE hotel_id = 1 AND room_number = '101' LIMIT 1), 2, 0, 2, 'CHECKED_IN')
ON CONFLICT (id) DO NOTHING;

SELECT setval('booking_rooms_id_seq', (SELECT GREATEST(MAX(id), 1) FROM booking_rooms));

INSERT INTO stay_guests (
    id, booking_room_id, hotel_id, room_number, first_name, last_name, full_name, date_of_birth, gender,
    guest_type, identity_type, identity_number, nationality, document_type, document_number, permanent_address,
    check_in_time, expected_check_out_time, is_deleted
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 1, '101', 'Nguyen', 'Van A', 'Nguyen Van A', '1990-05-15', 'MALE',
     'ADULT', 'CCCD', '001090012345', 'Vietnam', 'CCCD', '001090012345', 'Ha Noi, Viet Nam',
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '2 days', FALSE)
ON CONFLICT (id) DO UPDATE SET
    booking_room_id = EXCLUDED.booking_room_id,
    hotel_id = EXCLUDED.hotel_id,
    room_number = EXCLUDED.room_number,
    full_name = EXCLUDED.full_name,
    date_of_birth = EXCLUDED.date_of_birth,
    gender = EXCLUDED.gender,
    nationality = EXCLUDED.nationality,
    document_type = EXCLUDED.document_type,
    document_number = EXCLUDED.document_number,
    permanent_address = EXCLUDED.permanent_address,
    check_in_time = EXCLUDED.check_in_time,
    expected_check_out_time = EXCLUDED.expected_check_out_time,
    is_deleted = FALSE;

SELECT setval(pg_get_serial_sequence('stay_guests', 'id'), (SELECT GREATEST(MAX(id), 1) FROM stay_guests));

INSERT INTO lodging_queues (
    id, stay_guest_id, hotel_id, status, is_deleted
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 1, 'PENDING', FALSE)
ON CONFLICT (id) DO UPDATE SET
    stay_guest_id = EXCLUDED.stay_guest_id,
    hotel_id = EXCLUDED.hotel_id,
    status = 'PENDING',
    error_message = NULL,
    exported_at = NULL,
    batch_reference = NULL,
    is_deleted = FALSE;

SELECT setval(pg_get_serial_sequence('lodging_queues', 'id'), (SELECT GREATEST(MAX(id), 1) FROM lodging_queues));

-- =============================================================================
-- 15. ADVISOR READ-ONLY DATABASE USER (T12 ZERO BUSINESS WRITES ENFORCEMENT)
-- =============================================================================
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'advisor_readonly') THEN
    CREATE ROLE advisor_readonly WITH LOGIN PASSWORD 'AdvisorReadOnly123!';
  END IF;
END
$$;

GRANT CONNECT ON DATABASE hotel_acceptance_db TO advisor_readonly;
GRANT USAGE ON SCHEMA public TO advisor_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO advisor_readonly;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON ALL TABLES IN SCHEMA public FROM advisor_readonly;
