package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingSearchDto extends BaseSearchDto {

    Short hotelId;
    Long guestId;
    String bookingNumber;
    String roomNumber;
    BookingType bookingType;
    BookingStatus status;
    LocalDate checkInDate;
    LocalDate checkOutDate;
}
