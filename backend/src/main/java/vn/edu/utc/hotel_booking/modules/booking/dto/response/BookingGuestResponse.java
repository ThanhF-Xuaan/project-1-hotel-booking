package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingGuestType;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingGuestResponse {

    Long id;
    String firstName;
    String lastName;
    String fullName;
    LocalDate birthDate;
    BookingGuestType guestType;
    String identityType;
    String identityNumber;
}
