package vn.edu.utc.hotel_booking.modules.ai.personalization.service;

import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationAuthorizationSnapshot;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationAuthorizationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationCatalogSnapshot;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRecommendationResponse;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRecommendationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRoomTypeSuggestion;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationStayRecord;
import vn.edu.utc.hotel_booking.modules.ai.personalization.error.PersonalizationRecommendationError;
import vn.edu.utc.hotel_booking.modules.ai.personalization.error.PersonalizationRecommendationException;
import vn.edu.utc.hotel_booking.modules.ai.personalization.port.PersonalizationAuthorizationPort;
import vn.edu.utc.hotel_booking.modules.ai.personalization.port.PersonalizationCatalogPort;
import vn.edu.utc.hotel_booking.modules.ai.personalization.port.PersonalizationHistoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class PersonalizationRecommendationService {

    public static final String DATA_MODE = "LOCAL_SYNTHETIC";
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PersonalizationAuthorizationPort authorizationPort;
    private final PersonalizationHistoryPort historyPort;
    private final PersonalizationCatalogPort catalogPort;
    private final RoomAffinityCore affinityCore;
    private final Clock clock;

    public PersonalizationRecommendationService(
            PersonalizationAuthorizationPort authorizationPort,
            PersonalizationHistoryPort historyPort,
            PersonalizationCatalogPort catalogPort,
            RoomAffinityCore affinityCore,
            Clock clock
    ) {
        this.authorizationPort = authorizationPort;
        this.historyPort = historyPort;
        this.catalogPort = catalogPort;
        this.affinityCore = affinityCore;
        this.clock = clock;
    }

    public PersonalizationRecommendationResponse recommend(
            String issuer,
            String subject,
            Integer hotelId,
            int maxSuggestions
    ) {
        if (hotelId == null || hotelId < 1 || hotelId > Short.MAX_VALUE || maxSuggestions < 1 || maxSuggestions > 10) {
            throw error(PersonalizationRecommendationError.INVALID_REQUEST);
        }
        Instant asOf = clock.instant();
        LocalDate referenceDate = asOf.atZone(HOTEL_ZONE).toLocalDate();
        PersonalizationAuthorizationSnapshot initial = authorizationPort.authorize(issuer, subject, hotelId);
        requireInitialIdentityAndHotel(initial);
        if (initial.status() == PersonalizationAuthorizationStatus.CONSENT_REQUIRED) {
            return consentRequired(hotelId);
        }

        OffsetDateTime fromInclusive = referenceDate.minusMonths(12)
                .atStartOfDay(HOTEL_ZONE)
                .toOffsetDateTime();
        OffsetDateTime toInclusive = asOf.atOffset(ZoneOffset.UTC);

        List<PersonalizationStayRecord> history;
        try {
            history = historyPort.findEligibleStays(initial.guestId(), hotelId, fromInclusive, toInclusive);
        } catch (RuntimeException exception) {
            throw unavailable();
        }
        if (history == null) {
            throw unavailable();
        }

        PersonalizationRecommendationResponse computed;
        if (history.isEmpty()) {
            computed = coldStart(hotelId, "NO_ELIGIBLE_HISTORY");
        } else {
            PersonalizationCatalogSnapshot catalog;
            try {
                catalog = catalogPort.findCurrentCatalog(hotelId);
            } catch (RuntimeException exception) {
                throw unavailable();
            }
            if (catalog == null) {
                throw unavailable();
            }
            if (!catalog.hotelEligible()) {
                throw error(PersonalizationRecommendationError.HOTEL_NOT_FOUND);
            }
            if (catalog.roomTypes().isEmpty()) {
                computed = coldStart(hotelId, "NO_PUBLIC_ROOM_TYPES");
            } else {
                List<PersonalizationRoomTypeSuggestion> suggestions = affinityCore.rank(
                        history, catalog.roomTypes(), hotelId, referenceDate, asOf, maxSuggestions
                );
                computed = suggestions.isEmpty()
                        ? coldStart(hotelId, "NO_ELIGIBLE_HISTORY")
                        : ready(hotelId, suggestions);
            }
        }

        PersonalizationAuthorizationSnapshot finalAuthorization = authorizationPort.revalidate(
                issuer,
                subject,
                hotelId,
                initial.bindingVersion(),
                initial.consentRevision()
        );
        requireFinalIdentityAndHotel(finalAuthorization);
        if (finalAuthorization.status() == PersonalizationAuthorizationStatus.CONSENT_REQUIRED) {
            return consentRequired(hotelId);
        }
        return computed;
    }

    private void requireInitialIdentityAndHotel(PersonalizationAuthorizationSnapshot authorization) {
        if (authorization == null || authorization.status() == null) {
            throw unavailable();
        }
        switch (authorization.status()) {
            case IDENTITY_LINK_REQUIRED -> throw error(PersonalizationRecommendationError.IDENTITY_LINK_REQUIRED);
            case HOTEL_NOT_FOUND -> throw error(PersonalizationRecommendationError.HOTEL_NOT_FOUND);
            case IDENTITY_BINDING_CONFLICT -> throw error(PersonalizationRecommendationError.IDENTITY_BINDING_CONFLICT);
            case CONSENT_REVISION_CONFLICT -> throw error(PersonalizationRecommendationError.CONSENT_REVISION_CONFLICT);
            case AUTHORIZED, CONSENT_REQUIRED -> { }
        }
        if (authorization.status() == PersonalizationAuthorizationStatus.AUTHORIZED
                && (authorization.guestId() == null || authorization.guestId() < 1
                || authorization.bindingVersion() < 1 || authorization.consentRevision() < 1)) {
            throw unavailable();
        }
    }

    private void requireFinalIdentityAndHotel(PersonalizationAuthorizationSnapshot authorization) {
        if (authorization == null || authorization.status() == null) {
            throw unavailable();
        }
        switch (authorization.status()) {
            case IDENTITY_LINK_REQUIRED -> throw error(PersonalizationRecommendationError.IDENTITY_LINK_REQUIRED);
            case HOTEL_NOT_FOUND -> throw error(PersonalizationRecommendationError.HOTEL_NOT_FOUND);
            case IDENTITY_BINDING_CONFLICT -> throw error(PersonalizationRecommendationError.IDENTITY_BINDING_CONFLICT);
            case CONSENT_REVISION_CONFLICT -> throw error(PersonalizationRecommendationError.CONSENT_REVISION_CONFLICT);
            case AUTHORIZED, CONSENT_REQUIRED -> { }
        }
        if (authorization.status() == PersonalizationAuthorizationStatus.AUTHORIZED
                && (authorization.guestId() == null || authorization.guestId() < 1
                || authorization.bindingVersion() < 1 || authorization.consentRevision() < 1)) {
            throw unavailable();
        }
    }

    private PersonalizationRecommendationResponse consentRequired(Integer hotelId) {
        return new PersonalizationRecommendationResponse(
                PersonalizationRecommendationStatus.CONSENT_REQUIRED,
                hotelId,
                DATA_MODE,
                List.of(),
                true,
                false,
                "CONSENT_REQUIRED"
        );
    }

    private PersonalizationRecommendationResponse coldStart(Integer hotelId, String reasonCode) {
        return new PersonalizationRecommendationResponse(
                PersonalizationRecommendationStatus.COLD_START_FALLBACK,
                hotelId,
                DATA_MODE,
                List.of(),
                true,
                false,
                reasonCode
        );
    }

    private PersonalizationRecommendationResponse ready(
            Integer hotelId,
            List<PersonalizationRoomTypeSuggestion> suggestions
    ) {
        return new PersonalizationRecommendationResponse(
                PersonalizationRecommendationStatus.PERSONALIZED_READY,
                hotelId,
                DATA_MODE,
                suggestions,
                true,
                false,
                null
        );
    }

    private PersonalizationRecommendationException unavailable() {
        return error(PersonalizationRecommendationError.DEPENDENCY_UNAVAILABLE);
    }

    private PersonalizationRecommendationException error(PersonalizationRecommendationError error) {
        return new PersonalizationRecommendationException(error);
    }
}
