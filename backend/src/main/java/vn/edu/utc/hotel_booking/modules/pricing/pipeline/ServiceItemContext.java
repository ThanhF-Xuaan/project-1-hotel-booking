package vn.edu.utc.hotel_booking.modules.pricing.pipeline;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceItemContext {
    Integer serviceId;
    String serviceName;
    ServicePricingType pricingType;
    BigDecimal unitPrice;
    Integer quantity;
    BigDecimal subtotal;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    BigDecimal subtotalWithServiceFee;
    Integer taxCategoryId;
    BigDecimal vatPercent;
    BigDecimal vatAmount;
    BigDecimal grossServiceTotal;
}
