package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceResponse {

    Long id;
    Long bookingId;
    String bookingNumber;
    String invoiceNumber;
    BigDecimal subTotal;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    BigDecimal vatAmount;
    BigDecimal grandTotal;
    InvoiceStatus status;
    OffsetDateTime issuedAt;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    @Builder.Default
    List<InvoiceDetailResponse> details = new ArrayList<>();
}
