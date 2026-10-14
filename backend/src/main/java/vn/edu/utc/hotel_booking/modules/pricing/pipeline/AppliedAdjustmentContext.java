package vn.edu.utc.hotel_booking.modules.pricing.pipeline;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppliedAdjustmentContext {

    String ruleCode;
    String ruleName;
    String adjustmentType;
    BigDecimal adjustmentValue;
    BigDecimal appliedAmount;
}
