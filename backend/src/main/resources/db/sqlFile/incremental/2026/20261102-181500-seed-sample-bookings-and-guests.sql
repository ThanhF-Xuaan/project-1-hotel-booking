-- =========================================================================
-- MIGRATION: SEED DỮ LIỆU MẪU ĐƠN ĐẶT PHÒNG & KHÁCH HÀNG CHO CÁC PHÒNG CÓ SẴN
-- Môi trường: dev-seed (Local / Dev / Staging)
-- Khách sạn & Phòng có sẵn:
--   1. Viettel Luxury Hà Nội (hotel_id = 1):   Phòng 101C (DLX), 201D (STE), 301E (EXE)
--   2. Viettel Grand Đà Nẵng (hotel_id = 2):   Phòng 101B (SUP), 201C (DLX)
--   3. Viettel Boutique Sapa (hotel_id = 3):   Phòng 101A (STD), 102A (STD)
-- =========================================================================

DO $$
DECLARE
    v_hotel_hn_id SMALLINT;
    v_hotel_dn_id SMALLINT;
    v_hotel_sp_id SMALLINT;

    v_hrt_hn_dlx INT;
    v_hrt_hn_ste INT;
    v_hrt_hn_exe INT;
    v_hrt_dn_sup INT;
    v_hrt_dn_dlx INT;
    v_hrt_sp_std INT;

    v_room_101c_id INT;
    v_room_201d_id INT;
    v_room_301e_id INT;
    v_room_101b_id INT;
    v_room_201c_id INT;
    v_room_101a_id INT;
    v_room_102a_id INT;

    v_guest_an_id BIGINT;
    v_guest_nam_id BIGINT;
    v_guest_tung_id BIGINT;

    v_booking_hn_id BIGINT;
    v_booking_dn_id BIGINT;
    v_booking_sp_id BIGINT;

    v_bdetail_hn_dlx_id BIGINT;
    v_bdetail_hn_ste_id BIGINT;
    v_bdetail_hn_exe_id BIGINT;
    v_bdetail_dn_sup_id BIGINT;
    v_bdetail_dn_dlx_id BIGINT;
    v_bdetail_sp_std_id BIGINT;

    v_broom_101c_id BIGINT;
    v_broom_201d_id BIGINT;
    v_broom_301e_id BIGINT;
    v_broom_101b_id BIGINT;
    v_broom_201c_id BIGINT;
    v_broom_101a_id BIGINT;
    v_broom_102a_id BIGINT;

    v_inv_hn_id BIGINT;
    v_inv_dn_id BIGINT;
    v_inv_sp_id BIGINT;
