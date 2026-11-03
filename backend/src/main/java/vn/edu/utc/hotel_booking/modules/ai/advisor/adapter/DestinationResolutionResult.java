package vn.edu.utc.hotel_booking.modules.ai.advisor.adapter;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AmbiguityDto;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DestinationResolutionResult {
    Integer hotelId;
    Short regionId;
    String resolvedName;
    boolean ambiguous;
    DestinationResolutionStatus status;

    /** Supports existing internal builders while keeping an explicit unavailable outcome. */
    public DestinationResolutionStatus getStatus() {
        if (status != null) return status;
        if (ambiguous) return DestinationResolutionStatus.AMBIGUOUS;
        if ((hotelId != null && hotelId > 0) || (regionId != null && regionId > 0)) return DestinationResolutionStatus.RESOLVED;
        return DestinationResolutionStatus.UNRESOLVED;
    }

    @Builder.Default
    List<AmbiguityDto> ambiguities = new ArrayList<>();
}
