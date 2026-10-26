package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BookingDailyRateService {

    /**
     * Tìm danh sách giá phòng theo ngày của một phòng đặt
     *
     * @param bookingRoomId ID của phòng đặt
     * @return Danh sách BookingDailyRate
     */
    List<BookingDailyRate> findByBookingRoomId(Long bookingRoomId);

    /**
     * Tìm các bản ghi giá ngày đang hoạt động (không thuộc đơn/phòng bị hủy) cho một khách sạn vào một ngày cụ thể
     *
     * @param hotelId ID của khách sạn
     * @param stayDate Ngày lưu trú
     * @return Danh sách BookingDailyRate
     */
    List<BookingDailyRate> findActiveDailyRatesForHotelAndDate(Short hotelId, LocalDate stayDate);

    /**
     * Tính tổng doanh thu tiền phòng trong ngày cho một khách sạn (phục vụ đối soát kiểm toán đêm)
     *
     * @param hotelId ID của khách sạn
     * @param date Ngày kiểm toán
     * @return Tổng doanh thu tiền phòng (tổng netPrice)
     */
    BigDecimal calculateDailyRoomRevenue(Short hotelId, LocalDate date);

    /**
     * Tìm các bản ghi giá ngày hoạt động cho danh sách khách sạn trong khoảng ngày (phục vụ báo cáo BI cấp cao)
     *
     * @param hotelIds Danh sách ID khách sạn
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Danh sách BookingDailyRate
     */
    List<BookingDailyRate> findActiveDailyRatesForHotelsAndDateRange(List<Short> hotelIds, LocalDate startDate, LocalDate endDate);
}
