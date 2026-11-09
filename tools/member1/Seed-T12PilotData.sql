-- T12 pilot data for the isolated Member 1 database only.
-- Room names/capacities are reference-backed; quantities, base prices and daily
-- inventory are intentionally authored pilot values. They are not production facts.

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM hotels
        WHERE id = 4 AND status = 'ACTIVE' AND is_deleted = FALSE
    ) THEN
        RAISE EXCEPTION 'T12 pilot hotel 4 is missing or inactive';
    END IF;
END $$;

UPDATE hotels
SET name = 'Vinpearl Resort & Spa Hạ Long (dữ liệu pilot DB)',
    address = 'Đảo Rều, Bãi Cháy, Hạ Long, Quảng Ninh',
    check_in_time = TIME '14:00:00',
    check_out_time = TIME '12:00:00',
    service_fee_percent = 5.00,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 4;

INSERT INTO room_types (code, name, status, is_deleted)
SELECT v.code, v.name, 'ACTIVE', FALSE
FROM (VALUES
    ('HL-DLX-K', 'Deluxe King hướng vườn'),
    ('HL-DLX-O', 'Deluxe Twin hướng biển'),
    ('HL-EXS', 'Executive Suite King'),
    ('HL-FAM', 'Family Suite')
) AS v(code, name)
WHERE NOT EXISTS (
    SELECT 1 FROM room_types existing
    WHERE existing.code = v.code AND existing.is_deleted = FALSE
);

UPDATE room_types target
SET name = source.name,
    status = 'ACTIVE',
    is_deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP
FROM (VALUES
    ('HL-DLX-K', 'Deluxe King hướng vườn'),
    ('HL-DLX-O', 'Deluxe Twin hướng biển'),
    ('HL-EXS', 'Executive Suite King'),
    ('HL-FAM', 'Family Suite')
) AS source(code, name)
WHERE target.code = source.code;

INSERT INTO hotel_room_types (
    hotel_id, room_type_id, tax_category_id,
    standard_adults, standard_children,
    max_adults, max_children, max_infants, max_total_guests,
    max_beds, extra_beds, base_price, total_quantity, status, is_deleted
)
SELECT
    4, room_type.id, tax_category.id,
    pilot.standard_adults, pilot.standard_children,
    pilot.max_adults, pilot.max_children, pilot.max_infants, pilot.max_total_guests,
    pilot.max_beds, pilot.extra_beds, pilot.base_price, pilot.total_quantity,
    'ACTIVE', FALSE
FROM (VALUES
    ('HL-DLX-K', 2::smallint, 0::smallint, 2::smallint, 2::smallint, 1::smallint, 4::smallint, 1::smallint, 1::smallint, 2920000.00::numeric, 160),
    ('HL-DLX-O', 2::smallint, 0::smallint, 2::smallint, 2::smallint, 1::smallint, 4::smallint, 2::smallint, 1::smallint, 3800000.00::numeric, 160),
    ('HL-EXS', 2::smallint, 0::smallint, 2::smallint, 2::smallint, 1::smallint, 4::smallint, 1::smallint, 1::smallint, 5200000.00::numeric, 48),
    ('HL-FAM', 4::smallint, 0::smallint, 4::smallint, 2::smallint, 1::smallint, 4::smallint, 2::smallint, 0::smallint, 6630000.00::numeric, 16)
) AS pilot(
    code, standard_adults, standard_children, max_adults, max_children,
    max_infants, max_total_guests, max_beds, extra_beds, base_price, total_quantity
)
JOIN room_types room_type ON room_type.code = pilot.code AND room_type.is_deleted = FALSE
CROSS JOIN LATERAL (
    SELECT id FROM tax_categories
    WHERE category_code = 'ROOM' AND status = 'ACTIVE' AND is_deleted = FALSE
    ORDER BY id LIMIT 1
) tax_category
ON CONFLICT (hotel_id, room_type_id) WHERE is_deleted = FALSE
DO UPDATE SET
    tax_category_id = EXCLUDED.tax_category_id,
    standard_adults = EXCLUDED.standard_adults,
    standard_children = EXCLUDED.standard_children,
    max_adults = EXCLUDED.max_adults,
    max_children = EXCLUDED.max_children,
    max_infants = EXCLUDED.max_infants,
    max_total_guests = EXCLUDED.max_total_guests,
    max_beds = EXCLUDED.max_beds,
    extra_beds = EXCLUDED.extra_beds,
    base_price = EXCLUDED.base_price,
    total_quantity = EXCLUDED.total_quantity,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO hotel_room_type_features (hotel_room_type_id, room_feature_id)
