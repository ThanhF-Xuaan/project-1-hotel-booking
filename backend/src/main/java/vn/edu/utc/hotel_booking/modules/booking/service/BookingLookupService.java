package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;

public interface BookingLookupService {

    /**
     * Tra cứu thực thể Booking theo ID (ném ngoại lệ BOOKING_NOT_FOUND nếu không tìm thấy)
     *
     * @param id ID của đơn đặt phòng
     * @return Thực thể Booking
     */
    Booking getBookingEntityById(Long id);

    /**
     * Tra cứu thực thể BookingRoom theo ID (ném ngoại lệ BOOKING_ROOM_NOT_FOUND nếu không tìm thấy)
     *
     * @param id ID của phòng đặt
     * @return Thực thể BookingRoom
     */
    BookingRoom getBookingRoomEntityById(Long id);

    /**
     * Lấy toàn bộ danh sách phòng đặt thuộc đơn đặt phòng (qua quan hệ Booking -> BookingDetail -> BookingRoom)
     *
     * @param bookingId ID của đơn đặt phòng
     * @return Danh sách các BookingRoom
     */
    java.util.List<BookingRoom> getBookingRoomsByBookingId(Long bookingId);

    /**
     * Tra cứu thực thể Booking gốc từ ID của phòng đặt
     *
     * @param bookingRoomId ID của phòng đặt
     * @return Thực thể Booking
     */
    Booking getBookingByRoomId(Long bookingRoomId);

    /**
     * Lấy thông tin phòng đang có khách ở (OCCUPIED) theo roomInstanceId
     *
     * @param roomInstanceId ID phòng vật lý
     * @return DTO thông tin lưu trú RoomOccupancyDto (hoặc null nếu không có khách ở)
     */
    vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomOccupancyDto getActiveOccupancyByRoomInstanceId(Integer roomInstanceId);

    /**
     * Lấy thông tin hàng loạt các phòng đang có khách ở theo danh sách roomInstanceIds (Batch lookup chống N+1)
     *
     * @param roomInstanceIds Danh sách ID phòng vật lý
     * @return Map giữa roomInstanceId và RoomOccupancyDto
     */
    java.util.Map<Integer, vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomOccupancyDto> getActiveOccupanciesByRoomInstanceIds(
            java.util.Collection<Integer> roomInstanceIds
    );

    /**
     * Tính tổng doanh thu tiền phòng trong ngày của khách sạn
     *
     * @param hotelId ID khách sạn
     * @param date Ngày kiểm toán
     * @return Tổng doanh thu tiền phòng
     */
    java.math.BigDecimal getDailyRoomRevenue(Short hotelId, java.time.LocalDate date);
}