BEGIN
    -- 1. LẤY ID CÁC KHÁCH SẠN CƠ SỞ (ID 1: Hà Nội, ID 2: Đà Nẵng, ID 3: Sapa)
    SELECT id INTO v_hotel_hn_id FROM hotels WHERE id = 1;
    SELECT id INTO v_hotel_dn_id FROM hotels WHERE id = 2;
    SELECT id INTO v_hotel_sp_id FROM hotels WHERE id = 3;

    IF v_hotel_hn_id IS NULL OR v_hotel_dn_id IS NULL OR v_hotel_sp_id IS NULL THEN
        RAISE NOTICE 'Chưa tìm thấy đủ 3 khách sạn cơ sở trong DB, bỏ qua seed bookings.';
        RETURN;
    END IF;

    -- 2. LẤY ID CÁC HẠNG PHÒNG TƯƠNG ỨNG
    SELECT hrt.id INTO v_hrt_hn_dlx FROM hotel_room_types hrt JOIN room_types rt ON rt.id = hrt.room_type_id WHERE hrt.hotel_id = v_hotel_hn_id AND rt.code = 'DLX' LIMIT 1;
    SELECT hrt.id INTO v_hrt_hn_ste FROM hotel_room_types hrt JOIN room_types rt ON rt.id = hrt.room_type_id WHERE hrt.hotel_id = v_hotel_hn_id AND rt.code = 'STE' LIMIT 1;
    SELECT hrt.id INTO v_hrt_hn_exe FROM hotel_room_types hrt JOIN room_types rt ON rt.id = hrt.room_type_id WHERE hrt.hotel_id = v_hotel_hn_id AND rt.code = 'EXE' LIMIT 1;

    SELECT hrt.id INTO v_hrt_dn_sup FROM hotel_room_types hrt JOIN room_types rt ON rt.id = hrt.room_type_id WHERE hrt.hotel_id = v_hotel_dn_id AND rt.code = 'SUP' LIMIT 1;
    SELECT hrt.id INTO v_hrt_dn_dlx FROM hotel_room_types hrt JOIN room_types rt ON rt.id = hrt.room_type_id WHERE hrt.hotel_id = v_hotel_dn_id AND rt.code = 'DLX' LIMIT 1;

    SELECT hrt.id INTO v_hrt_sp_std FROM hotel_room_types hrt JOIN room_types rt ON rt.id = hrt.room_type_id WHERE hrt.hotel_id = v_hotel_sp_id AND rt.code = 'STD' LIMIT 1;

    -- 3. LẤY ID CÁC PHÒNG VẬT LÝ CÓ SẴN (THEO SỐ PHÒNG HẬU TỐ)
    SELECT id INTO v_room_101c_id FROM room_instances WHERE hotel_id = v_hotel_hn_id AND room_number = '101C' LIMIT 1;
    SELECT id INTO v_room_201d_id FROM room_instances WHERE hotel_id = v_hotel_hn_id AND room_number = '201D' LIMIT 1;
    SELECT id INTO v_room_301e_id FROM room_instances WHERE hotel_id = v_hotel_hn_id AND room_number = '301E' LIMIT 1;

    SELECT id INTO v_room_101b_id FROM room_instances WHERE hotel_id = v_hotel_dn_id AND room_number = '101B' LIMIT 1;
    SELECT id INTO v_room_201c_id FROM room_instances WHERE hotel_id = v_hotel_dn_id AND room_number = '201C' LIMIT 1;

    SELECT id INTO v_room_101a_id FROM room_instances WHERE hotel_id = v_hotel_sp_id AND room_number = '101A' LIMIT 1;
    SELECT id INTO v_room_102a_id FROM room_instances WHERE hotel_id = v_hotel_sp_id AND room_number = '102A' LIMIT 1;

    -- =========================================================================
    -- BƯỚC 1: TẠO HỒ SƠ KHÁCH HÀNG ĐẠI DIỆN ĐẶT PHÒNG (booking_guests)
    -- =========================================================================
    SELECT id INTO v_guest_an_id FROM booking_guests WHERE phone = '0901234501' LIMIT 1;
    IF v_guest_an_id IS NULL THEN
        INSERT INTO booking_guests (full_name, phone, email, identity_type, identity_number, nationality, birth_date, status)
        VALUES ('Nguyễn Văn An', '0901234501', 'an.nguyen@example.com', 'CCCD', '001088001234', 'Việt Nam', '1988-03-12', 'ACTIVE')
        RETURNING id INTO v_guest_an_id;
    END IF;

    SELECT id INTO v_guest_nam_id FROM booking_guests WHERE phone = '0901234502' LIMIT 1;
    IF v_guest_nam_id IS NULL THEN
        INSERT INTO booking_guests (full_name, phone, email, identity_type, identity_number, nationality, birth_date, status)
        VALUES ('Đỗ Hải Nam', '0901234502', 'nam.do@example.com', 'CCCD', '001091009012', 'Việt Nam', '1991-06-17', 'ACTIVE')
        RETURNING id INTO v_guest_nam_id;
    END IF;

    SELECT id INTO v_guest_tung_id FROM booking_guests WHERE phone = '0901234503' LIMIT 1;
    IF v_guest_tung_id IS NULL THEN
        INSERT INTO booking_guests (full_name, phone, email, identity_type, identity_number, nationality, birth_date, status)
        VALUES ('Vũ Thanh Tùng', '0901234503', 'tung.vu@example.com', 'CCCD', '001093006789', 'Việt Nam', '1993-04-15', 'ACTIVE')
        RETURNING id INTO v_guest_tung_id;
    END IF;

    -- =========================================================================
    -- BƯỚC 2: TẠO CÁC ĐƠN ĐẶT PHÒNG (bookings)
    -- =========================================================================

    -- Đơn 1: Viettel Luxury Hà Nội (BK-2026-HN001) - 3 phòng Deluxe + Suite + Executive
    IF NOT EXISTS (SELECT 1 FROM bookings WHERE booking_number = 'BK-2026-HN001') THEN
        INSERT INTO bookings (
            hotel_id, guest_id, booking_type, booking_number,
            subtotal_amount, service_fee_rate, service_fee_amount, total_vat_amount, total_amount,
            status, issued_at
        )
        VALUES (
            v_hotel_hn_id, v_guest_an_id, 'FIT', 'BK-2026-HN001',
            25500000.00, 5.00, 1275000.00, 2142000.00, 28917000.00,
            'CONFIRMED', CURRENT_TIMESTAMP
        )
        RETURNING id INTO v_booking_hn_id;

        -- Booking details: 1 Deluxe, 1 Suite, 1 Executive (Lưu trú 3 đêm: hôm nay -> hôm nay + 3)
        INSERT INTO booking_details (booking_id, hotel_room_type_id, room_type_name, quantity, check_in_date, check_out_date)
        VALUES (v_booking_hn_id, v_hrt_hn_dlx, 'Deluxe Room', 1, CURRENT_DATE, CURRENT_DATE + 3)
        RETURNING id INTO v_bdetail_hn_dlx_id;

        INSERT INTO booking_details (booking_id, hotel_room_type_id, room_type_name, quantity, check_in_date, check_out_date)
        VALUES (v_booking_hn_id, v_hrt_hn_ste, 'Suite', 1, CURRENT_DATE, CURRENT_DATE + 3)
        RETURNING id INTO v_bdetail_hn_ste_id;

        INSERT INTO booking_details (booking_id, hotel_room_type_id, room_type_name, quantity, check_in_date, check_out_date)
        VALUES (v_booking_hn_id, v_hrt_hn_exe, 'Executive Suite', 1, CURRENT_DATE, CURRENT_DATE + 3)
        RETURNING id INTO v_bdetail_hn_exe_id;

        -- Gán phòng vật lý (booking_rooms) ở trạng thái CHECKED_IN
        INSERT INTO booking_rooms (booking_detail_id, room_instance_id, adult_count, child_count, guest_count, status, actual_check_in_at)
        VALUES (v_bdetail_hn_dlx_id, v_room_101c_id, 2, 0, 2, 'CHECKED_IN', CURRENT_TIMESTAMP)
        RETURNING id INTO v_broom_101c_id;

        INSERT INTO booking_rooms (booking_detail_id, room_instance_id, adult_count, child_count, guest_count, status, actual_check_in_at)
        VALUES (v_bdetail_hn_ste_id, v_room_201d_id, 2, 0, 2, 'CHECKED_IN', CURRENT_TIMESTAMP)
        RETURNING id INTO v_broom_201d_id;

        INSERT INTO booking_rooms (booking_detail_id, room_instance_id, adult_count, child_count, guest_count, status, actual_check_in_at)
        VALUES (v_bdetail_hn_exe_id, v_room_301e_id, 2, 0, 2, 'CHECKED_IN', CURRENT_TIMESTAMP)
        RETURNING id INTO v_broom_301e_id;

        -- Khách lưu trú thực tế tại phòng (stay_guests)
        -- Phòng 101C:
        INSERT INTO stay_guests (hotel_id, booking_id, booking_room_id, room_id, room_number, first_name, last_name, full_name, date_of_birth, guest_type, gender, nationality, document_type, document_number, check_in_time, expected_check_out_time, reason_for_stay)
        VALUES
        (v_hotel_hn_id, v_booking_hn_id, v_broom_101c_id, v_room_101c_id, '101C', 'An', 'Nguyễn Văn', 'Nguyễn Văn An', '1988-03-12', 'ADULT', 'MALE', 'Việt Nam', 'CCCD', '001088001234', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Công tác'),
        (v_hotel_hn_id, v_booking_hn_id, v_broom_101c_id, v_room_101c_id, '101C', 'Bích', 'Trần Thị', 'Trần Thị Bích', '1990-07-25', 'ADULT', 'FEMALE', 'Việt Nam', 'CCCD', '001190002345', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Du lịch');

        -- Phòng 201D:
        INSERT INTO stay_guests (hotel_id, booking_id, booking_room_id, room_id, room_number, first_name, last_name, full_name, date_of_birth, guest_type, gender, nationality, document_type, document_number, check_in_time, expected_check_out_time, reason_for_stay)
        VALUES
        (v_hotel_hn_id, v_booking_hn_id, v_broom_201d_id, v_room_201d_id, '201D', 'Cường', 'Lê Hoàng', 'Lê Hoàng Cường', '1985-11-05', 'ADULT', 'MALE', 'Việt Nam', 'CCCD', '001085003456', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Hội thảo'),
        (v_hotel_hn_id, v_booking_hn_id, v_broom_201d_id, v_room_201d_id, '201D', 'Dung', 'Phạm Thu', 'Phạm Thu Dung', '1992-09-19', 'ADULT', 'FEMALE', 'Việt Nam', 'CCCD', '001192004567', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Hội thảo');

        -- Phòng 301E:
        INSERT INTO stay_guests (hotel_id, booking_id, booking_room_id, room_id, room_number, first_name, last_name, full_name, date_of_birth, guest_type, gender, nationality, document_type, document_number, check_in_time, expected_check_out_time, reason_for_stay)
        VALUES
        (v_hotel_hn_id, v_booking_hn_id, v_broom_301e_id, v_room_301e_id, '301E', 'Đức', 'Hoàng Minh', 'Hoàng Minh Đức', '1987-01-30', 'ADULT', 'MALE', 'Việt Nam', 'CCCD', '001087005678', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Nghỉ dưỡng VIP'),
        (v_hotel_hn_id, v_booking_hn_id, v_broom_301e_id, v_room_301e_id, '301E', 'Hương', 'Vũ Mai', 'Vũ Mai Hương', '1994-04-14', 'ADULT', 'FEMALE', 'Việt Nam', 'CCCD', '001194006789', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Nghỉ dưỡng VIP');

        -- Hóa đơn Folio cho đơn BK-2026-HN001 (ISSUED để test thu tiền VNPay)
        INSERT INTO invoices (booking_id, invoice_number, sub_total, service_fee_rate, service_fee_amount, vat_amount, grand_total, status, issued_at)
        VALUES (v_booking_hn_id, 'INV-2026-HN001', 25500000.00, 5.00, 1275000.00, 2142000.00, 28917000.00, 'ISSUED', CURRENT_TIMESTAMP)
        RETURNING id INTO v_inv_hn_id;

        INSERT INTO invoice_details (invoice_id, reference_id, line_type, description, quantity, unit_price, subtotal, service_fee_rate, service_fee_amount, vat_rate, vat_amount, total_amount)
        VALUES
        (v_inv_hn_id, v_bdetail_hn_dlx_id, 'ROOM_RATE', 'Tiền phòng Deluxe Room (Phòng 101C - 3 đêm)', 3, 2500000.00, 7500000.00, 5.00, 375000.00, 8.00, 630000.00, 8505000.00),
        (v_inv_hn_id, v_bdetail_hn_ste_id, 'ROOM_RATE', 'Tiền phòng Suite (Phòng 201D - 3 đêm)', 3, 3500000.00, 10500000.00, 5.00, 525000.00, 8.00, 882000.00, 11907000.00),
        (v_inv_hn_id, v_bdetail_hn_exe_id, 'ROOM_RATE', 'Tiền phòng Executive Suite (Phòng 301E - 3 đêm)', 3, 2500000.00, 7500000.00, 5.00, 375000.00, 8.00, 630000.00, 8505000.00);

        -- Đổi trạng thái 3 phòng sang OCCUPIED
        UPDATE room_instances SET current_status = 'OCCUPIED' WHERE id IN (v_room_101c_id, v_room_201d_id, v_room_301e_id);
    END IF;

    -- Đơn 2: Viettel Grand Đà Nẵng (BK-2026-DN001) - 2 phòng Superior + Deluxe
    IF NOT EXISTS (SELECT 1 FROM bookings WHERE booking_number = 'BK-2026-DN001') THEN
        INSERT INTO bookings (
            hotel_id, guest_id, booking_type, booking_number,
            subtotal_amount, service_fee_rate, service_fee_amount, total_vat_amount, total_amount,
            status, issued_at
        )
        VALUES (
            v_hotel_dn_id, v_guest_nam_id, 'FIT', 'BK-2026-DN001',
            9600000.00, 5.00, 480000.00, 806400.00, 10886400.00,
            'CONFIRMED', CURRENT_TIMESTAMP
        )
        RETURNING id INTO v_booking_dn_id;

        INSERT INTO booking_details (booking_id, hotel_room_type_id, room_type_name, quantity, check_in_date, check_out_date)
        VALUES (v_booking_dn_id, v_hrt_dn_sup, 'Superior Room', 1, CURRENT_DATE, CURRENT_DATE + 3)
        RETURNING id INTO v_bdetail_dn_sup_id;

        INSERT INTO booking_details (booking_id, hotel_room_type_id, room_type_name, quantity, check_in_date, check_out_date)
        VALUES (v_booking_dn_id, v_hrt_dn_dlx, 'Deluxe Room', 1, CURRENT_DATE, CURRENT_DATE + 3)
        RETURNING id INTO v_bdetail_dn_dlx_id;

        INSERT INTO booking_rooms (booking_detail_id, room_instance_id, adult_count, child_count, guest_count, status, actual_check_in_at)
        VALUES (v_bdetail_dn_sup_id, v_room_101b_id, 2, 0, 2, 'CHECKED_IN', CURRENT_TIMESTAMP)
        RETURNING id INTO v_broom_101b_id;

        INSERT INTO booking_rooms (booking_detail_id, room_instance_id, adult_count, child_count, guest_count, status, actual_check_in_at)
        VALUES (v_bdetail_dn_dlx_id, v_room_201c_id, 2, 0, 2, 'CHECKED_IN', CURRENT_TIMESTAMP)
        RETURNING id INTO v_broom_201c_id;

        INSERT INTO stay_guests (hotel_id, booking_id, booking_room_id, room_id, room_number, first_name, last_name, full_name, date_of_birth, guest_type, gender, nationality, document_type, document_number, check_in_time, expected_check_out_time, reason_for_stay)
        VALUES
        (v_hotel_dn_id, v_booking_dn_id, v_broom_101b_id, v_room_101b_id, '101B', 'Nam', 'Đỗ Hải', 'Đỗ Hải Nam', '1991-06-17', 'ADULT', 'MALE', 'Việt Nam', 'CCCD', '001091009012', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Du lịch'),
        (v_hotel_dn_id, v_booking_dn_id, v_broom_101b_id, v_room_101b_id, '101B', 'Nga', 'Ngô Phương', 'Ngô Phương Nga', '1996-05-03', 'ADULT', 'FEMALE', 'Việt Nam', 'CCCD', '001196010123', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Du lịch'),
        (v_hotel_dn_id, v_booking_dn_id, v_broom_201c_id, v_room_201c_id, '201C', 'Hùng', 'Phan Văn', 'Phan Văn Hùng', '1989-08-22', 'ADULT', 'MALE', 'Việt Nam', 'CCCD', '001089007890', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Công tác'),
        (v_hotel_dn_id, v_booking_dn_id, v_broom_201c_id, v_room_201c_id, '201C', 'Thảo', 'Lê Thanh', 'Lê Thanh Thảo', '1993-10-10', 'ADULT', 'FEMALE', 'Việt Nam', 'CCCD', '001193008901', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Công tác');

        INSERT INTO invoices (booking_id, invoice_number, sub_total, service_fee_rate, service_fee_amount, vat_amount, grand_total, status, issued_at)
        VALUES (v_booking_dn_id, 'INV-2026-DN001', 9600000.00, 5.00, 480000.00, 806400.00, 10886400.00, 'ISSUED', CURRENT_TIMESTAMP)
        RETURNING id INTO v_inv_dn_id;

        INSERT INTO invoice_details (invoice_id, reference_id, line_type, description, quantity, unit_price, subtotal, service_fee_rate, service_fee_amount, vat_rate, vat_amount, total_amount)
        VALUES
        (v_inv_dn_id, v_bdetail_dn_sup_id, 'ROOM_RATE', 'Tiền phòng Superior Room (Phòng 101B - 3 đêm)', 3, 1500000.00, 4500000.00, 5.00, 225000.00, 8.00, 378000.00, 5103000.00),
        (v_inv_dn_id, v_bdetail_dn_dlx_id, 'ROOM_RATE', 'Tiền phòng Deluxe Room (Phòng 201C - 3 đêm)', 3, 1700000.00, 5100000.00, 5.00, 255000.00, 8.00, 428400.00, 5783400.00);

        UPDATE room_instances SET current_status = 'OCCUPIED' WHERE id IN (v_room_101b_id, v_room_201c_id);
    END IF;

    -- Đơn 3: Viettel Boutique Sa Pa (BK-2026-SP001) - 2 phòng Standard
    IF NOT EXISTS (SELECT 1 FROM bookings WHERE booking_number = 'BK-2026-SP001') THEN
        INSERT INTO bookings (
            hotel_id, guest_id, booking_type, booking_number,
            subtotal_amount, service_fee_rate, service_fee_amount, total_vat_amount, total_amount,
            status, issued_at
        )
        VALUES (
            v_hotel_sp_id, v_guest_tung_id, 'FIT', 'BK-2026-SP001',
            4800000.00, 5.00, 240000.00, 403200.00, 5443200.00,
            'CONFIRMED', CURRENT_TIMESTAMP
        )
        RETURNING id INTO v_booking_sp_id;

        INSERT INTO booking_details (booking_id, hotel_room_type_id, room_type_name, quantity, check_in_date, check_out_date)
        VALUES (v_booking_sp_id, v_hrt_sp_std, 'Standard Room', 2, CURRENT_DATE, CURRENT_DATE + 3)
        RETURNING id INTO v_bdetail_sp_std_id;

        INSERT INTO booking_rooms (booking_detail_id, room_instance_id, adult_count, child_count, guest_count, status, actual_check_in_at)
        VALUES (v_bdetail_sp_std_id, v_room_101a_id, 2, 0, 2, 'CHECKED_IN', CURRENT_TIMESTAMP)
        RETURNING id INTO v_broom_101a_id;

        INSERT INTO booking_rooms (booking_detail_id, room_instance_id, adult_count, child_count, guest_count, status, actual_check_in_at)
        VALUES (v_bdetail_sp_std_id, v_room_102a_id, 2, 0, 2, 'CHECKED_IN', CURRENT_TIMESTAMP)
        RETURNING id INTO v_broom_102a_id;

        INSERT INTO stay_guests (hotel_id, booking_id, booking_room_id, room_id, room_number, first_name, last_name, full_name, date_of_birth, guest_type, gender, nationality, document_type, document_number, check_in_time, expected_check_out_time, reason_for_stay)
        VALUES
        (v_hotel_sp_id, v_booking_sp_id, v_broom_101a_id, v_room_101a_id, '101A', 'Tùng', 'Vũ Thanh', 'Vũ Thanh Tùng', '1993-04-15', 'ADULT', 'MALE', 'Việt Nam', 'CCCD', '001093006789', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Nghỉ dưỡng'),
        (v_hotel_sp_id, v_booking_sp_id, v_broom_101a_id, v_room_101a_id, '101A', 'Linh', 'Nguyễn Thùy', 'Nguyễn Thùy Linh', '1995-12-08', 'ADULT', 'FEMALE', 'Việt Nam', 'CCCD', '001195007890', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Nghỉ dưỡng'),
        (v_hotel_sp_id, v_booking_sp_id, v_broom_102a_id, v_room_102a_id, '102A', 'Anh', 'Đặng Quốc', 'Đặng Quốc Anh', '1991-09-20', 'ADULT', 'MALE', 'Việt Nam', 'CCCD', '001091005678', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Du lịch'),
        (v_hotel_sp_id, v_booking_sp_id, v_broom_102a_id, v_room_102a_id, '102A', 'Hà', 'Bùi Thị', 'Bùi Thị Hà', '1994-02-18', 'ADULT', 'FEMALE', 'Việt Nam', 'CCCD', '001194004567', CURRENT_TIMESTAMP, (CURRENT_DATE + 3)::timestamp with time zone + interval '12 hours', 'Du lịch');

        INSERT INTO invoices (booking_id, invoice_number, sub_total, service_fee_rate, service_fee_amount, vat_amount, grand_total, status, issued_at)
        VALUES (v_booking_sp_id, 'INV-2026-SP001', 4800000.00, 5.00, 240000.00, 403200.00, 5443200.00, 'ISSUED', CURRENT_TIMESTAMP)
        RETURNING id INTO v_inv_sp_id;

        INSERT INTO invoice_details (invoice_id, reference_id, line_type, description, quantity, unit_price, subtotal, service_fee_rate, service_fee_amount, vat_rate, vat_amount, total_amount)
        VALUES
        (v_inv_sp_id, v_bdetail_sp_std_id, 'ROOM_RATE', 'Tiền phòng Standard Room (Phòng 101A & 102A - 3 đêm)', 6, 800000.00, 4800000.00, 5.00, 240000.00, 8.00, 403200.00, 5443200.00);

        UPDATE room_instances SET current_status = 'OCCUPIED' WHERE id IN (v_room_101a_id, v_room_102a_id);
    END IF;

END $$;
