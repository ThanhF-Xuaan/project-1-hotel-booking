package vn.edu.utc.hotel_booking.modules.notification.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceItemEmailDto {
    String description;
    Integer quantity;
    BigDecimal unitPrice;
    String unitPriceFormatted;
    BigDecimal vatRate;
    String vatRateFormatted;
    BigDecimal vatAmount;
    String vatAmountFormatted;
    BigDecimal subtotal;
    String subtotalFormatted;
}
