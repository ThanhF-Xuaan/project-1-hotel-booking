-- Run only against the local development DB. Send this file as UTF-8 bytes
-- (docker cp then psql -f, or binary stdin). Preview rolls back by default.
-- Pass -v apply_t05_local_repair=on to commit after reviewing the counts.
\set ON_ERROR_STOP on
\if :{?apply_t05_local_repair}
\else
  \set apply_t05_local_repair off
\endif

BEGIN;
SET LOCAL client_encoding = 'UTF8';

-- Match exactly the observed replacement of each non-ASCII UTF-8 byte by '?'.
CREATE FUNCTION pg_temp.damaged(value text) RETURNS text
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT regexp_replace(encode(convert_to(value, 'UTF8'), 'escape'), '\\[0-7]{3}', '?', 'g')
$$;

CREATE FUNCTION pg_temp.restore_seed_text(stored text, original text) RETURNS text
LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN stored = pg_temp.damaged(original) THEN original ELSE stored END
$$;

WITH fixture(booking_number, room_number, dob, first_name, last_name, full_name,
             permanent_address, current_address, reason_for_stay) AS (
    VALUES
        ('BKG-20261103-0001', '101', DATE '1990-08-15', 'Văn An', 'Nguyễn', 'Nguyễn Văn An',
         'Số 12 Chùa Bộc, Đống Đa, Hà Nội', 'Hà Nội', 'Công tác'),
        ('BKG-20261104-0002', '201', DATE '1995-04-20', 'Thị Mai', 'Trần', 'Trần Thị Mai',
         'Phường Máy Tơ, Ngô Quyền, Hải Phòng', 'Hải Phòng', 'Nghỉ dưỡng'),
        ('BKG-20261104-0002', '201', DATE '2018-05-10', 'Hoàng Nam', 'Lê', 'Lê Hoàng Nam',
         'Phường Máy Tơ, Ngô Quyền, Hải Phòng', 'Hải Phòng', 'Nghỉ dưỡng'),
        ('BKG-20261015-0003', '102', DATE '1990-08-15', 'Văn An', 'Nguyễn', 'Nguyễn Văn An',
         'Số 12 Chùa Bộc, Đống Đa, Hà Nội', 'Hà Nội', 'Du lịch')
), restored AS (
    SELECT sg.id,
           pg_temp.restore_seed_text(sg.first_name, f.first_name) AS first_name,
           pg_temp.restore_seed_text(sg.last_name, f.last_name) AS last_name,
           pg_temp.restore_seed_text(sg.full_name, f.full_name) AS full_name,
           pg_temp.restore_seed_text(sg.nationality, 'Việt Nam') AS nationality,
           pg_temp.restore_seed_text(sg.permanent_address, f.permanent_address) AS permanent_address,
           pg_temp.restore_seed_text(sg.current_address, f.current_address) AS current_address,
           pg_temp.restore_seed_text(sg.reason_for_stay, f.reason_for_stay) AS reason_for_stay
    FROM fixture f
    JOIN public.bookings b ON b.booking_number = f.booking_number
    JOIN public.stay_guests sg ON sg.booking_id = b.id AND sg.hotel_id = b.hotel_id
        AND sg.room_number = f.room_number AND sg.date_of_birth = f.dob
        AND sg.full_name IN (f.full_name, pg_temp.damaged(f.full_name))
    WHERE NOT sg.is_deleted
    FOR UPDATE OF sg
), repaired AS (
    UPDATE public.stay_guests sg
    SET first_name = r.first_name, last_name = r.last_name, full_name = r.full_name,
        nationality = r.nationality, permanent_address = r.permanent_address,
        current_address = r.current_address, reason_for_stay = r.reason_for_stay
    FROM restored r
    WHERE sg.id = r.id
      AND (sg.first_name, sg.last_name, sg.full_name, sg.nationality, sg.permanent_address,
           sg.current_address, sg.reason_for_stay)
          IS DISTINCT FROM
          (r.first_name, r.last_name, r.full_name, r.nationality, r.permanent_address,
           r.current_address, r.reason_for_stay)
    RETURNING 1
)
SELECT 'stay_guests' AS repaired_table, count(*) AS repaired_rows FROM repaired;

WITH fixture(booking_number, email, full_name, nationality) AS (
    VALUES
        ('BKG-20261103-0001', 'an.nguyen@example.com', 'Nguyễn Văn An', 'Việt Nam'),
        ('BKG-20261104-0002', 'mai.tran@example.com', 'Trần Thị Mai', 'Việt Nam'),
        ('BKG-20261110-0004', 'mbrown@example.com', 'Michael Brown', 'Hoa Kỳ')
), repaired AS (
    UPDATE public.booking_guests g
    SET full_name = pg_temp.restore_seed_text(g.full_name, f.full_name),
        nationality = pg_temp.restore_seed_text(g.nationality, f.nationality)
    FROM fixture f JOIN public.bookings b ON b.booking_number = f.booking_number
    WHERE b.guest_id = g.id AND g.email = f.email
      AND (g.full_name = pg_temp.damaged(f.full_name) AND g.full_name <> f.full_name
        OR g.nationality = pg_temp.damaged(f.nationality) AND g.nationality <> f.nationality)
    RETURNING 1
)
SELECT 'booking_guests' AS repaired_table, count(*) AS repaired_rows FROM repaired;

WITH fixture(booking_number, notes, room_type_name) AS (
    VALUES
        ('BKG-20261103-0001', 'Khách VIP, nhận phòng sớm', 'Deluxe Room (Phòng Sang trọng)'),
        ('BKG-20261104-0002', 'Yêu cầu phòng tầng cao, có trẻ nhỏ', 'Suite (Phòng Căn hộ/Thương gia)'),
        ('BKG-20261015-0003', 'Lưu trú đã hoàn thành', 'Deluxe Room (Phòng Sang trọng)'),
        ('BKG-20261110-0004', 'Khách quốc tế, yêu cầu phòng view biển yên tĩnh', 'Superior Room (Phòng Cao cấp)')
), repaired_bookings AS (
    UPDATE public.bookings b SET notes = f.notes
    FROM fixture f
    WHERE b.booking_number = f.booking_number AND b.notes = pg_temp.damaged(f.notes)
    RETURNING 1
), repaired_details AS (
    UPDATE public.booking_details d SET room_type_name = f.room_type_name
    FROM fixture f JOIN public.bookings b ON b.booking_number = f.booking_number
    WHERE d.booking_id = b.id AND d.room_type_name = pg_temp.damaged(f.room_type_name)
    RETURNING 1
)
SELECT 'bookings' AS repaired_table, count(*) AS repaired_rows FROM repaired_bookings
UNION ALL
SELECT 'booking_details', count(*) FROM repaired_details;

\if :apply_t05_local_repair
  COMMIT;
\else
  ROLLBACK;
\endif
