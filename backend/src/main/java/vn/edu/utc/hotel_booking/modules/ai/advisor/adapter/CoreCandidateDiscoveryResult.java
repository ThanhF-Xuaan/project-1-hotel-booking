package vn.edu.utc.hotel_booking.modules.ai.advisor.adapter;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.PriceQuoteDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CoverageReason;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.CandidateAvailability;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.RoomCandidate;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoreCandidateDiscoveryResult {
    @Builder.Default
    List<RoomCandidate> candidates = new ArrayList<>();

    @Builder.Default
    Map<Integer, List<CandidateAvailability>> availabilityByRoomTypeId = new HashMap<>();

    @Builder.Default
    Map<Integer, PriceQuoteDto> quoteByRoomTypeId = new HashMap<>();

    @Builder.Default
    boolean catalogComplete = true;

    @Builder.Default
    Set<Integer> failedRoomTypeIds = new HashSet<>();

    @Builder.Default
    List<CoverageReason> coverageReasons = new ArrayList<>();

    @Builder.Default
    boolean allFailed = false;
}
