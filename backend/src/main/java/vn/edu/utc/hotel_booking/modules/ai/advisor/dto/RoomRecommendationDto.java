package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomRecommendationDto {
    String recommendationId;
    int rank;
    RoomSummaryDto room;
    PriceQuoteDto price;

    @Builder.Default
    List<RecommendationReasonDto> reasons = new ArrayList<>();

    RecommendationActionDto action;
    StayIntervalDto stay;
}
