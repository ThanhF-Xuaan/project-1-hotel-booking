package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingChargeType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingChargeResponse {

    Long id;
    Long bookingRoomId;
    Long bookingGuestId;
    BookingChargeType chargeType;
    String itemName;
    String description;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal subtotal;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    BigDecimal vatRate;
    BigDecimal vatAmount;
    BigDecimal totalAmount;
    OffsetDateTime issuedAt;
}
