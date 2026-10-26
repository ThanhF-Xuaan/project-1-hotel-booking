package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

/** Identity and consent facts resolved server-side; never serialized by the recommendation API. */
public record PersonalizationAuthorizationSnapshot(
        PersonalizationAuthorizationStatus status,
        Long guestId,
        long bindingVersion,
        long consentRevision
) {
}
