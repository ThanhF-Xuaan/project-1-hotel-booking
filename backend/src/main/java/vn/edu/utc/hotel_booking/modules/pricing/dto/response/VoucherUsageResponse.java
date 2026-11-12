package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherUsageResponse {

    Long id;
    Integer voucherId;
    String voucherCode;
    Long bookingGuestId;
    String guestFullName;
    String guestPhoneNumber;
    BigDecimal discountAmount;
    OffsetDateTime usedAt;
}

