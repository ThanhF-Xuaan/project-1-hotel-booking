package vn.edu.utc.hotel_booking.modules.ai.advisor.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AmbiguityDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaEvidenceDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationResult {
    boolean valid;
    AdvisorStatus status;
    String reply;
    RoomSearchCriteriaDto criteria;

    @Builder.Default
    List<String> missingFields = new ArrayList<>();

    @Builder.Default
    List<AmbiguityDto> ambiguities = new ArrayList<>();

    @Builder.Default
    List<CriteriaEvidenceDto> provenance = new ArrayList<>();
}
