package vn.edu.utc.hotel_booking.modules.booking.dto.policy;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationFeeType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancellationPolicySnapshotDto {

    Integer policyId;
    String policyCode;
    String name;
    BookingType bookingType;
    Integer freeCancellationBeforeDays;
    CancellationFeeType cancellationFeeType;
    BigDecimal defaultPenaltyPercent;

    @Builder.Default
    List<CancellationTierDto> tiers = new ArrayList<>();

    Boolean overrideApplied;
    String overrideReason;
}
