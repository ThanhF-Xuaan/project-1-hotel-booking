package vn.edu.utc.hotel_booking.modules.identity.personalization.dto;

import vn.edu.utc.hotel_booking.modules.identity.personalization.error.PersonalizationConsentError;

public record PersonalizationConsentErrorDetail(String reasonCode, boolean retryable) {

    public static PersonalizationConsentErrorDetail from(PersonalizationConsentError error) {
        return new PersonalizationConsentErrorDetail(error.name(), error.isRetryable());
    }
}
