package vn.edu.utc.hotel_booking.modules.identity.personalization.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationAuthorizationSnapshot;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationAuthorizationStatus;
import vn.edu.utc.hotel_booking.modules.identity.personalization.dto.PersonalizationConsentUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.personalization.dto.PersonalizationConsentView;
import vn.edu.utc.hotel_booking.modules.identity.personalization.entity.PersonalizationAccountBinding;
import vn.edu.utc.hotel_booking.modules.identity.personalization.entity.PersonalizationConsent;
import vn.edu.utc.hotel_booking.modules.identity.personalization.entity.PersonalizationConsentEvent;
import vn.edu.utc.hotel_booking.modules.identity.personalization.enums.PersonalizationConsentStatus;
import vn.edu.utc.hotel_booking.modules.identity.personalization.error.PersonalizationConsentError;
import vn.edu.utc.hotel_booking.modules.identity.personalization.error.PersonalizationConsentException;
import vn.edu.utc.hotel_booking.modules.identity.personalization.repository.PersonalizationAccountBindingRepository;
import vn.edu.utc.hotel_booking.modules.identity.personalization.repository.PersonalizationConsentEventRepository;
import vn.edu.utc.hotel_booking.modules.identity.personalization.repository.PersonalizationConsentRepository;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.time.OffsetDateTime;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalizationConsentServiceImplTest {

    private static final String ISSUER = "https://identity.example.test/realms/hotel";
    private static final String SUBJECT = "subject-123";

    @Mock
    private PersonalizationAccountBindingRepository bindingRepository;
    @Mock
    private PersonalizationConsentRepository consentRepository;
    @Mock
    private PersonalizationConsentEventRepository eventRepository;
    @Mock
    private HotelService hotelService;

    private PersonalizationConsentServiceImpl service;
    private PersonalizationAccountBinding binding;

    @BeforeEach
    void setUp() {
        service = new PersonalizationConsentServiceImpl(
                bindingRepository, consentRepository, eventRepository, hotelService);
        binding = new PersonalizationAccountBinding();
        binding.setId(81L);
        binding.setIssuer(ISSUER);
        binding.setSubject(SUBJECT);
        binding.setGuestId(5L);
        binding.setBindingVersion(1L);
        binding.setSource("SYNTHETIC_FIXTURE");
        binding.setStatus(PersonalizationAccountBinding.ACTIVE);
    }

    @Test
    void getWithoutConsentReturnsNotGrantedWithoutWriting() {
        when(bindingRepository.findActiveForAccount(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.empty());

        PersonalizationConsentView view = service.getCurrentConsent(ISSUER, SUBJECT, (short) 1);

        assertThat(view.status()).isEqualTo(PersonalizationConsentStatus.NOT_GRANTED);
        assertThat(view.granted()).isFalse();
        assertThat(view.revision()).isZero();
        assertThat(view.bindingVersion()).isEqualTo(1L);
        assertThat(view.acceptedPolicyVersion()).isNull();
        assertThat(view.updatedAt()).isNull();
        verifyNoInteractions(eventRepository);
        verify(consentRepository, never()).saveAndFlush(any());
    }

    @Test
    void authorizationResolvesBindingHotelAndCurrentConsentWithoutWriting() {
        PersonalizationConsent currentConsent = consent(true, 3, PersonalizationConsentServiceImpl.CURRENT_POLICY_VERSION);
        when(bindingRepository.findActiveForAccount(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.of(currentConsent));

        PersonalizationAuthorizationSnapshot authorization = service.authorize(ISSUER, SUBJECT, 1);

        assertThat(authorization).isEqualTo(new PersonalizationAuthorizationSnapshot(
                PersonalizationAuthorizationStatus.AUTHORIZED, 5L, 1L, 3L));
        verifyNoInteractions(eventRepository);
        verify(consentRepository, never()).saveAndFlush(any());
    }

    @Test
    void authorizationWithoutBindingDoesNotReadHotelOrConsent() {
        when(bindingRepository.findActiveForAccount(ISSUER, SUBJECT)).thenReturn(Optional.empty());

        PersonalizationAuthorizationSnapshot authorization = service.authorize(ISSUER, SUBJECT, 1);

        assertThat(authorization.status()).isEqualTo(PersonalizationAuthorizationStatus.IDENTITY_LINK_REQUIRED);
        verifyNoInteractions(hotelService, consentRepository, eventRepository);
    }

    @Test
    void authorizationWithoutCurrentConsentReturnsConsentRequired() {
        when(bindingRepository.findActiveForAccount(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.empty());

        PersonalizationAuthorizationSnapshot authorization = service.authorize(ISSUER, SUBJECT, 1);

        assertThat(authorization.status()).isEqualTo(PersonalizationAuthorizationStatus.CONSENT_REQUIRED);
        assertThat(authorization.guestId()).isEqualTo(5L);
        assertThat(authorization.consentRevision()).isZero();
        verifyNoInteractions(eventRepository);
    }

    @Test
    void finalAuthorizationDetectsRebindBeforeReadingConsent() {
        binding.setBindingVersion(2L);
        when(bindingRepository.findActiveForAccount(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));

        PersonalizationAuthorizationSnapshot authorization = service.revalidate(ISSUER, SUBJECT, 1, 1, 4);

        assertThat(authorization.status()).isEqualTo(PersonalizationAuthorizationStatus.IDENTITY_BINDING_CONFLICT);
        verifyNoInteractions(hotelService, consentRepository, eventRepository);
    }

    @Test
    void finalAuthorizationObservesRevokedConsentBeforeReturningResults() {
        PersonalizationConsent revoked = consent(false, 4, PersonalizationConsentServiceImpl.CURRENT_POLICY_VERSION);
        when(bindingRepository.findActiveForAccount(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.of(revoked));

        PersonalizationAuthorizationSnapshot authorization = service.revalidate(ISSUER, SUBJECT, 1, 1, 3);

        assertThat(authorization.status()).isEqualTo(PersonalizationAuthorizationStatus.CONSENT_REQUIRED);
        assertThat(authorization.consentRevision()).isEqualTo(4L);
    }

    @Test
    void finalAuthorizationDetectsRevisionChangeWhenConsentRemainsGranted() {
        PersonalizationConsent changed = consent(true, 4, PersonalizationConsentServiceImpl.CURRENT_POLICY_VERSION);
        when(bindingRepository.findActiveForAccount(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.of(changed));

        PersonalizationAuthorizationSnapshot authorization = service.revalidate(ISSUER, SUBJECT, 1, 1, 3);

        assertThat(authorization.status()).isEqualTo(PersonalizationAuthorizationStatus.CONSENT_REVISION_CONFLICT);
        assertThat(authorization.consentRevision()).isEqualTo(4L);
    }

    @Test
    void recommendationAuthorizationAlwaysStartsFreshReadOnlyTransactions() throws Exception {
        Method authorize = PersonalizationConsentServiceImpl.class.getMethod(
                "authorize", String.class, String.class, Integer.class);
        Method revalidate = PersonalizationConsentServiceImpl.class.getMethod(
                "revalidate", String.class, String.class, Integer.class, long.class, long.class);

        assertThat(authorize.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(revalidate.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(authorize.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(revalidate.getAnnotation(Transactional.class).readOnly()).isTrue();
    }

    @Test
    void grantWritesStateAndEventWithSameRevisionAndBindingVersion() {
        when(bindingRepository.findActiveForAccountForUpdate(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.empty());

        PersonalizationConsentView view = service.updateConsent(ISSUER, SUBJECT, grantRequest(0, 1));

        ArgumentCaptor<PersonalizationConsent> stateCaptor = ArgumentCaptor.forClass(PersonalizationConsent.class);
        ArgumentCaptor<PersonalizationConsentEvent> eventCaptor = ArgumentCaptor.forClass(PersonalizationConsentEvent.class);
        InOrder writeOrder = inOrder(consentRepository, eventRepository);
        writeOrder.verify(consentRepository).saveAndFlush(stateCaptor.capture());
        writeOrder.verify(eventRepository).saveAndFlush(eventCaptor.capture());

        PersonalizationConsent state = stateCaptor.getValue();
        PersonalizationConsentEvent event = eventCaptor.getValue();
        assertThat(view.status()).isEqualTo(PersonalizationConsentStatus.GRANTED);
        assertThat(view.revision()).isEqualTo(1L);
        assertThat(state.isGranted()).isTrue();
        assertThat(state.getPolicyVersion()).isEqualTo(PersonalizationConsentServiceImpl.CURRENT_POLICY_VERSION);
        assertThat(event.getBindingVersion()).isEqualTo(1L);
        assertThat(event.getRevision()).isEqualTo(state.getRevision());
        assertThat(event.isGranted()).isTrue();
        assertThat(event.getPolicyVersion()).isEqualTo(state.getPolicyVersion());
    }

    @Test
    void revokeBeforeAnyGrantCreatesFirstRevisionAndEventWithoutPolicy() {
        when(bindingRepository.findActiveForAccountForUpdate(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.empty());

        PersonalizationConsentView view = service.updateConsent(ISSUER, SUBJECT, revokeRequest(0, 1));

        ArgumentCaptor<PersonalizationConsent> stateCaptor = ArgumentCaptor.forClass(PersonalizationConsent.class);
        ArgumentCaptor<PersonalizationConsentEvent> eventCaptor = ArgumentCaptor.forClass(PersonalizationConsentEvent.class);
        verify(consentRepository).saveAndFlush(stateCaptor.capture());
        verify(eventRepository).saveAndFlush(eventCaptor.capture());
        assertThat(view.status()).isEqualTo(PersonalizationConsentStatus.NOT_GRANTED);
        assertThat(view.revision()).isEqualTo(1L);
        assertThat(stateCaptor.getValue().getPolicyVersion()).isNull();
        assertThat(eventCaptor.getValue().getPolicyVersion()).isNull();
        assertThat(eventCaptor.getValue().isGranted()).isFalse();
    }

    @Test
    void revokeRetainsPreviouslyAcceptedPolicyVersion() {
        PersonalizationConsent previous = consent(true, 3L,
                PersonalizationConsentServiceImpl.CURRENT_POLICY_VERSION);
        when(bindingRepository.findActiveForAccountForUpdate(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.of(previous));

        PersonalizationConsentView view = service.updateConsent(ISSUER, SUBJECT, revokeRequest(3, 1));

        assertThat(view.status()).isEqualTo(PersonalizationConsentStatus.NOT_GRANTED);
        assertThat(view.granted()).isFalse();
        assertThat(view.revision()).isEqualTo(4L);
        assertThat(view.acceptedPolicyVersion()).isEqualTo(PersonalizationConsentServiceImpl.CURRENT_POLICY_VERSION);
        assertThat(view.updatedAt()).isNotNull();
        verify(consentRepository).saveAndFlush(previous);
        verify(eventRepository).saveAndFlush(any(PersonalizationConsentEvent.class));
    }

    @Test
    void oldGrantedPolicyRequiresReconsentWithoutMutatingState() {
        PersonalizationConsent previous = consent(true, 2L, "u14-old-policy");
        when(bindingRepository.findActiveForAccount(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.of(previous));

        PersonalizationConsentView view = service.getCurrentConsent(ISSUER, SUBJECT, (short) 1);

        assertThat(view.status()).isEqualTo(PersonalizationConsentStatus.RECONSENT_REQUIRED);
        assertThat(view.granted()).isFalse();
        assertThat(view.acceptedPolicyVersion()).isEqualTo("u14-old-policy");
        verify(consentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void staleRevisionConflictsBeforeAnyWrites() {
        when(bindingRepository.findActiveForAccountForUpdate(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.of(consent(true, 1L,
                PersonalizationConsentServiceImpl.CURRENT_POLICY_VERSION)));

        assertThatThrownBy(() -> service.updateConsent(ISSUER, SUBJECT, grantRequest(0, 1)))
                .isInstanceOfSatisfying(PersonalizationConsentException.class,
                        exception -> assertThat(exception.getError())
                                .isEqualTo(PersonalizationConsentError.CONSENT_REVISION_CONFLICT));

        verify(consentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void staleBindingVersionConflictsBeforeReadingConsentOrHotel() {
        when(bindingRepository.findActiveForAccountForUpdate(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));

        assertThatThrownBy(() -> service.updateConsent(ISSUER, SUBJECT, grantRequest(0, 2)))
                .isInstanceOfSatisfying(PersonalizationConsentException.class,
                        exception -> assertThat(exception.getError())
                                .isEqualTo(PersonalizationConsentError.IDENTITY_BINDING_CONFLICT));

        verifyNoInteractions(consentRepository, eventRepository, hotelService);
    }

    @Test
    void stalePolicyVersionConflictsWithoutWriting() {
        when(bindingRepository.findActiveForAccountForUpdate(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.empty());
        PersonalizationConsentUpdateRequest stalePolicy = grantRequest(0, 1);
        stalePolicy.setPolicyVersion("u14-old-policy");

        assertThatThrownBy(() -> service.updateConsent(ISSUER, SUBJECT, stalePolicy))
                .isInstanceOfSatisfying(PersonalizationConsentException.class,
                        exception -> assertThat(exception.getError())
                                .isEqualTo(PersonalizationConsentError.CONSENT_POLICY_CONFLICT));

        verify(consentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void inactiveHotelMapsToScopedHotelNotFound() {
        when(bindingRepository.findActiveForAccountForUpdate(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(false);

        assertThatThrownBy(() -> service.updateConsent(ISSUER, SUBJECT, grantRequest(0, 1)))
                .isInstanceOfSatisfying(PersonalizationConsentException.class,
                        exception -> assertThat(exception.getError())
                                .isEqualTo(PersonalizationConsentError.HOTEL_NOT_FOUND));
        verifyNoInteractions(consentRepository, eventRepository);
    }

    @Test
    void updateIsTransactionalAndLocksBindingBeforeConsentRead() throws Exception {
        assertThat(PersonalizationConsentServiceImpl.class
                .getMethod("updateConsent", String.class, String.class, PersonalizationConsentUpdateRequest.class)
                .getAnnotation(Transactional.class)).isNotNull();

        when(bindingRepository.findActiveForAccountForUpdate(ISSUER, SUBJECT)).thenReturn(Optional.of(binding));
        when(hotelService.isActiveHotel((short) 1)).thenReturn(true);
        when(consentRepository.findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE)).thenReturn(Optional.empty());

        service.updateConsent(ISSUER, SUBJECT, grantRequest(0, 1));

        InOrder order = inOrder(bindingRepository, consentRepository, eventRepository);
        order.verify(bindingRepository).findActiveForAccountForUpdate(ISSUER, SUBJECT);
        order.verify(consentRepository).findByBindingIdAndHotelIdAndPurpose(81L, (short) 1,
                PersonalizationConsentServiceImpl.PURPOSE);
        order.verify(consentRepository).saveAndFlush(any(PersonalizationConsent.class));
        order.verify(eventRepository).saveAndFlush(any(PersonalizationConsentEvent.class));
    }

    @Test
    void missingBindingFailsClosedBeforeHotelOrConsentAccess() {
        when(bindingRepository.findActiveForAccount(ISSUER, SUBJECT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentConsent(ISSUER, SUBJECT, (short) 1))
                .isInstanceOfSatisfying(PersonalizationConsentException.class,
                        exception -> assertThat(exception.getError())
                                .isEqualTo(PersonalizationConsentError.IDENTITY_LINK_REQUIRED));
        verifyNoInteractions(hotelService, consentRepository, eventRepository);
    }

    private PersonalizationConsentUpdateRequest grantRequest(long expectedRevision, long bindingVersion) {
        PersonalizationConsentUpdateRequest request = new PersonalizationConsentUpdateRequest();
        request.setHotelId((short) 1);
        request.setGranted(true);
        request.setExpectedRevision(expectedRevision);
        request.setExpectedBindingVersion(bindingVersion);
        request.setPolicyVersion(PersonalizationConsentServiceImpl.CURRENT_POLICY_VERSION);
        return request;
    }

    private PersonalizationConsentUpdateRequest revokeRequest(long expectedRevision, long bindingVersion) {
        PersonalizationConsentUpdateRequest request = new PersonalizationConsentUpdateRequest();
        request.setHotelId((short) 1);
        request.setGranted(false);
        request.setExpectedRevision(expectedRevision);
        request.setExpectedBindingVersion(bindingVersion);
        return request;
    }

    private PersonalizationConsent consent(boolean granted, long revision, String policyVersion) {
        PersonalizationConsent consent = new PersonalizationConsent();
        consent.setBindingId(81L);
        consent.setHotelId((short) 1);
        consent.setPurpose(PersonalizationConsentServiceImpl.PURPOSE);
        consent.setGranted(granted);
        consent.setRevision(revision);
        consent.setPolicyVersion(policyVersion);
        consent.setUpdatedAt(OffsetDateTime.parse("2026-10-26T10:00:00Z"));
        return consent;
    }

}
