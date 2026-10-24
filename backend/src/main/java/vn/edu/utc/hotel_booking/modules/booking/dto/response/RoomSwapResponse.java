package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSwapResponse {

    Long swapRecordId;
    Long bookingId;
    String bookingNumber;
    Long bookingRoomId;

    Integer previousRoomInstanceId;
    String previousRoomNumber;
    String previousRoomTypeName;

    Integer newRoomInstanceId;
    String newRoomNumber;
    String newRoomTypeName;

    LocalDate swapDate;
    LocalDate effectiveDate;
    String reason;

    Boolean isUpgrade;
    Boolean isComplimentary;
    BigDecimal priceDifference;

    BookingRoomStatus roomStatus;
    OffsetDateTime swappedAt;
    String performedBy;
}
