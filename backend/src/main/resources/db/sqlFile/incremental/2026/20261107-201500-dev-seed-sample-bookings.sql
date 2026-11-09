-- ==============================================================================
-- DỮ LIỆU SEED MẪU: BOOKINGS, BOOKING_ROOMS, STAY_GUESTS, LODGING_QUEUES
-- Phục vụ Member 1 (T05 - Khai báo lưu trú, U13 - OCR, U14 - Personalization)
-- Đã sửa số phòng thành 101C, 201D, 102C khớp với room_instances và trigger m1_active_stay_key
-- ==============================================================================

DO $$
DECLARE
    -- Guest IDs
    v_guest_1_id BIGINT;
    v_guest_2_id BIGINT;
    v_guest_3_id BIGINT;

    -- Booking 1 (Hotel 1 - DLX - CHECKED_IN)
    v_b1_id BIGINT;
    v_b1_detail_id BIGINT;
    v_b1_room_id BIGINT;
    v_b1_stay_guest_id BIGINT;

    -- Booking 2 (Hotel 1 - STE - CHECKED_IN, Gia đình)
    v_b2_id BIGINT;
    v_b2_detail_id BIGINT;
    v_b2_room_id BIGINT;
    v_b2_sg1_id BIGINT;
    v_b2_sg2_id BIGINT;

    -- Booking 3 (Hotel 1 - DLX - CHECKED_OUT trong quá khứ, cùng Guest 1)
    v_b3_id BIGINT;
    v_b3_detail_id BIGINT;
    v_b3_room_id BIGINT;
    v_b3_stay_guest_id BIGINT;

    -- Booking 4 (Hotel 2 Đà Nẵng - SUP - EXPECTED sắp đến)
    v_b4_id BIGINT;
    v_b4_detail_id BIGINT;
    v_b4_room_id BIGINT;

