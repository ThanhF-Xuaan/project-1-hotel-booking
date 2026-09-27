package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionResponse {

    Long id;
    Long paymentId;
    Long bookingId;
    TransactionType transactionType;
    BigDecimal amount;
    String referenceCode;
    String status;
    OffsetDateTime issuedAt;
    OffsetDateTime createdAt;
}
