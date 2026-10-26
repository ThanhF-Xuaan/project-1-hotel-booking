package vn.edu.utc.hotel_booking.modules.identity.personalization.dto;

import vn.edu.utc.hotel_booking.modules.identity.personalization.enums.PersonalizationConsentStatus;

import java.time.OffsetDateTime;

public record PersonalizationConsentView(
        Short hotelId,
        PersonalizationConsentStatus status,
        boolean granted,
        long revision,
        long bindingVersion,
        String purpose,
        String currentPolicyVersion,
        String acceptedPolicyVersion,
        OffsetDateTime updatedAt,
        int historyLookbackMonths,
        String noticeText,
        String privacyPolicyUrl,
        String dataMode
) {
}
