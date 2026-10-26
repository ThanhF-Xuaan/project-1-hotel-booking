package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import java.time.OffsetDateTime;

/** Minimal, read-only stay data exposed to the U14 personalization module. */
public record PersonalizationStayRecord(
        Long bookingRoomId,
        Integer hotelId,
        Integer hotelRoomTypeId,
        OffsetDateTime actualCheckInAt,
        OffsetDateTime actualCheckOutAt
) {
}
