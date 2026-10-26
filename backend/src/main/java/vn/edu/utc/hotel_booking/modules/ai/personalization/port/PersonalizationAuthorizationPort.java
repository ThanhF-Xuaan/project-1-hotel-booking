package vn.edu.utc.hotel_booking.modules.ai.personalization.port;

import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationAuthorizationSnapshot;

/** Read-only identity/consent boundary implemented by Identity. */
public interface PersonalizationAuthorizationPort {

    PersonalizationAuthorizationSnapshot authorize(String issuer, String subject, Integer hotelId);

    PersonalizationAuthorizationSnapshot revalidate(
            String issuer,
            String subject,
            Integer hotelId,
            long expectedBindingVersion,
            long expectedConsentRevision
    );
}
