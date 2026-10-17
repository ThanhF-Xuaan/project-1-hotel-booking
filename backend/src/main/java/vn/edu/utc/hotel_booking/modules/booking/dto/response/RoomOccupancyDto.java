package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomOccupancyDto {
    Integer roomInstanceId;
    String bookingNumber;
    LocalDate checkOutDate;
    String guestName;
}
