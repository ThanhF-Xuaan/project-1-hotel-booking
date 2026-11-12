package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscountRuleTypeResponse {

    String code;
    String displayName;
    Short priority;
    String status;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
