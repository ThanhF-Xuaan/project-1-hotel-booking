-- ==============================================================================
-- SPRINT 4 — U24: DATABASE PERFORMANCE OPTIMIZATION INDEXES
-- Author: utc-hotel-team
-- Date: 2026-11-01
-- ==============================================================================

-- 1. BOOKINGS & INVENTORY ASSIGNMENT
-- Tối ưu lọc đơn đặt phòng theo khách sạn (multi-tenant scope)
CREATE INDEX IF NOT EXISTS idx_bookings_hotel_id ON bookings(hotel_id);

-- Tối ưu join và tìm kiếm chi tiết đặt phòng theo loại phòng
CREATE INDEX IF NOT EXISTS idx_booking_details_hotel_room_type ON booking_details(hotel_room_type_id);

-- Tối ưu tìm kiếm phòng đặt theo phòng vật lý và tra cứu trạng thái phòng
CREATE INDEX IF NOT EXISTS idx_booking_rooms_room_instance ON booking_rooms(room_instance_id);

-- 2. ROOM SLOTS & MAINTENANCE
-- Tối ưu tìm kiếm và giải phóng slot theo lệnh bảo trì
CREATE INDEX IF NOT EXISTS idx_room_slots_maintenance_block ON room_slots(maintenance_block_id);

-- Tối ưu tìm kiếm slot theo phòng đặt
CREATE INDEX IF NOT EXISTS idx_room_slots_booking_room ON room_slots(booking_room_id);

-- 3. ROOM INSTANCES & VACANCY
-- Tối ưu tìm kiếm phòng trống theo khách sạn và trạng thái
CREATE INDEX IF NOT EXISTS idx_room_instances_hotel_status ON room_instances(hotel_id, current_status) WHERE is_deleted = false;

-- 4. PAYMENTS & TRANSACTIONS
-- Tối ưu tra cứu thanh toán theo mã tham chiếu giao dịch (webhook & polling)
CREATE INDEX IF NOT EXISTS idx_payments_txn_ref ON payments(transaction_reference);

-- Tối ưu quét và lọc thanh toán theo trạng thái (PENDING, SUCCESS...)
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

-- Tối ưu tìm kiếm giao dịch theo đơn đặt phòng và thanh toán
CREATE INDEX IF NOT EXISTS idx_transactions_booking_id ON transactions(booking_id);
CREATE INDEX IF NOT EXISTS idx_transactions_payment_id ON transactions(payment_id);

-- 5. PRICING RULES (RATE PLANS)
-- Tối ưu truy vấn quy tắc giá hoạt động theo loại phòng và khoảng ngày
CREATE INDEX IF NOT EXISTS idx_pricing_rules_lookup ON pricing_rules(hotel_room_type_id, status, start_date, end_date) WHERE is_deleted = false;

