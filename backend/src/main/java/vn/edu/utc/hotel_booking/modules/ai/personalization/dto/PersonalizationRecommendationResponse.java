package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalizationRecommendationResponse(
        PersonalizationRecommendationStatus status,
        Integer hotelId,
        String dataMode,
        List<PersonalizationRoomTypeSuggestion> roomTypeSuggestions,
        boolean nonBindingSuggestion,
        boolean pricingOrPromotionMutated,
        String reasonCode
) {
    public PersonalizationRecommendationResponse {
        roomTypeSuggestions = roomTypeSuggestions == null ? List.of() : List.copyOf(roomTypeSuggestions);
    }
}
