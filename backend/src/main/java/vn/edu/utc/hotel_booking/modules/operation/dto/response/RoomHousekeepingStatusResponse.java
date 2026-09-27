package vn.edu.utc.hotel_booking.modules.operation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomHousekeepingStatusResponse {

    Integer roomInstanceId;
    String roomNumber;
    Integer hotelRoomTypeId;
    String roomTypeName;
    Short hotelId;
    String hotelName;
    String currentStatus; // READY, OCCUPIED, CLEANING, DIRTY, MAINTENANCE
    String guestName;
    String bookingNumber;
    LocalDate checkOutDate;
}
