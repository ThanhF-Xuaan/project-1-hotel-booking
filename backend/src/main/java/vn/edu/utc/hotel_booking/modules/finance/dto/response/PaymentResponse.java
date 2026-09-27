package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentPurpose;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {

    Long id;
    Long bookingId;
    String bookingNumber;
    BigDecimal totalAmount;
    PaymentPurpose paymentPurpose;
    PaymentMethod paymentMethod;
    String paymentProvider;
    String transactionReference;
    PaymentStatus status;
    OffsetDateTime paidAt;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
