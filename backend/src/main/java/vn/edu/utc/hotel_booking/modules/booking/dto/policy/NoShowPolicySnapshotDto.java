package vn.edu.utc.hotel_booking.modules.booking.dto.policy;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPenaltyType;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoShowPolicySnapshotDto {

    Integer policyId;
    String policyCode;
    String name;
    BookingType bookingType;
    LocalTime releaseRoomTime;
    NoShowPenaltyType penaltyType;
    BigDecimal penaltyValue;
    Boolean overrideApplied;
    String overrideReason;
}
