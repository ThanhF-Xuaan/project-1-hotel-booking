package vn.edu.utc.hotel_booking.modules.booking.repository.projection;

import java.time.Instant;

/** Native-query projection; never expose a Booking entity to AI. */
public interface PersonalizationHistoryRow {
    Long getBookingRoomId();

    Integer getHotelId();

    Integer getHotelRoomTypeId();

    Instant getActualCheckInAt();

    Instant getActualCheckOutAt();
}
