package vn.edu.utc.hotel_booking.modules.notification.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.notification.dto.ServiceItemEmailDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Builder.Default
    List<ServiceItemEmailDto> services = new ArrayList<>();

    @Builder.Default
    List<ServiceItemEmailDto> surcharges = new ArrayList<>();
}
