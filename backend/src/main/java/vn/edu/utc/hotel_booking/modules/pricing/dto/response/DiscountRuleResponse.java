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
public class DiscountRuleResponse {

    Integer id;
    Integer hotelRoomTypeId;
    String hotelRoomTypeName;
    Short hotelId;
    String hotelName;
    Integer campaignId;
    String campaignName;
    String ruleTypeCode;
    String ruleTypeDisplayName;
    String conditions;
    String discountType;
    BigDecimal discountValue;
    LocalDate startDate;
    LocalDate endDate;
    String status;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
