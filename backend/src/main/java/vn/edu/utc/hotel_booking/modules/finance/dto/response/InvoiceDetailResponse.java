package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.InvoiceLineType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDetailResponse {

    Long id;
    Long referenceId;
    InvoiceLineType lineType;
    String description;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal subtotal;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    BigDecimal vatRate;
    BigDecimal vatAmount;
    BigDecimal totalAmount;
    OffsetDateTime createdAt;
}
