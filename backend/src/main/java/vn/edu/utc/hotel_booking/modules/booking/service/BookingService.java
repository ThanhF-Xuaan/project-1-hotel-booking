package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingChargeCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingChargeResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;

public interface BookingService {

    BookingResponse createBooking(BookingCreateRequest request);

    PageResponse<BookingResponse> filter(BookingSearchDto searchDto);

    BookingResponse getById(Long id);

    BookingResponse getByBookingNumber(String bookingNumber);

    BookingResponse updateStatus(Long id, BookingStatus status);

    BookingResponse assignRoom(Long bookingRoomId, Integer roomInstanceId);

    BookingChargeResponse addCharge(Long bookingRoomId, BookingChargeCreateRequest request);

    BookingResponse checkIn(Long bookingRoomId);

    BookingResponse checkOut(Long bookingRoomId);
}
