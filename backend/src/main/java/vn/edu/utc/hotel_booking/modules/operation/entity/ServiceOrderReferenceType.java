package vn.edu.utc.hotel_booking.modules.operation.entity;

/**
 * 4 cấp độ hạch toán của đơn đặt dịch vụ (Service Orders):
 * - BOOKING: Đơn tính theo toàn bộ đơn đặt phòng (ví dụ: xe đón sân bay đoàn, addon trực tuyến). reference_id = bookings.id
 * - BOOKING_ROOM: Đơn tính theo từng phòng vật lý (ví dụ: minibar, room service). reference_id = booking_rooms.id
 * - STAY_GUEST: Đơn tính theo từng khách lưu trú cá nhân để tách hóa đơn split-billing (ví dụ: spa, giặt ủi). reference_id = stay_guests.id
 * - WALK_IN: Khách vãng lai đến ăn uống/uống cafe mà không thuê phòng. reference_id = NULL
 */
public enum ServiceOrderReferenceType {
    BOOKING,
    BOOKING_ROOM,
    STAY_GUEST,
    WALK_IN
}
