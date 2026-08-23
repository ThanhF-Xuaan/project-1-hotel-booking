-- ==============================================================================
-- 1. ĐÁNH INDEX (TỐI ƯU HIỆU NĂNG TRUY VẤN)
-- ==============================================================================

-- A. ĐÁNH INDEX CHO BẢNG MENUS (Đã sửa từ catalog_items)
CREATE INDEX idx_menus_hotel_id ON menus(hotel_id);
CREATE INDEX idx_menus_tax_category_id ON menus(tax_category_id);
CREATE INDEX idx_menus_type ON menus(menu_type); -- Phục vụ query lọc Hàng hóa / Dịch vụ nhanh

-- B. ĐÁNH INDEX CHO CÁC KHÓA NGOẠI (Foreign Keys) THƯỜNG XUYÊN JOIN
CREATE INDEX idx_staffs_hotel_id ON staffs(hotel_id);
CREATE INDEX idx_hotel_room_types_hotel_id ON hotel_room_types(hotel_id);
CREATE INDEX idx_room_instances_hotel_room_type ON room_instances(hotel_id, hotel_room_type_id);
CREATE INDEX idx_booking_details_booking_id ON booking_details(booking_id);
CREATE INDEX idx_payments_booking_id ON payments(booking_id);

-- [BỔ SUNG] Đánh index cho các FK quan trọng của luồng Tài chính & Order (Chống thắt cổ chai khi Checkout)
CREATE INDEX idx_booking_daily_rates_detail_id ON booking_daily_rates(booking_detail_id);
CREATE INDEX idx_service_orders_booking_id ON service_orders(booking_id);
CREATE INDEX idx_service_order_details_menu_id ON service_order_details(menu_id);
CREATE INDEX idx_invoice_details_invoice_id ON invoice_details(invoice_id);

-- C. ĐÁNH INDEX CHO CÁC CỘT THƯỜNG XUYÊN TRONG MỆNH ĐỀ WHERE (Tìm kiếm, Lọc ngày)
CREATE INDEX idx_room_availability_date ON room_availability(date);
CREATE INDEX idx_room_availability_locked_until ON room_availability(locked_until);
CREATE INDEX idx_room_slots_date ON room_slots(slot_date);
CREATE INDEX idx_bookings_guest_id ON bookings(guest_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_booking_details_dates ON booking_details(check_in_date, check_out_date);

-- D. ĐÁNH INDEX CHO CÁC BẢNG CẤU HÌNH & LUẬT LỆ
CREATE INDEX idx_surcharge_rules_age_policy_id ON surcharge_rules(age_policy_id);
CREATE INDEX idx_vat_rules_tax_category_id ON vat_rules(tax_category_id);

-- [BỔ SUNG BI] Index cho bảng hóa đơn để gom báo cáo siêu tốc theo loại doanh thu (BI/Dashboard)
CREATE INDEX idx_invoice_details_line_type ON invoice_details(line_type);