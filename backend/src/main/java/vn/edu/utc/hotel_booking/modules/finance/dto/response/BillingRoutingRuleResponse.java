package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.ChargeCategory;
import vn.edu.utc.hotel_booking.modules.finance.entity.RoutingTarget;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BillingRoutingRuleResponse {

    Long id;
    Long masterFolioId;
    ChargeCategory chargeCategory;
    RoutingTarget routeTarget;
    BigDecimal maxCoveredAmount;
    Boolean isActive;
    OffsetDateTime createdAt;
}
