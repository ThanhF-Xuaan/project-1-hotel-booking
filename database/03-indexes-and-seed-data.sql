-- ==============================================================================
-- 8. ĐÁNH INDEX (TỐI ƯU HIỆU NĂNG TRUY VẤN)
-- ==============================================================================

-- A. ĐÁNH INDEX CHO BẢNG MENUS & CATALOG (Tối ưu tìm kiếm dịch vụ)
CREATE INDEX idx_menus_hotel_id ON menus(hotel_id);
CREATE INDEX idx_menus_tax_category_id ON menus(tax_category_id);
CREATE INDEX idx_menus_type ON menus(menu_type); 

-- B. ĐÁNH INDEX CHO CỤM IAM & TỔ CHỨC (Tối ưu hóa quá trình Login & Phân quyền)
CREATE INDEX idx_staffs_keycloak_id ON staffs(keycloak_id); -- BẮT BUỘC: Vì Keycloak đăng nhập dùng UUID này
CREATE INDEX idx_staffs_scope ON staffs(scope_type, scope_entity_id); -- BẮT BUỘC: Để Backend quét Data Scope nhanh

-- C. ĐÁNH INDEX CHO KHO PHÒNG & GIÁ (Chống thắt cổ chai khi Khách hàng Search phòng online)
CREATE INDEX idx_hotel_room_types_hotel_id ON hotel_room_types(hotel_id);
CREATE INDEX idx_room_instances_hotel_room_type ON room_instances(hotel_id, hotel_room_type_id);
CREATE INDEX idx_room_availability_date ON room_availability(date);
CREATE INDEX idx_room_availability_locked_until ON room_availability(locked_until);
CREATE INDEX idx_room_slots_date ON room_slots(slot_date);

-- D. ĐÁNH INDEX CHO LUỒNG ĐẶT PHÒNG (BOOKINGS & ROOM ASSIGNMENT)
CREATE INDEX idx_bookings_guest_id ON bookings(guest_id);
CREATE INDEX idx_bookings_company_id ON bookings(company_id); -- [MỚI] Tối ưu tìm kiếm cho Khách đoàn/Corporate
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_booking_details_booking_id ON booking_details(booking_id);
CREATE INDEX idx_booking_details_dates ON booking_details(check_in_date, check_out_date);

CREATE INDEX idx_booking_rooms_detail_id ON booking_rooms(booking_detail_id);
CREATE INDEX idx_booking_rooms_status ON booking_rooms(status); -- Tối ưu cho việc Lễ tân lọc phòng "EXPECTED", "CHECKED_IN"

-- E. ĐÁNH INDEX CHO LUỒNG TÀI CHÍNH & NIGHT AUDIT (Chống giật lag lúc Checkout)
CREATE INDEX idx_payments_booking_id ON payments(booking_id);

-- [ĐÃ SỬA]: Chuyển thành booking_room_id cho khớp với thiết kế Folio ở v7
CREATE INDEX idx_booking_daily_rates_room_id ON booking_daily_rates(booking_room_id);
CREATE INDEX idx_booking_daily_rates_status ON booking_daily_rates(status); -- Tối ưu cho Job Night Audit quét trạng thái 'PENDING'

CREATE INDEX idx_booking_charges_room_id ON booking_charges(booking_room_id);

CREATE INDEX idx_service_orders_booking_id ON service_orders(booking_id);
CREATE INDEX idx_service_order_details_menu_id ON service_order_details(menu_id);

-- F. ĐÁNH INDEX CHO HÓA ĐƠN & BÁO CÁO BI (Business Intelligence)
CREATE INDEX idx_invoices_booking_id ON invoices(booking_id);
CREATE INDEX idx_invoice_details_invoice_id ON invoice_details(invoice_id);
CREATE INDEX idx_invoice_details_line_type ON invoice_details(line_type); -- Gom báo cáo siêu tốc theo loại doanh thu (BI/Dashboard)

-- G. ĐÁNH INDEX CHO LUẬT CẤU HÌNH (RULES)
CREATE INDEX idx_surcharge_rules_age_policy_id ON surcharge_rules(age_policy_id);
CREATE INDEX idx_vat_rules_tax_category_id ON vat_rules(tax_category_id);

-- ĐÁNH INDEX CHO BẢNG AUDIT 

-- Tối ưu cho query: "Tìm lịch sử thay đổi của Đơn đặt phòng ID = 12345"
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);

-- Tối ưu cho query: "Nhân viên Nguyễn Văn A đã làm những gì trong tháng này?"
CREATE INDEX idx_audit_logs_staff ON audit_logs(staff_id);

-- Tối ưu cho query lọc theo thời gian (Bảo mật)
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);