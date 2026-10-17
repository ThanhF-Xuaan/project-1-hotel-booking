package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingChargeCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingCharge;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;

import java.util.List;

public interface BookingChargeService {

    BookingCharge save(BookingCharge charge);

    List<BookingCharge> saveAll(List<BookingCharge> charges);

    List<BookingCharge> findByBookingRoomId(Long bookingRoomId);

    BookingCharge createCharge(BookingRoom bookingRoom, BookingChargeCreateRequest request);
}
