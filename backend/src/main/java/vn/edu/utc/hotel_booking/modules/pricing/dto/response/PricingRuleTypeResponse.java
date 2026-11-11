package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricingRuleTypeResponse {

    String code;
    String displayName;
    String description;
    Short priority;
    String status;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
