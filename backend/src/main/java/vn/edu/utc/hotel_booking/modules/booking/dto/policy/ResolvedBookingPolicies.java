package vn.edu.utc.hotel_booking.modules.booking.dto.policy;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationPolicy;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPolicy;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResolvedBookingPolicies {
    CancellationPolicy cancellationPolicy;
    NoShowPolicy noShowPolicy;
    String cancellationPolicySnapshotJson;
    String noShowPolicySnapshotJson;
    CancellationPolicySnapshotDto cancellationSnapshot;
    NoShowPolicySnapshotDto noShowSnapshot;
}
