package vn.edu.utc.hotel_booking.modules.pricing.pipeline;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricingSummary {
    BigDecimal totalBasePrice;
    BigDecimal totalSurchargeAmount;
    BigDecimal totalRateAdjustmentAmount;
    BigDecimal totalSeasonalAdjustment;
    BigDecimal totalHolidayAdjustment;
    BigDecimal totalDiscountAmount;
    BigDecimal totalRoomNetAmount;
    BigDecimal totalRoomServiceFee;
    BigDecimal totalRoomVat;
    BigDecimal totalRoomGrossAmount;
    BigDecimal totalServiceSubtotal;
    BigDecimal totalServiceFee;
    BigDecimal totalServiceVat;
    BigDecimal totalServiceGrossAmount;
    BigDecimal grandTotal;
}
