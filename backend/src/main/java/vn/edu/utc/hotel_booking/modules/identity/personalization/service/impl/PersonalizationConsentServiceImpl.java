package vn.edu.utc.hotel_booking.modules.identity.personalization.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationAuthorizationSnapshot;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationAuthorizationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.port.PersonalizationAuthorizationPort;
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
import vn.edu.utc.hotel_booking.modules.identity.personalization.service.PersonalizationConsentService;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalizationConsentServiceImpl
        implements PersonalizationConsentService, PersonalizationAuthorizationPort {

    public static final String PURPOSE = "ROOM_TYPE_RECOMMENDATION";
    public static final String CURRENT_POLICY_VERSION = "u14-local-room-affinity-v1";
    public static final String PRIVACY_POLICY_URL = "/portal/privacy";
    public static final String DATA_MODE = "LOCAL_SYNTHETIC";
    public static final int HISTORY_LOOKBACK_MONTHS = 12;
    public static final String NOTICE_TEXT = "Đây là bản demo dùng dữ liệu giả lập. Khi consent có hiệu lực, "
            + "hệ thống phân tích các booking FIT một phòng đã hoàn tất, đứng tên hồ sơ được liên kết, "
            + "tại khách sạn này trong 12 tháng gần nhất để xếp hạng hạng phòng, bao gồm booking trước "
            + "thời điểm cấp consent. Có thể rút consent bất cứ lúc nào. Gợi ý không thay đổi giá, khuyến mại "
            + "hoặc đặt phòng và không bảo đảm giá hay tồn phòng.";

    private final PersonalizationAccountBindingRepository bindingRepository;
    private final PersonalizationConsentRepository consentRepository;
    private final PersonalizationConsentEventRepository eventRepository;
    private final HotelService hotelService;

    @Override
    public PersonalizationConsentView getCurrentConsent(String issuer, String subject, Short hotelId) {
        validateAccountIdentity(issuer, subject);
        validateHotelId(hotelId);

        PersonalizationAccountBinding binding = bindingRepository.findActiveForAccount(issuer, subject)
                .orElseThrow(() -> error(PersonalizationConsentError.IDENTITY_LINK_REQUIRED));
        requireActiveHotel(hotelId);

        Optional<PersonalizationConsent> consent = consentRepository.findByBindingIdAndHotelIdAndPurpose(
                binding.getId(), hotelId, PURPOSE);
        return toView(binding.getBindingVersion(), hotelId, consent.orElse(null));
    }

    @Override
    @Transactional
    public PersonalizationConsentView updateConsent(
            String issuer,
            String subject,
            PersonalizationConsentUpdateRequest request
    ) {
        validateAccountIdentity(issuer, subject);
        validateRequest(request);

        PersonalizationAccountBinding binding = bindingRepository
                .findActiveForAccountForUpdate(issuer, subject)
                .orElseThrow(() -> error(PersonalizationConsentError.IDENTITY_LINK_REQUIRED));

        if (!binding.getBindingVersion().equals(request.getExpectedBindingVersion())) {
            throw error(PersonalizationConsentError.IDENTITY_BINDING_CONFLICT);
        }

        requireActiveHotel(request.getHotelId());

        Optional<PersonalizationConsent> existing = consentRepository.findByBindingIdAndHotelIdAndPurpose(
                binding.getId(), request.getHotelId(), PURPOSE);
        long actualRevision = existing.map(PersonalizationConsent::getRevision).orElse(0L);
        if (actualRevision != request.getExpectedRevision()) {
            throw error(PersonalizationConsentError.CONSENT_REVISION_CONFLICT);
        }

        boolean granted = Boolean.TRUE.equals(request.getGranted());
        if (granted && !CURRENT_POLICY_VERSION.equals(request.getPolicyVersion())) {
            throw error(PersonalizationConsentError.CONSENT_POLICY_CONFLICT);
        }

        String acceptedPolicyVersion = granted
                ? CURRENT_POLICY_VERSION
                : existing.map(PersonalizationConsent::getPolicyVersion).orElse(null);
        long nextRevision = actualRevision + 1;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        PersonalizationConsent state = existing.orElseGet(PersonalizationConsent::new);
        state.setBindingId(binding.getId());
        state.setHotelId(request.getHotelId());
        state.setPurpose(PURPOSE);
        state.setGranted(granted);
        state.setRevision(nextRevision);
        state.setPolicyVersion(acceptedPolicyVersion);
        state.setUpdatedAt(now);
        consentRepository.saveAndFlush(state);

        PersonalizationConsentEvent event = new PersonalizationConsentEvent();
        event.setBindingId(binding.getId());
        event.setBindingVersion(binding.getBindingVersion());
        event.setHotelId(request.getHotelId());
        event.setPurpose(PURPOSE);
        event.setRevision(nextRevision);
        event.setGranted(granted);
        event.setPolicyVersion(acceptedPolicyVersion);
        eventRepository.saveAndFlush(event);

        return toView(binding.getBindingVersion(), request.getHotelId(), Optional.of(state));
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public PersonalizationAuthorizationSnapshot authorize(String issuer, String subject, Integer hotelId) {
        validateAccountIdentity(issuer, subject);
        Short scopedHotelId = toHotelId(hotelId);
        Optional<PersonalizationAccountBinding> binding = bindingRepository.findActiveForAccount(issuer, subject);
        if (binding.isEmpty()) {
            return authorization(PersonalizationAuthorizationStatus.IDENTITY_LINK_REQUIRED, null, 0, 0);
        }

        if (!isActiveHotel(scopedHotelId)) {
            return authorization(PersonalizationAuthorizationStatus.HOTEL_NOT_FOUND,
                    binding.get().getGuestId(), binding.get().getBindingVersion(), 0);
        }

        Optional<PersonalizationConsent> consent = consentRepository.findByBindingIdAndHotelIdAndPurpose(
                binding.get().getId(), scopedHotelId, PURPOSE);
        long revision = consent.map(PersonalizationConsent::getRevision).orElse(0L);
        boolean granted = consent.filter(PersonalizationConsent::isGranted)
                .filter(state -> CURRENT_POLICY_VERSION.equals(state.getPolicyVersion()))
                .isPresent();
        return authorization(
                granted ? PersonalizationAuthorizationStatus.AUTHORIZED
                        : PersonalizationAuthorizationStatus.CONSENT_REQUIRED,
                binding.get().getGuestId(),
                binding.get().getBindingVersion(),
                revision
        );
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public PersonalizationAuthorizationSnapshot revalidate(
            String issuer,
            String subject,
            Integer hotelId,
            long expectedBindingVersion,
            long expectedConsentRevision
    ) {
        validateAccountIdentity(issuer, subject);
        Short scopedHotelId = toHotelId(hotelId);
        Optional<PersonalizationAccountBinding> binding = bindingRepository.findActiveForAccount(issuer, subject);
        if (binding.isEmpty()) {
            return authorization(PersonalizationAuthorizationStatus.IDENTITY_LINK_REQUIRED, null, 0, 0);
        }
        PersonalizationAccountBinding activeBinding = binding.get();
        if (activeBinding.getBindingVersion() != expectedBindingVersion) {
            return authorization(PersonalizationAuthorizationStatus.IDENTITY_BINDING_CONFLICT,
                    activeBinding.getGuestId(), activeBinding.getBindingVersion(), 0);
        }
        if (!isActiveHotel(scopedHotelId)) {
            return authorization(PersonalizationAuthorizationStatus.HOTEL_NOT_FOUND,
                    activeBinding.getGuestId(), activeBinding.getBindingVersion(), 0);
        }

        Optional<PersonalizationConsent> consent = consentRepository.findByBindingIdAndHotelIdAndPurpose(
                activeBinding.getId(), scopedHotelId, PURPOSE);
        long actualRevision = consent.map(PersonalizationConsent::getRevision).orElse(0L);
        boolean granted = consent.filter(PersonalizationConsent::isGranted)
                .filter(state -> CURRENT_POLICY_VERSION.equals(state.getPolicyVersion()))
                .isPresent();
        if (!granted) {
            return authorization(PersonalizationAuthorizationStatus.CONSENT_REQUIRED,
                    activeBinding.getGuestId(), activeBinding.getBindingVersion(), actualRevision);
        }
        if (actualRevision != expectedConsentRevision) {
            return authorization(PersonalizationAuthorizationStatus.CONSENT_REVISION_CONFLICT,
                    activeBinding.getGuestId(), activeBinding.getBindingVersion(), actualRevision);
        }
        return authorization(PersonalizationAuthorizationStatus.AUTHORIZED,
                activeBinding.getGuestId(), activeBinding.getBindingVersion(), actualRevision);
    }

    private Short toHotelId(Integer hotelId) {
        if (hotelId == null || hotelId < 1 || hotelId > Short.MAX_VALUE) {
            throw error(PersonalizationConsentError.INVALID_REQUEST);
        }
        return hotelId.shortValue();
    }

    private boolean isActiveHotel(Short hotelId) {
        return hotelService.isActiveHotel(hotelId);
    }

    private PersonalizationAuthorizationSnapshot authorization(
            PersonalizationAuthorizationStatus status,
            Long guestId,
            long bindingVersion,
            long consentRevision
    ) {
        return new PersonalizationAuthorizationSnapshot(status, guestId, bindingVersion, consentRevision);
    }

    private void validateAccountIdentity(String issuer, String subject) {
        if (!StringUtils.hasText(issuer) || issuer.length() > 512
                || !StringUtils.hasText(subject) || subject.length() > 255) {
            throw error(PersonalizationConsentError.IDENTITY_LINK_REQUIRED);
        }
    }

    private void validateHotelId(Short hotelId) {
        if (hotelId == null || hotelId < 1) {
            throw error(PersonalizationConsentError.INVALID_REQUEST);
        }
    }

    private void validateRequest(PersonalizationConsentUpdateRequest request) {
        if (request == null
                || request.getHotelId() == null
                || request.getHotelId() < 1
                || request.getGranted() == null
                || request.getExpectedRevision() == null
                || request.getExpectedRevision() < 0
                || request.getExpectedBindingVersion() == null
                || request.getExpectedBindingVersion() < 1) {
            throw error(PersonalizationConsentError.INVALID_REQUEST);
        }

        boolean granted = Boolean.TRUE.equals(request.getGranted());
        String policyVersion = request.getPolicyVersion();
        if (granted && (!request.isPolicyVersionProvided()
                || !request.isPolicyVersionTypeValid()
                || !StringUtils.hasText(policyVersion)
                || policyVersion.codePointCount(0, policyVersion.length()) > 100)) {
            throw error(PersonalizationConsentError.INVALID_REQUEST);
        }
        if (!granted && request.isPolicyVersionProvided()) {
            throw error(PersonalizationConsentError.INVALID_REQUEST);
        }
        if (!request.getUnknownProperties().isEmpty()) {
            throw error(PersonalizationConsentError.INVALID_REQUEST);
        }
    }

    private void requireActiveHotel(Short hotelId) {
        if (!isActiveHotel(hotelId)) {
            throw error(PersonalizationConsentError.HOTEL_NOT_FOUND);
        }
    }

    private PersonalizationConsentView toView(
            long bindingVersion,
            Short hotelId,
            PersonalizationConsent consent
    ) {
        if (consent == null) {
            return view(hotelId, PersonalizationConsentStatus.NOT_GRANTED, false, 0,
                    bindingVersion, null, null);
        }

        String acceptedPolicyVersion = consent.getPolicyVersion();
        if (!consent.isGranted()) {
            return view(hotelId, PersonalizationConsentStatus.NOT_GRANTED, false, consent.getRevision(),
                    bindingVersion, acceptedPolicyVersion, consent.getUpdatedAt());
        }
        if (!CURRENT_POLICY_VERSION.equals(acceptedPolicyVersion)) {
            return view(hotelId, PersonalizationConsentStatus.RECONSENT_REQUIRED, false,
                    consent.getRevision(), bindingVersion, acceptedPolicyVersion, consent.getUpdatedAt());
        }
        return view(hotelId, PersonalizationConsentStatus.GRANTED, true, consent.getRevision(),
                bindingVersion, acceptedPolicyVersion, consent.getUpdatedAt());
    }

    private PersonalizationConsentView toView(
            long bindingVersion,
            Short hotelId,
            Optional<PersonalizationConsent> consent
    ) {
        return toView(bindingVersion, hotelId, consent.orElse(null));
    }

    private PersonalizationConsentView view(
            Short hotelId,
            PersonalizationConsentStatus status,
            boolean granted,
            long revision,
            long bindingVersion,
            String acceptedPolicyVersion,
            OffsetDateTime updatedAt
    ) {
        return new PersonalizationConsentView(
                hotelId,
                status,
                granted,
                revision,
                bindingVersion,
                PURPOSE,
                CURRENT_POLICY_VERSION,
                acceptedPolicyVersion,
                updatedAt,
                HISTORY_LOOKBACK_MONTHS,
                NOTICE_TEXT,
                PRIVACY_POLICY_URL,
                DATA_MODE
        );
    }

    private PersonalizationConsentException error(PersonalizationConsentError error) {
        return new PersonalizationConsentException(error);
    }
}
