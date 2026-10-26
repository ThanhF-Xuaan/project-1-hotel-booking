package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import vn.edu.utc.hotel_booking.modules.ai.personalization.error.PersonalizationRecommendationError;

public record PersonalizationRecommendationErrorDetail(String reasonCode, boolean retryable) {

    public static PersonalizationRecommendationErrorDetail from(PersonalizationRecommendationError error) {
        return new PersonalizationRecommendationErrorDetail(error.name(), error.retryable());
    }
}
