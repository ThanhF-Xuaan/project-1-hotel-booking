package vn.edu.utc.hotel_booking.modules.identity.personalization.error;

public class PersonalizationConsentException extends RuntimeException {

    private final PersonalizationConsentError error;

    public PersonalizationConsentException(PersonalizationConsentError error) {
        super(error.getMessage());
        this.error = error;
    }

    public PersonalizationConsentError getError() {
        return error;
    }
}
