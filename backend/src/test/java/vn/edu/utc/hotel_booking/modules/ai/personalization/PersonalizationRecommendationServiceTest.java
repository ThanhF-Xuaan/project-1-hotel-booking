package vn.edu.utc.hotel_booking.modules.ai.personalization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationAuthorizationSnapshot;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationAuthorizationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationCatalogSnapshot;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRecommendationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRoomType;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationStayRecord;
import vn.edu.utc.hotel_booking.modules.ai.personalization.error.PersonalizationRecommendationError;
import vn.edu.utc.hotel_booking.modules.ai.personalization.error.PersonalizationRecommendationException;
import vn.edu.utc.hotel_booking.modules.ai.personalization.port.PersonalizationAuthorizationPort;
import vn.edu.utc.hotel_booking.modules.ai.personalization.port.PersonalizationCatalogPort;
import vn.edu.utc.hotel_booking.modules.ai.personalization.port.PersonalizationHistoryPort;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.PersonalizationRecommendationService;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.RoomAffinityCore;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalizationRecommendationServiceTest {

    private static final String ISSUER = "https://identity.example.test/realms/hotel";
    private static final String SUBJECT = "customer-subject";
    private static final Instant AS_OF = Instant.parse("2026-10-26T10:00:00Z");
    private static final PersonalizationAuthorizationSnapshot AUTHORIZED = new PersonalizationAuthorizationSnapshot(
            PersonalizationAuthorizationStatus.AUTHORIZED, 5L, 1L, 3L);

    @Mock
    private PersonalizationAuthorizationPort authorizationPort;
    @Mock
    private PersonalizationHistoryPort historyPort;
    @Mock
    private PersonalizationCatalogPort catalogPort;

    private PersonalizationRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new PersonalizationRecommendationService(
                authorizationPort,
                historyPort,
                catalogPort,
                new RoomAffinityCore(),
                Clock.fixed(AS_OF, ZoneOffset.UTC)
        );
    }

    @Test
    void doesNotReadHistoryOrCatalogWithoutCurrentConsent() {
        when(authorizationPort.authorize(ISSUER, SUBJECT, 1)).thenReturn(new PersonalizationAuthorizationSnapshot(
                PersonalizationAuthorizationStatus.CONSENT_REQUIRED, 5L, 1L, 2L));

        var response = service.recommend(ISSUER, SUBJECT, 1, 3);

        assertThat(response.status()).isEqualTo(PersonalizationRecommendationStatus.CONSENT_REQUIRED);
        assertThat(response.reasonCode()).isEqualTo("CONSENT_REQUIRED");
        assertThat(response.roomTypeSuggestions()).isEmpty();
        assertThat(response.nonBindingSuggestion()).isTrue();
        assertThat(response.pricingOrPromotionMutated()).isFalse();
        verifyNoInteractions(historyPort, catalogPort);
        verify(authorizationPort, never()).revalidate(ISSUER, SUBJECT, 1, 1, 2);
    }

    @Test
    void usesServerClockAndReturnsOnlyCurrentCatalogSuggestions() {
        when(authorizationPort.authorize(ISSUER, SUBJECT, 1)).thenReturn(AUTHORIZED);
        when(historyPort.findEligibleStays(5L, 1,
                java.time.OffsetDateTime.parse("2025-10-26T00:00:00+07:00"),
                java.time.OffsetDateTime.parse("2026-10-26T10:00:00Z")))
                .thenReturn(List.of(
                        stay(1L, 101), stay(2L, 101), stay(3L, 102)
                ));
        when(catalogPort.findCurrentCatalog(1)).thenReturn(new PersonalizationCatalogSnapshot(true,
                List.of(roomType(101), roomType(102))));
        when(authorizationPort.revalidate(ISSUER, SUBJECT, 1, 1, 3)).thenReturn(AUTHORIZED);

        var response = service.recommend(ISSUER, SUBJECT, 1, 2);

        assertThat(response.status()).isEqualTo(PersonalizationRecommendationStatus.PERSONALIZED_READY);
        assertThat(response.hotelId()).isEqualTo(1);
        assertThat(response.dataMode()).isEqualTo("LOCAL_SYNTHETIC");
        assertThat(response.roomTypeSuggestions()).extracting(item -> item.hotelRoomTypeId())
                .containsExactly(101, 102);
        assertThat(response.roomTypeSuggestions()).extracting(item -> item.affinityScore())
                .containsExactly(new java.math.BigDecimal("1.0000"), new java.math.BigDecimal("0.5000"));
        assertThat(response.reasonCode()).isNull();
        assertThat(response.pricingOrPromotionMutated()).isFalse();
        verify(authorizationPort).revalidate(ISSUER, SUBJECT, 1, 1, 3);
    }

    @Test
    void returnsColdStartForEmptyHistoryAndStillRevalidatesConsent() {
        when(authorizationPort.authorize(ISSUER, SUBJECT, 1)).thenReturn(AUTHORIZED);
        when(historyPort.findEligibleStays(5L, 1,
                java.time.OffsetDateTime.parse("2025-10-26T00:00:00+07:00"),
                java.time.OffsetDateTime.parse("2026-10-26T10:00:00Z"))).thenReturn(List.of());
        when(authorizationPort.revalidate(ISSUER, SUBJECT, 1, 1, 3)).thenReturn(AUTHORIZED);

        var response = service.recommend(ISSUER, SUBJECT, 1, 3);

        assertThat(response.status()).isEqualTo(PersonalizationRecommendationStatus.COLD_START_FALLBACK);
        assertThat(response.reasonCode()).isEqualTo("NO_ELIGIBLE_HISTORY");
        assertThat(response.roomTypeSuggestions()).isEmpty();
        verifyNoInteractions(catalogPort);
        verify(authorizationPort).revalidate(ISSUER, SUBJECT, 1, 1, 3);
    }

    @Test
    void dropsComputedSuggestionsWhenFinalAuthorizationFindsRevokedConsent() {
        when(authorizationPort.authorize(ISSUER, SUBJECT, 1)).thenReturn(AUTHORIZED);
        when(historyPort.findEligibleStays(5L, 1,
                java.time.OffsetDateTime.parse("2025-10-26T00:00:00+07:00"),
                java.time.OffsetDateTime.parse("2026-10-26T10:00:00Z"))).thenReturn(List.of(stay(1L, 101)));
        when(catalogPort.findCurrentCatalog(1)).thenReturn(new PersonalizationCatalogSnapshot(true, List.of(roomType(101))));
        when(authorizationPort.revalidate(ISSUER, SUBJECT, 1, 1, 3)).thenReturn(new PersonalizationAuthorizationSnapshot(
                PersonalizationAuthorizationStatus.CONSENT_REQUIRED, 5L, 1L, 4L));

        var response = service.recommend(ISSUER, SUBJECT, 1, 3);

        assertThat(response.status()).isEqualTo(PersonalizationRecommendationStatus.CONSENT_REQUIRED);
        assertThat(response.roomTypeSuggestions()).isEmpty();
        assertThat(response.reasonCode()).isEqualTo("CONSENT_REQUIRED");
    }

    @Test
    void mapsFinalBindingAndRevisionChangesToTheirSpecificConflicts() {
        when(authorizationPort.authorize(ISSUER, SUBJECT, 1)).thenReturn(AUTHORIZED);
        when(historyPort.findEligibleStays(5L, 1,
                java.time.OffsetDateTime.parse("2025-10-26T00:00:00+07:00"),
                java.time.OffsetDateTime.parse("2026-10-26T10:00:00Z"))).thenReturn(List.of());
        when(authorizationPort.revalidate(ISSUER, SUBJECT, 1, 1, 3)).thenReturn(new PersonalizationAuthorizationSnapshot(
                PersonalizationAuthorizationStatus.IDENTITY_BINDING_CONFLICT, 6L, 2L, 0L));

        assertThatThrownBy(() -> service.recommend(ISSUER, SUBJECT, 1, 3))
                .isInstanceOfSatisfying(PersonalizationRecommendationException.class,
                        exception -> assertThat(exception.error())
                                .isEqualTo(PersonalizationRecommendationError.IDENTITY_BINDING_CONFLICT));
    }

    @Test
    void mapsProviderFailuresAndInactiveHotelsWithoutClaimingColdStart() {
        when(authorizationPort.authorize(ISSUER, SUBJECT, 1)).thenReturn(AUTHORIZED);
        when(historyPort.findEligibleStays(5L, 1,
                java.time.OffsetDateTime.parse("2025-10-26T00:00:00+07:00"),
                java.time.OffsetDateTime.parse("2026-10-26T10:00:00Z")))
                .thenThrow(new IllegalStateException("synthetic provider outage"));

        assertThatThrownBy(() -> service.recommend(ISSUER, SUBJECT, 1, 3))
                .isInstanceOfSatisfying(PersonalizationRecommendationException.class,
                        exception -> assertThat(exception.error())
                                .isEqualTo(PersonalizationRecommendationError.DEPENDENCY_UNAVAILABLE));

        doReturn(List.of(stay(1L, 101))).when(historyPort).findEligibleStays(5L, 1,
                java.time.OffsetDateTime.parse("2025-10-26T00:00:00+07:00"),
                java.time.OffsetDateTime.parse("2026-10-26T10:00:00Z"));
        when(catalogPort.findCurrentCatalog(1)).thenReturn(new PersonalizationCatalogSnapshot(false, List.of()));
        assertThatThrownBy(() -> service.recommend(ISSUER, SUBJECT, 1, 3))
                .isInstanceOfSatisfying(PersonalizationRecommendationException.class,
                        exception -> assertThat(exception.error())
                                .isEqualTo(PersonalizationRecommendationError.HOTEL_NOT_FOUND));
    }

    private PersonalizationStayRecord stay(long id, int roomTypeId) {
        return new PersonalizationStayRecord(
                id,
                1,
                roomTypeId,
                java.time.OffsetDateTime.parse("2026-10-01T16:00:00+07:00"),
                java.time.OffsetDateTime.parse("2026-10-03T12:00:00+07:00")
        );
    }

    private PersonalizationRoomType roomType(int id) {
        return new PersonalizationRoomType(id, "TYPE-" + id, "Room " + id);
    }
}
