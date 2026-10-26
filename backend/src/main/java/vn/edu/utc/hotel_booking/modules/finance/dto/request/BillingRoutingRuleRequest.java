package vn.edu.utc.hotel_booking.modules.finance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.ChargeCategory;
import vn.edu.utc.hotel_booking.modules.finance.entity.RoutingTarget;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BillingRoutingRuleRequest {

    @NotNull(message = "Danh mục chi phí không được để trống")
    ChargeCategory chargeCategory;

    @NotNull(message = "Mục tiêu định tuyến không được để trống")
    RoutingTarget routeTarget;

    BigDecimal maxCoveredAmount;

    @Builder.Default
    Boolean isActive = true;
}
