package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.CancellationTierDto;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationFeeType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancellationPolicyResponse {

    Integer id;
    String policyCode;
    String name;
    BookingType bookingType;
    String description;
    Integer freeCancellationBeforeDays;
    CancellationFeeType cancellationFeeType;
    BigDecimal defaultPenaltyPercent;

    @Builder.Default
    List<CancellationTierDto> tiers = new ArrayList<>();

    Boolean isActive;
    Boolean isDeleted;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    String createdBy;
    String updatedBy;
}
