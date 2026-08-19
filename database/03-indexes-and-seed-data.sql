-- ==============================================================================
-- 1. ĐÁNH INDEX (TỐI ƯU HIỆU NĂNG TRUY VẤN)
-- ==============================================================================
-- Đánh Index cho các khóa ngoại (Foreign Keys) thường xuyên được dùng trong lệnh JOIN
CREATE INDEX idx_staffs_hotel_id ON staffs(hotel_id);
CREATE INDEX idx_catalog_items_hotel_id ON catalog_items(hotel_id);
CREATE INDEX idx_hotel_room_types_hotel_id ON hotel_room_types(hotel_id);
CREATE INDEX idx_room_instances_hotel_room_type ON room_instances(hotel_id, hotel_room_type_id);
CREATE INDEX idx_booking_details_booking_id ON booking_details(booking_id);
CREATE INDEX idx_payments_booking_id ON payments(booking_id);

-- Đánh Index cho các cột thường xuyên được dùng trong mệnh đề WHERE (Tìm kiếm, Lọc theo khoảng thời gian)
CREATE INDEX idx_room_availability_date ON room_availability(date);
CREATE INDEX idx_room_availability_locked_until ON room_availability(locked_until);
CREATE INDEX idx_room_slots_date ON room_slots(slot_date);
CREATE INDEX idx_bookings_guest_id ON bookings(guest_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_booking_details_dates ON booking_details(check_in_date, check_out_date);


-- Bổ sung Index cho bảng surcharge_rules (Để query lấy policy nhanh)
CREATE INDEX idx_surcharge_rules_age_policy_id ON surcharge_rules(age_policy_id);

-- Bổ sung Index cho bảng vat_rules (Để query lấy thuế nhanh)
CREATE INDEX idx_vat_rules_tax_category_id ON vat_rules(tax_category_id);

-- Bổ sung Index cho bảng catalog_items (Để query lấy thuế nhanh)
CREATE INDEX idx_catalog_items_tax_category_id ON catalog_items(tax_category_id);