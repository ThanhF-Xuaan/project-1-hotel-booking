package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaEvidenceDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomAdvisorSession {
    UUID sessionId;
    long sessionVersion;
    String ownerKey;
    RoomSearchCriteriaDto criteria;

    @Builder.Default
    List<CriteriaEvidenceDto> criteriaProvenance = new ArrayList<>();

    AdvisorResultDto lastResult;

    @Builder.Default
    Instant lastAccessedAt = Instant.now();
}
