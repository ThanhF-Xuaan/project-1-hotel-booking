package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ComparisonFocus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ComparisonStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagCitationDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-criteria room comparison matrix with deterministic price deltas and trade-off advice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomComparisonMatrixDto {

    ComparisonStatus status;

    ComparisonFocus focus;

    @Builder.Default
    List<RoomComparisonItemDto> items = new ArrayList<>();

    @Builder.Default
    List<String> sharedFeatureNames = new ArrayList<>();

    String maxSavingsAmount;
    BigDecimal maxSavingsPercent;

    String tradeOffSummary;

    @Builder.Default
    List<RagCitationDto> staticDescriptionCitations = new ArrayList<>();
}
