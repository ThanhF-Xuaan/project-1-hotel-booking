package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;

import java.util.List;

public interface BookingDetailService {

    /**
     * Lấy thực thể BookingDetail theo ID (ném ngoại lệ BOOKING_DETAIL_NOT_FOUND nếu không tìm thấy)
     *
     * @param id ID của chi tiết đặt phòng
     * @return Thực thể BookingDetail
     */
    BookingDetail getEntityById(Long id);

    /**
     * Tìm danh sách BookingDetail thuộc về một đơn đặt phòng
     *
     * @param bookingId ID của đơn đặt phòng
     * @return Danh sách BookingDetail
     */
    List<BookingDetail> findByBookingId(Long bookingId);

    /**
     * Kiểm tra xem có chi tiết đặt phòng nào đang tham chiếu tới chính sách hủy hay không
     *
     * @param cancellationPolicyId ID của chính sách hủy
     * @return true nếu tồn tại ít nhất 1 chi tiết đặt phòng tham chiếu, ngược lại false
     */
    boolean existsByCancellationPolicyId(Integer cancellationPolicyId);

    /**
     * Kiểm tra xem có chi tiết đặt phòng nào đang tham chiếu tới chính sách vắng mặt hay không
     *
     * @param noShowPolicyId ID của chính sách vắng mặt
     * @return true nếu tồn tại ít nhất 1 chi tiết đặt phòng tham chiếu, ngược lại false
     */
    boolean existsByNoShowPolicyId(Integer noShowPolicyId);
}
