package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SurchargeRuleResponse {

    Integer id;

    // Room Type & Hotel Info
    Integer hotelRoomTypeId;
    String roomTypeName;
    Short hotelId;
    String hotelName;

    // Age Policy Info
    Short agePolicyId;
    String agePolicyGuestType;
    Short agePolicyMinAge;
    Short agePolicyMaxAge;

    // Rule Details
    String ruleType;
    String pricingType;
    String conditions;
    String adjustmentType;
    BigDecimal adjustmentValue;
    LocalDate startDate;
    LocalDate endDate;
    String status;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
