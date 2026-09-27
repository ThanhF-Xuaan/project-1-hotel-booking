package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingRoomResponse {

    Long id;
    Long bookingDetailId;
    Integer roomInstanceId;
    String roomNumber;
    Short adultCount;
    Short childCount;
    Short infantCount;
    Short guestCount;
    BookingRoomStatus status;
    OffsetDateTime actualCheckInAt;
    OffsetDateTime actualCheckOutAt;
    OffsetDateTime assignedAt;

    @Builder.Default
    List<BookingGuestResponse> bookingGuests = new ArrayList<>();

    @Builder.Default
    List<BookingDailyRateResponse> dailyRates = new ArrayList<>();

    @Builder.Default
    List<BookingChargeResponse> charges = new ArrayList<>();
}