BEGIN
    -- Kiểm tra nếu đã có dữ liệu mẫu thì đồng bộ số phòng (nếu cần) và bỏ qua
    IF EXISTS (SELECT 1 FROM bookings WHERE booking_number = 'BKG-20261103-0001') THEN
        UPDATE stay_guests SET room_number = '101C' WHERE room_id = 11 AND room_number = '101';
        UPDATE stay_guests SET room_number = '201D' WHERE room_id = 17 AND room_number = '201';
        UPDATE stay_guests SET room_number = '102C' WHERE room_id = 12 AND room_number = '102';
        RAISE NOTICE 'Dữ liệu booking mẫu đã tồn tại. Đã đồng bộ số phòng khớp room_instances.';
        RETURN;
    END IF;

    -- --------------------------------------------------------------------------
    -- 1. TẠO KHÁCH HÀNG MẪU (booking_guests)
    -- --------------------------------------------------------------------------
    -- Khách 1: Nguyễn Văn An (khách quen, có lịch sử lưu trú để phục vụ U14)
    INSERT INTO booking_guests (full_name, phone, email, identity_type, identity_number, nationality, status)
    VALUES ('Nguyễn Văn An', '0912345678', 'an.nguyen@example.com', 'CCCD', '001090012345', 'Việt Nam', 'ACTIVE')
    RETURNING id INTO v_guest_1_id;

    -- Khách 2: Trần Thị Mai (khách lưu trú gia đình tại Hotel 1)
    INSERT INTO booking_guests (full_name, phone, email, identity_type, identity_number, nationality, status)
    VALUES ('Trần Thị Mai', '0987654321', 'mai.tran@example.com', 'CCCD', '031195009876', 'Việt Nam', 'ACTIVE')
    RETURNING id INTO v_guest_2_id;

    -- Khách 3: Michael Brown (khách quốc tế sắp đến Hotel 2 Đà Nẵng)
    INSERT INTO booking_guests (full_name, phone, email, identity_type, identity_number, nationality, status)
    VALUES ('Michael Brown', '+12025550143', 'mbrown@example.com', 'PASSPORT', 'B9876543', 'Hoa Kỳ', 'ACTIVE')
    RETURNING id INTO v_guest_3_id;

    -- --------------------------------------------------------------------------
    -- 2. BOOKING 1: Khách sạn 1 (Hà Nội) - Phòng 101C (DLX) - Đang lưu trú (CHECKED_IN)
    --    Phục vụ T05: Kiểm thử hàng đợi khai báo lưu trú (PENDING)
    -- --------------------------------------------------------------------------
    INSERT INTO bookings (
        hotel_id, guest_id, booking_type, booking_number,
        subtotal_amount, service_fee_rate, service_fee_amount, total_vat_amount, total_amount,
        status, issued_at, notes
    ) VALUES (
        1, v_guest_1_id, 'FIT', 'BKG-20261103-0001',
        7500000.00, 5.00, 375000.00, 630000.00, 8505000.00,
        'CONFIRMED', '2026-11-03 10:00:00+07', 'Khách VIP, nhận phòng sớm'
    ) RETURNING id INTO v_b1_id;

    INSERT INTO booking_details (
        booking_id, hotel_room_type_id, room_type_name, quantity,
        check_in_date, check_out_date, cancellation_policy_id, no_show_policy_id
    ) VALUES (
        v_b1_id, 1, 'Deluxe Room (Phòng Sang trọng)', 1,
        '2026-11-03', '2026-11-06', 1, 1
    ) RETURNING id INTO v_b1_detail_id;

    INSERT INTO booking_rooms (
        booking_detail_id, room_instance_id, adult_count, child_count, infant_count, guest_count,
        status, actual_check_in_at, actual_check_out_at, assigned_at
    ) VALUES (
        v_b1_detail_id, 11, 1, 0, 0, 1,
        'CHECKED_IN', '2026-11-03 14:15:00+07', NULL, '2026-11-03 14:15:00+07'
    ) RETURNING id INTO v_b1_room_id;

    -- Daily rates cho 3 đêm (03/11, 04/11, 05/11)
    INSERT INTO booking_daily_rates (booking_room_id, stay_date, base_price, service_fee_rate, service_fee_amount, tax_category_id, vat_percent, vat_amount, net_price, status)
    VALUES 
        (v_b1_room_id, '2026-11-03', 2500000.00, 5.00, 125000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 210000.00, 2835000.00, 'CONFIRMED'),
        (v_b1_room_id, '2026-11-04', 2500000.00, 5.00, 125000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 210000.00, 2835000.00, 'CONFIRMED'),
        (v_b1_room_id, '2026-11-05', 2500000.00, 5.00, 125000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 210000.00, 2835000.00, 'CONFIRMED');

    -- Cập nhật trạng thái phòng 101C thành OCCUPIED
    UPDATE room_instances SET current_status = 'OCCUPIED' WHERE id = 11;

    -- Tạo StayGuest tương ứng cho Booking 1
    INSERT INTO stay_guests (
        booking_room_id, hotel_id, booking_id, room_id, room_number,
        first_name, last_name, full_name, guest_type, gender, nationality,
        document_type, document_number, date_of_birth,
        permanent_address, current_address, check_in_time, expected_check_out_time, reason_for_stay
    ) VALUES (
        v_b1_room_id, 1, v_b1_id, 11, '101C',
        'Văn An', 'Nguyễn', 'Nguyễn Văn An', 'ADULT', 'MALE', 'Việt Nam',
        'CCCD', '001090012345', '1990-08-15',
        'Số 12 Chùa Bộc, Đống Đa, Hà Nội', 'Hà Nội', '2026-11-03 14:15:00+07', '2026-11-06 12:00:00+07', 'Công tác'
    ) RETURNING id INTO v_b1_stay_guest_id;

    -- Đẩy vào hàng đợi lưu trú (LodgingQueue) với trạng thái PENDING
    INSERT INTO lodging_queues (stay_guest_id, hotel_id, status)
    VALUES (v_b1_stay_guest_id, 1, 'PENDING');

    -- --------------------------------------------------------------------------
    -- 3. BOOKING 2: Khách sạn 1 (Hà Nội) - Phòng 201D (STE) - Đang lưu trú (CHECKED_IN)
    --    Phục vụ T05: 2 khách (1 Người lớn, 1 Trẻ em), 1 PENDING và 1 EXPORTED
    -- --------------------------------------------------------------------------
    INSERT INTO bookings (
        hotel_id, guest_id, booking_type, booking_number,
        subtotal_amount, service_fee_rate, service_fee_amount, total_vat_amount, total_amount,
        status, issued_at, notes
    ) VALUES (
        1, v_guest_2_id, 'FIT', 'BKG-20261104-0002',
        13500000.00, 5.00, 675000.00, 1134000.00, 15309000.00,
        'CONFIRMED', '2026-11-04 11:30:00+07', 'Yêu cầu phòng tầng cao, có trẻ nhỏ'
    ) RETURNING id INTO v_b2_id;

    INSERT INTO booking_details (
        booking_id, hotel_room_type_id, room_type_name, quantity,
        check_in_date, check_out_date, cancellation_policy_id, no_show_policy_id
    ) VALUES (
        v_b2_id, 2, 'Suite (Phòng Căn hộ/Thương gia)', 1,
        '2026-11-04', '2026-11-07', 1, 1
    ) RETURNING id INTO v_b2_detail_id;

    INSERT INTO booking_rooms (
        booking_detail_id, room_instance_id, adult_count, child_count, infant_count, guest_count,
        status, actual_check_in_at, actual_check_out_at, assigned_at
    ) VALUES (
        v_b2_detail_id, 17, 1, 1, 0, 2,
        'CHECKED_IN', '2026-11-04 14:00:00+07', NULL, '2026-11-04 14:00:00+07'
    ) RETURNING id INTO v_b2_room_id;

    INSERT INTO booking_daily_rates (booking_room_id, stay_date, base_price, service_fee_rate, service_fee_amount, tax_category_id, vat_percent, vat_amount, net_price, status)
    VALUES 
        (v_b2_room_id, '2026-11-04', 4500000.00, 5.00, 225000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 378000.00, 5103000.00, 'CONFIRMED'),
        (v_b2_room_id, '2026-11-05', 4500000.00, 5.00, 225000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 378000.00, 5103000.00, 'CONFIRMED'),
        (v_b2_room_id, '2026-11-06', 4500000.00, 5.00, 225000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 378000.00, 5103000.00, 'CONFIRMED');

    -- Cập nhật trạng thái phòng 201D thành OCCUPIED
    UPDATE room_instances SET current_status = 'OCCUPIED' WHERE id = 17;

    -- StayGuest 1: Người lớn (Trần Thị Mai) -> Đã xuất khai báo lưu trú (EXPORTED)
    INSERT INTO stay_guests (
        booking_room_id, hotel_id, booking_id, room_id, room_number,
        first_name, last_name, full_name, guest_type, gender, nationality,
        document_type, document_number, date_of_birth,
        permanent_address, current_address, check_in_time, expected_check_out_time, reason_for_stay
    ) VALUES (
        v_b2_room_id, 1, v_b2_id, 17, '201D',
        'Thị Mai', 'Trần', 'Trần Thị Mai', 'ADULT', 'FEMALE', 'Việt Nam',
        'CCCD', '031195009876', '1995-04-20',
        'Phường Máy Tơ, Ngô Quyền, Hải Phòng', 'Hải Phòng', '2026-11-04 14:00:00+07', '2026-11-07 12:00:00+07', 'Nghỉ dưỡng'
    ) RETURNING id INTO v_b2_sg1_id;

    INSERT INTO lodging_queues (stay_guest_id, hotel_id, status, exported_at, batch_reference)
    VALUES (v_b2_sg1_id, 1, 'EXPORTED', '2026-11-04 16:30:00+07', 'BATCH-LODGING-20261104-01');

    -- StayGuest 2: Trẻ em (Lê Hoàng Nam) -> Đang chờ khai báo (PENDING)
    INSERT INTO stay_guests (
        booking_room_id, hotel_id, booking_id, room_id, room_number,
        first_name, last_name, full_name, guest_type, gender, nationality,
        document_type, document_number, date_of_birth,
        permanent_address, current_address, check_in_time, expected_check_out_time, reason_for_stay
    ) VALUES (
        v_b2_room_id, 1, v_b2_id, 17, '201D',
        'Hoàng Nam', 'Lê', 'Lê Hoàng Nam', 'CHILD', 'MALE', 'Việt Nam',
        'CCCD', '', '2018-05-10',
        'Phường Máy Tơ, Ngô Quyền, Hải Phòng', 'Hải Phòng', '2026-11-04 14:00:00+07', '2026-11-07 12:00:00+07', 'Nghỉ dưỡng'
    ) RETURNING id INTO v_b2_sg2_id;

    INSERT INTO lodging_queues (stay_guest_id, hotel_id, status)
    VALUES (v_b2_sg2_id, 1, 'PENDING');

    -- --------------------------------------------------------------------------
    -- 4. BOOKING 3: Khách sạn 1 (Hà Nội) - Phòng 102C (DLX) - ĐÃ HOÀN THÀNH (CHECKED_OUT)
    --    Khách hàng: Nguyễn Văn An (v_guest_1_id)
    --    Phục vụ U14: Tạo lịch sử lưu trú quá khứ để Personalization phân tích sở thích phòng
    -- --------------------------------------------------------------------------
    INSERT INTO bookings (
        hotel_id, guest_id, booking_type, booking_number,
        subtotal_amount, service_fee_rate, service_fee_amount, total_vat_amount, total_amount,
        status, issued_at, notes
    ) VALUES (
        1, v_guest_1_id, 'FIT', 'BKG-20261015-0003',
        7500000.00, 5.00, 375000.00, 630000.00, 8505000.00,
        'CONFIRMED', '2026-10-15 09:00:00+07', 'Lưu trú đã hoàn thành'
    ) RETURNING id INTO v_b3_id;

    INSERT INTO booking_details (
        booking_id, hotel_room_type_id, room_type_name, quantity,
        check_in_date, check_out_date, cancellation_policy_id, no_show_policy_id
    ) VALUES (
        v_b3_id, 1, 'Deluxe Room (Phòng Sang trọng)', 1,
        '2026-10-15', '2026-10-18', 1, 1
    ) RETURNING id INTO v_b3_detail_id;

    INSERT INTO booking_rooms (
        booking_detail_id, room_instance_id, adult_count, child_count, infant_count, guest_count,
        status, actual_check_in_at, actual_check_out_at, assigned_at
    ) VALUES (
        v_b3_detail_id, 12, 1, 0, 0, 1,
        'CHECKED_OUT', '2026-10-15 14:30:00+07', '2026-10-18 11:45:00+07', '2026-10-15 14:30:00+07'
    ) RETURNING id INTO v_b3_room_id;

    INSERT INTO booking_daily_rates (booking_room_id, stay_date, base_price, service_fee_rate, service_fee_amount, tax_category_id, vat_percent, vat_amount, net_price, status)
    VALUES 
        (v_b3_room_id, '2026-10-15', 2500000.00, 5.00, 125000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 210000.00, 2835000.00, 'CONFIRMED'),
        (v_b3_room_id, '2026-10-16', 2500000.00, 5.00, 125000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 210000.00, 2835000.00, 'CONFIRMED'),
        (v_b3_room_id, '2026-10-17', 2500000.00, 5.00, 125000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 210000.00, 2835000.00, 'CONFIRMED');

    INSERT INTO stay_guests (
        booking_room_id, hotel_id, booking_id, room_id, room_number,
        first_name, last_name, full_name, guest_type, gender, nationality,
        document_type, document_number, date_of_birth,
        permanent_address, current_address, check_in_time, expected_check_out_time, actual_check_out_time, reason_for_stay
    ) VALUES (
        v_b3_room_id, 1, v_b3_id, 12, '102C',
        'Văn An', 'Nguyễn', 'Nguyễn Văn An', 'ADULT', 'MALE', 'Việt Nam',
        'CCCD', '001090012345', '1990-08-15',
        'Số 12 Chùa Bộc, Đống Đa, Hà Nội', 'Hà Nội', '2026-10-15 14:30:00+07', '2026-10-18 12:00:00+07', '2026-10-18 11:45:00+07', 'Du lịch'
    ) RETURNING id INTO v_b3_stay_guest_id;

    INSERT INTO lodging_queues (stay_guest_id, hotel_id, status, exported_at, batch_reference)
    VALUES (v_b3_stay_guest_id, 1, 'EXPORTED', '2026-10-15 17:00:00+07', 'BATCH-LODGING-20261015-01');

    -- --------------------------------------------------------------------------
    -- 5. BOOKING 4: Khách sạn 2 (Đà Nẵng) - Phòng 101B (SUP) - Sắp đến (EXPECTED)
    --    Khách quốc tế: Michael Brown
    -- --------------------------------------------------------------------------
    INSERT INTO bookings (
        hotel_id, guest_id, booking_type, booking_number,
        subtotal_amount, service_fee_rate, service_fee_amount, total_vat_amount, total_amount,
        status, issued_at, notes
    ) VALUES (
        2, v_guest_3_id, 'FIT', 'BKG-20261110-0004',
        3600000.00, 5.00, 180000.00, 302400.00, 4082400.00,
        'CONFIRMED', '2026-11-04 15:00:00+07', 'Khách quốc tế, yêu cầu phòng view biển yên tĩnh'
    ) RETURNING id INTO v_b4_id;

    INSERT INTO booking_details (
        booking_id, hotel_room_type_id, room_type_name, quantity,
        check_in_date, check_out_date, cancellation_policy_id, no_show_policy_id
    ) VALUES (
        v_b4_id, 4, 'Superior Room (Phòng Cao cấp)', 1,
        '2026-11-10', '2026-11-13', 1, 1
    ) RETURNING id INTO v_b4_detail_id;

    INSERT INTO booking_rooms (
        booking_detail_id, room_instance_id, adult_count, child_count, infant_count, guest_count,
        status, actual_check_in_at, actual_check_out_at, assigned_at
    ) VALUES (
        v_b4_detail_id, 6, 1, 0, 0, 1,
        'EXPECTED', NULL, NULL, '2026-11-04 15:00:00+07'
    ) RETURNING id INTO v_b4_room_id;

    INSERT INTO booking_daily_rates (booking_room_id, stay_date, base_price, service_fee_rate, service_fee_amount, tax_category_id, vat_percent, vat_amount, net_price, status)
    VALUES 
        (v_b4_room_id, '2026-11-10', 1200000.00, 5.00, 60000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 100800.00, 1360800.00, 'PENDING'),
        (v_b4_room_id, '2026-11-11', 1200000.00, 5.00, 60000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 100800.00, 1360800.00, 'PENDING'),
        (v_b4_room_id, '2026-11-12', 1200000.00, 5.00, 60000.00, (SELECT id FROM tax_categories WHERE category_code = 'ROOM'), 8.00, 100800.00, 1360800.00, 'PENDING');

    RAISE NOTICE 'Đã tạo thành công 4 bộ dữ liệu booking mẫu!';
END $$;

