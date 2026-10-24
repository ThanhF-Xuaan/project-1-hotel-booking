package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FolioPaymentDto {

    Long paymentId;
    String paymentMethod;
    String paymentPurpose;
    String provider;
    String referenceCode;
    BigDecimal amount;
    String status;
    OffsetDateTime paidAt;
}
