package vn.edu.utc.hotel_booking.modules.finance.service;

import vn.edu.utc.hotel_booking.modules.finance.dto.response.FolioResponse;

public interface FolioService {

    /**
     * Lấy Sổ cái Folio chi phí lưu trú tổng hợp theo ID đơn đặt phòng
     *
     * @param bookingId ID đơn đặt phòng
     * @return Sổ cái Folio thời gian thực
     */
    FolioResponse getFolioByBookingId(Long bookingId);

    /**
     * Lấy Sổ cái Folio chi phí lưu trú cho một phòng cụ thể trong đơn
     *
     * @param bookingRoomId ID phòng đặt
     * @return Sổ cái Folio phòng thời gian thực
     */
    FolioResponse getFolioByBookingRoomId(Long bookingRoomId);

    /**
     * Tra cứu Sổ cái Folio theo mã đặt phòng (Booking Number)
     *
     * @param bookingNumber Mã đơn đặt phòng
     * @return Sổ cái Folio thời gian thực
     */
    FolioResponse getFolioByBookingNumber(String bookingNumber);
}
