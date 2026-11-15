package vn.edu.utc.hotel_booking.modules.notification.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentReceiptEmailEvent {

    Long paymentId;
    String bookingCode;
    String recipientEmail;
    String customerName;
    String hotelName;
    BigDecimal amount;
    String paymentMethod;
    String transactionReference;
    OffsetDateTime paidAt;
    BigDecimal totalBookingAmount;
    BigDecimal remainingBalance;
    String currency;
}
