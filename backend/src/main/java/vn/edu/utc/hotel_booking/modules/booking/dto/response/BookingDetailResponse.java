package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDetailResponse {

    Long id;
    Long bookingId;
    Integer hotelRoomTypeId;
    String roomTypeName;
    Short quantity;
    LocalDate checkInDate;
    LocalDate checkOutDate;

    @Builder.Default
    List<BookingRoomResponse> bookingRooms = new ArrayList<>();
}
