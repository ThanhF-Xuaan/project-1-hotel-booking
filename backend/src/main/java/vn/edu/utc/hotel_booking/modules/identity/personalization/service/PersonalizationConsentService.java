package vn.edu.utc.hotel_booking.modules.identity.personalization.service;

import vn.edu.utc.hotel_booking.modules.identity.personalization.dto.PersonalizationConsentUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.personalization.dto.PersonalizationConsentView;

public interface PersonalizationConsentService {

    PersonalizationConsentView getCurrentConsent(String issuer, String subject, Short hotelId);

    PersonalizationConsentView updateConsent(
            String issuer,
            String subject,
            PersonalizationConsentUpdateRequest request
    );
}
