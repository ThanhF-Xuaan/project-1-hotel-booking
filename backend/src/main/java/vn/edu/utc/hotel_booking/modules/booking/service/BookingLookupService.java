package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;

import java.util.Optional;

public interface BookingLookupService {

    /**
     * Tra cứu thực thể Booking theo ID (ném ngoại lệ BOOKING_NOT_FOUND nếu không tìm thấy)
     *
     * @param id ID của đơn đặt phòng
     * @return Thực thể Booking
     */
    Booking getBookingEntityById(Long id);

    /**
     * Tra cứu thực thể Booking theo mã đơn bookingNumber (ném ngoại lệ BOOKING_NOT_FOUND nếu không tìm thấy)
     *
     * @param bookingNumber Mã của đơn đặt phòng
     * @return Thực thể Booking
     */
    Booking getBookingEntityByNumber(String bookingNumber);

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
     * Tra cứu phòng đặt theo ID đơn đặt phòng và ID phòng vật lý
     *
     * @param bookingId ID đơn đặt phòng
     * @param roomInstanceId ID phòng vật lý
     * @return Optional chứa BookingRoom nếu tìm thấy
     */
    java.util.Optional<BookingRoom> findBookingRoomByBookingIdAndRoomInstanceId(Long bookingId, Integer roomInstanceId);

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

    /**
     * Kiểm tra xem có chi tiết đặt phòng nào đang tham chiếu tới chính sách hủy hay không
     *
     * @param cancellationPolicyId ID của chính sách hủy
     * @return true nếu có ít nhất 1 chi tiết đặt phòng tham chiếu, ngược lại false
     */
    boolean existsBookingDetailByCancellationPolicyId(Integer cancellationPolicyId);

    /**
     * Kiểm tra xem có chi tiết đặt phòng nào đang tham chiếu tới chính sách vắng mặt hay không
     *
     * @param noShowPolicyId ID của chính sách vắng mặt
     * @return true nếu có ít nhất 1 chi tiết đặt phòng tham chiếu, ngược lại false
     */
    boolean existsBookingDetailByNoShowPolicyId(Integer noShowPolicyId);

    /**
     * Tra cứu toàn bộ các đơn đặt phòng của một khách hàng (kèm thông tin khách sạn)
     *
     * @param guestId ID khách hàng
     * @return Danh sách các đơn đặt phòng
     */
    java.util.List<Booking> getBookingsByGuestId(Long guestId);

    /**
     * Tra cứu toàn bộ các đơn đặt phòng thuộc một hợp đồng đoàn
     *
     * @param contractId ID hợp đồng đoàn
     * @return Danh sách các đơn đặt phòng
     */
    java.util.List<Booking> getBookingsByContractId(Long contractId);

    /**
     * Tra cứu giá phòng hoạt động theo danh sách khách sạn và khoảng ngày (cho báo cáo BI cấp cao)
     *
     * @param hotelIds Danh sách ID khách sạn
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Danh sách BookingDailyRate
     */
    java.util.List<vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate> getActiveDailyRatesForHotelsAndDateRange(
            java.util.List<Short> hotelIds,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    );
}
