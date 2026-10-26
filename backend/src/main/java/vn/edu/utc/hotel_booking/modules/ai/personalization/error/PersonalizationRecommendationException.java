package vn.edu.utc.hotel_booking.modules.ai.personalization.error;

public class PersonalizationRecommendationException extends RuntimeException {

    private final PersonalizationRecommendationError error;

    public PersonalizationRecommendationException(PersonalizationRecommendationError error) {
        super(error.message());
        this.error = error;
    }

    public PersonalizationRecommendationError error() {
        return error;
    }
}
