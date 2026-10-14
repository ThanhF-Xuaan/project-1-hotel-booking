package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdvisorResultDto {
    @Builder.Default
    String contractVersion = "2.0";

    UUID requestId;
    UUID sessionId;
    long sessionVersion;
    AdvisorStatus status;
    String reply;
    RoomSearchCriteriaDto criteria;

    @Builder.Default
    List<String> missingFields = new ArrayList<>();

    @Builder.Default
    List<RoomRecommendationDto> recommendations = new ArrayList<>();

    SearchCoverageDto coverage;

    @Builder.Default
    String knowledgeStatus = "DISABLED";

    @Builder.Default
    List<Object> sources = Collections.emptyList();

    @Builder.Default
    List<String> warnings = new ArrayList<>();

    @Builder.Default
    List<CriteriaEvidenceDto> criteriaProvenance = new ArrayList<>();

    @Builder.Default
    List<AmbiguityDto> ambiguities = new ArrayList<>();
}
