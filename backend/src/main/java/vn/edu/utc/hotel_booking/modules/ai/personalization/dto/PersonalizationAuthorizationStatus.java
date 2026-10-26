package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

public enum PersonalizationAuthorizationStatus {
    AUTHORIZED,
    CONSENT_REQUIRED,
    IDENTITY_LINK_REQUIRED,
    IDENTITY_BINDING_CONFLICT,
    CONSENT_REVISION_CONFLICT,
    HOTEL_NOT_FOUND
}