SELECT hotel_room_type.id, feature.id
FROM hotel_room_types hotel_room_type
JOIN room_types room_type ON room_type.id = hotel_room_type.room_type_id
JOIN (VALUES
    ('HL-DLX-K', 'GARDEN_VIEW'), ('HL-DLX-K', 'BALCONY'),
    ('HL-DLX-K', 'BATHTUB'), ('HL-DLX-K', 'FREE_WIFI'),
    ('HL-DLX-O', 'SEA_VIEW'), ('HL-DLX-O', 'BALCONY'),
    ('HL-DLX-O', 'BATHTUB'), ('HL-DLX-O', 'FREE_WIFI'),
    ('HL-EXS', 'SEA_VIEW'), ('HL-EXS', 'BALCONY'),
    ('HL-EXS', 'BATHTUB'), ('HL-EXS', 'FREE_WIFI'),
    ('HL-EXS', 'SMART_TV'),
    ('HL-FAM', 'SEA_VIEW'), ('HL-FAM', 'BALCONY'),
    ('HL-FAM', 'BATHTUB'), ('HL-FAM', 'FREE_WIFI'),
    ('HL-FAM', 'SMART_TV')
) AS mapping(room_type_code, feature_code) ON mapping.room_type_code = room_type.code
JOIN room_features feature ON feature.code = mapping.feature_code
WHERE hotel_room_type.hotel_id = 4
  AND hotel_room_type.status = 'ACTIVE'
  AND hotel_room_type.is_deleted = FALSE
ON CONFLICT DO NOTHING;

INSERT INTO room_availability (
    hotel_room_type_id, date, total_rooms, booked_rooms,
    locked_rooms, ooo_rooms, version, created_at, updated_at
)
SELECT
    hotel_room_type.id,
    day::date,
    hotel_room_type.total_quantity,
    CASE room_type.code
        WHEN 'HL-DLX-K' THEN 40 + (EXTRACT(DOY FROM day)::int % 10)
        WHEN 'HL-DLX-O' THEN 70 + (EXTRACT(DOY FROM day)::int % 8)
        WHEN 'HL-EXS' THEN 30 + (EXTRACT(DOY FROM day)::int % 5)
        WHEN 'HL-FAM' THEN 10 + (EXTRACT(DOY FROM day)::int % 3)
    END,
    CASE room_type.code WHEN 'HL-FAM' THEN 1 ELSE 2 END,
    CASE room_type.code WHEN 'HL-DLX-K' THEN 3 WHEN 'HL-DLX-O' THEN 4 ELSE 1 END,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM hotel_room_types hotel_room_type
JOIN room_types room_type ON room_type.id = hotel_room_type.room_type_id
CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + INTERVAL '364 days', INTERVAL '1 day') day
WHERE hotel_room_type.hotel_id = 4
  AND room_type.code IN ('HL-DLX-K', 'HL-DLX-O', 'HL-EXS', 'HL-FAM')
  AND hotel_room_type.status = 'ACTIVE'
  AND hotel_room_type.is_deleted = FALSE
ON CONFLICT (hotel_room_type_id, date)
DO UPDATE SET
    total_rooms = EXCLUDED.total_rooms,
    booked_rooms = EXCLUDED.booked_rooms,
    locked_rooms = EXCLUDED.locked_rooms,
    ooo_rooms = EXCLUDED.ooo_rooms,
    version = room_availability.version + 1,
    updated_at = CURRENT_TIMESTAMP;

DO $$
DECLARE
    room_type_count integer;
    inventory_count integer;
    quantity_sum integer;
    minimum_available integer;
BEGIN
    SELECT count(*), sum(total_quantity)
    INTO room_type_count, quantity_sum
    FROM hotel_room_types hotel_room_type
    JOIN room_types room_type ON room_type.id = hotel_room_type.room_type_id
    WHERE hotel_room_type.hotel_id = 4
      AND room_type.code IN ('HL-DLX-K', 'HL-DLX-O', 'HL-EXS', 'HL-FAM')
      AND hotel_room_type.status = 'ACTIVE'
      AND hotel_room_type.is_deleted = FALSE;

    SELECT count(*), min(availability.available_count)
    INTO inventory_count, minimum_available
    FROM room_availability availability
    JOIN hotel_room_types hotel_room_type ON hotel_room_type.id = availability.hotel_room_type_id
    JOIN room_types room_type ON room_type.id = hotel_room_type.room_type_id
    WHERE hotel_room_type.hotel_id = 4
      AND room_type.code IN ('HL-DLX-K', 'HL-DLX-O', 'HL-EXS', 'HL-FAM')
      AND availability.date BETWEEN CURRENT_DATE AND CURRENT_DATE + 364;

    IF room_type_count <> 4 OR quantity_sum <> 384
       OR inventory_count <> 1460 OR minimum_available < 1 THEN
        RAISE EXCEPTION 'Invalid T12 pilot catalog or availability';
    END IF;
END $$;

COMMIT;
