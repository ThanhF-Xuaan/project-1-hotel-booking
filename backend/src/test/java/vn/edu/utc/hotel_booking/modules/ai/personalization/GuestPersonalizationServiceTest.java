package vn.edu.utc.hotel_booking.modules.ai.personalization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationResultDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizedRoomTypeSuggestionDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.StayHistoryRecordDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.enums.PersonalizationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.enums.TravelPatternSegment;
import vn.edu.utc.hotel_booking.modules.ai.personalization.privacy.PersonalizationPrivacyGuard;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.GuestPersonalizationService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GuestPersonalizationService – Unit Tests")
class GuestPersonalizationServiceTest {

    private GuestPersonalizationService service;
    private PersonalizationPrivacyGuard privacyGuard;

    private final LocalDate refDate = LocalDate.of(2026, 10, 1);

    @BeforeEach
    void setUp() {
        privacyGuard = new PersonalizationPrivacyGuard();
        service = new GuestPersonalizationService(privacyGuard);
    }

    // =====================================================================
    // TC-01: consentGranted = false -> CONSENT_REQUIRED
    // =====================================================================

    @Test
    @DisplayName("TC-01: Khách chưa đồng ý consent -> CONSENT_REQUIRED, gợi ý rỗng")
    void tc01_consentNotGranted_shouldReturnConsentRequired() {
        GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
                .guestId(101L)
                .consentGranted(false)
                .referenceDate(refDate)
                .stayHistory(List.of(
                        StayHistoryRecordDto.builder()
                                .hotelId(1)
                                .hotelRoomTypeId(10)
                                .roomTypeCode("DLX")
                                .roomTypeName("Deluxe Room")
                                .checkInDate(LocalDate.of(2026, 8, 1))
                                .checkOutDate(LocalDate.of(2026, 8, 5))
                                .adults(2)
                                .build()
                ))
                .build();

        GuestPersonalizationResultDto result = service.personalize(request);

        assertThat(result.getStatus()).isEqualTo(PersonalizationStatus.CONSENT_REQUIRED);
        assertThat(result.getRoomTypeSuggestions()).isEmpty();
        assertThat(result.getFeatureSuggestions()).isEmpty();
        assertThat(result.getServiceSuggestions()).isEmpty();
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("CONSENT_REQUIRED"));
    }

    // =====================================================================
    // TC-02: Lịch sử rỗng hoặc toàn bộ ngày không hợp lệ -> COLD_START_FALLBACK
    // =====================================================================

    @Test
    @DisplayName("TC-02: Lịch sử rỗng hoặc checkIn >= checkOut -> COLD_START_FALLBACK")
    void tc02_emptyOrInvalidHistory_shouldReturnColdStartFallback() {
        // Case A: Lịch sử rỗng
        GuestPersonalizationRequestDto emptyReq = GuestPersonalizationRequestDto.builder()
                .guestId(102L)
                .consentGranted(true)
                .stayHistory(Collections.emptyList())
                .build();
        GuestPersonalizationResultDto resEmpty = service.personalize(emptyReq);
        assertThat(resEmpty.getStatus()).isEqualTo(PersonalizationStatus.COLD_START_FALLBACK);
        assertThat(resEmpty.getRoomTypeSuggestions()).isEmpty();

        // Case B: Toàn bộ ngày sai logic (checkIn sau checkOut hoặc nằm trong tương lai)
        GuestPersonalizationRequestDto invalidReq = GuestPersonalizationRequestDto.builder()
                .guestId(102L)
                .consentGranted(true)
                .referenceDate(refDate)
                .stayHistory(List.of(
                        StayHistoryRecordDto.builder()
                                .hotelId(1)
                                .hotelRoomTypeId(11)
                                .roomTypeCode("STD")
                                .roomTypeName("Standard Room")
                                .checkInDate(LocalDate.of(2026, 9, 10))
                                .checkOutDate(LocalDate.of(2026, 9, 5)) // checkIn sau checkOut
                                .build(),
                        StayHistoryRecordDto.builder()
                                .hotelId(1)
                                .hotelRoomTypeId(12)
                                .roomTypeCode("SUP")
                                .roomTypeName("Superior Room")
                                .checkInDate(LocalDate.of(2026, 11, 1)) // Trong tương lai so với refDate
                                .checkOutDate(LocalDate.of(2026, 11, 5))
                                .build()
                ))
                .build();

        GuestPersonalizationResultDto resInvalid = service.personalize(invalidReq);
        assertThat(resInvalid.getStatus()).isEqualTo(PersonalizationStatus.COLD_START_FALLBACK);
        assertThat(resInvalid.getWarnings()).isNotEmpty();
    }

    // =====================================================================
    // TC-03: Trọng số suy giảm theo thời gian (Recency Decay)
    // =====================================================================

    @Test
    @DisplayName("TC-03: Trọng số suy giảm thời gian -> phòng ở gần đây thắng phòng ở 2 năm trước")
    void tc03_timeDecayWeighting_recentStaysWinOverOldStays() {
        // Khách từng ở Standard 2 lần cách đây 2 năm (2024), nhưng 2 lần gần nhất (tháng 8, 9/2026) ở Deluxe
        StayHistoryRecordDto oldStay1 = StayHistoryRecordDto.builder()
                .hotelId(1)
                .hotelRoomTypeId(101)
                .roomTypeCode("STD")
                .roomTypeName("Standard City View")
                .checkInDate(LocalDate.of(2024, 1, 10))
                .checkOutDate(LocalDate.of(2024, 1, 12))
                .adults(1)
                .build();

        StayHistoryRecordDto oldStay2 = StayHistoryRecordDto.builder()
                .hotelId(1)
                .hotelRoomTypeId(101)
                .roomTypeCode("STD")
                .roomTypeName("Standard City View")
                .checkInDate(LocalDate.of(2024, 3, 15))
                .checkOutDate(LocalDate.of(2024, 3, 17))
                .adults(1)
                .build();

        StayHistoryRecordDto recentStay1 = StayHistoryRecordDto.builder()
                .hotelId(1)
                .hotelRoomTypeId(102)
                .roomTypeCode("DLX")
                .roomTypeName("Deluxe Sea View")
                .checkInDate(LocalDate.of(2026, 8, 1))
                .checkOutDate(LocalDate.of(2026, 8, 3))
                .adults(1)
                .build();

        StayHistoryRecordDto recentStay2 = StayHistoryRecordDto.builder()
                .hotelId(1)
                .hotelRoomTypeId(102)
                .roomTypeCode("DLX")
                .roomTypeName("Deluxe Sea View")
                .checkInDate(LocalDate.of(2026, 9, 1))
                .checkOutDate(LocalDate.of(2026, 9, 3))
                .adults(1)
                .build();

        GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
                .guestId(103L)
                .consentGranted(true)
                .referenceDate(refDate)
                .stayHistory(List.of(oldStay1, oldStay2, recentStay1, recentStay2))
                .build();

        GuestPersonalizationResultDto result = service.personalize(request);

        assertThat(result.getStatus()).isEqualTo(PersonalizationStatus.PERSONALIZED_READY);
        List<PersonalizedRoomTypeSuggestionDto> suggestions = result.getRoomTypeSuggestions();
        assertThat(suggestions).hasSize(2);

        // Hạng phòng gần đây (Deluxe Sea View - ID 102) phải xếp thứ 1 vì điểm suy giảm theo thời gian cao hơn
        assertThat(suggestions.get(0).getHotelRoomTypeId()).isEqualTo(102);
        assertThat(suggestions.get(0).getRoomTypeCode()).isEqualTo("DLX");
        assertThat(suggestions.get(0).getAffinityScore()).isEqualTo(1.0); // Điểm chuẩn hóa max

        assertThat(suggestions.get(1).getHotelRoomTypeId()).isEqualTo(101);
        assertThat(suggestions.get(1).getAffinityScore()).isLessThan(suggestions.get(0).getAffinityScore());
    }

    // =====================================================================
    // TC-04: Ưu tiên theo targetHotelId
    // =====================================================================

    @Test
    @DisplayName("TC-04: targetHotelId được hưởng hệ số ưu tiên (boost x1.25)")
    void tc04_targetHotelIdBoost_shouldPrioritizeTargetHotel() {
        // Cùng ngày lưu trú và thời lượng, nhưng ở 2 khách sạn khác nhau
        StayHistoryRecordDto stayHotel1 = StayHistoryRecordDto.builder()
                .hotelId(1)
                .hotelRoomTypeId(201)
                .roomTypeCode("SUITE-H1")
                .roomTypeName("Executive Suite H1")
                .checkInDate(LocalDate.of(2026, 8, 1))
                .checkOutDate(LocalDate.of(2026, 8, 3))
                .adults(2)
                .build();

        StayHistoryRecordDto stayHotel2 = StayHistoryRecordDto.builder()
                .hotelId(2)
                .hotelRoomTypeId(202)
                .roomTypeCode("SUITE-H2")
                .roomTypeName("Executive Suite H2")
                .checkInDate(LocalDate.of(2026, 8, 1))
                .checkOutDate(LocalDate.of(2026, 8, 3))
                .adults(2)
                .build();

        GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
                .guestId(104L)
                .consentGranted(true)
                .referenceDate(refDate)
                .targetHotelId(2) // Khách đang xem phòng tại Khách sạn 2
                .stayHistory(List.of(stayHotel1, stayHotel2))
                .build();

        GuestPersonalizationResultDto result = service.personalize(request);

        assertThat(result.getRoomTypeSuggestions()).hasSize(2);
        // Phòng của Khách sạn 2 phải đứng đầu nhờ boost x1.25
        assertThat(result.getRoomTypeSuggestions().get(0).getHotelId()).isEqualTo(2);
        assertThat(result.getRoomTypeSuggestions().get(0).getExplanationReasons())
                .anyMatch(r -> r.contains("khách sạn mục tiêu"));
    }

    // =====================================================================
    // TC-05: Nhận diện phân khúc khách (TravelPatternSegment)
    // =====================================================================

    @Test
    @DisplayName("TC-05: Nhận diện đúng phân khúc FAMILY_LEISURE, COUPLE_GETAWAY, BUSINESS_FREQUENT")
    void tc05_travelPatternSegmentDetection() {
        // Family: có trẻ em
        StayHistoryRecordDto familyStay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(1).roomTypeCode("FAM").roomTypeName("Family Room")
                .checkInDate(LocalDate.of(2026, 7, 1)).checkOutDate(LocalDate.of(2026, 7, 5))
                .adults(2).children(2).build();
        GuestPersonalizationResultDto resFamily = service.personalize(GuestPersonalizationRequestDto.builder()
                .guestId(1L).consentGranted(true).referenceDate(refDate).stayHistory(List.of(familyStay)).build());
        assertThat(resFamily.getDetectedSegment()).isEqualTo(TravelPatternSegment.FAMILY_LEISURE);

        // Couple: 2 người lớn, không có trẻ em
        StayHistoryRecordDto coupleStay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(2).roomTypeCode("HON").roomTypeName("Honeymoon Suite")
                .checkInDate(LocalDate.of(2026, 7, 1)).checkOutDate(LocalDate.of(2026, 7, 5))
                .adults(2).children(0).build();
        GuestPersonalizationResultDto resCouple = service.personalize(GuestPersonalizationRequestDto.builder()
                .guestId(2L).consentGranted(true).referenceDate(refDate).stayHistory(List.of(coupleStay)).build());
        assertThat(resCouple.getDetectedSegment()).isEqualTo(TravelPatternSegment.COUPLE_GETAWAY);

        // Business Frequent: đi 1 mình, >= 2 lần, có WORK_DESK / AIRPORT_TRANSFER
        StayHistoryRecordDto bizStay1 = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(3).roomTypeCode("BIZ").roomTypeName("Business Room")
                .checkInDate(LocalDate.of(2026, 8, 1)).checkOutDate(LocalDate.of(2026, 8, 3))
                .adults(1).children(0).preferredFeatureCodes(List.of("WORK_DESK", "HIGH_FLOOR")).build();
        StayHistoryRecordDto bizStay2 = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(3).roomTypeCode("BIZ").roomTypeName("Business Room")
                .checkInDate(LocalDate.of(2026, 9, 1)).checkOutDate(LocalDate.of(2026, 9, 3))
                .adults(1).children(0).usedServiceCodes(List.of("AIRPORT_TRANSFER", "LAUNDRY")).build();
        GuestPersonalizationResultDto resBiz = service.personalize(GuestPersonalizationRequestDto.builder()
                .guestId(3L).consentGranted(true).referenceDate(refDate).stayHistory(List.of(bizStay1, bizStay2)).build());
        assertThat(resBiz.getDetectedSegment()).isEqualTo(TravelPatternSegment.BUSINESS_FREQUENT);
    }

    // =====================================================================
    // TC-06: Bất biến kiến trúc U14: nonBindingSuggestion & pricingOrPromotionMutated
    // =====================================================================

    @Test
    @DisplayName("TC-06: Bất biến kiến trúc: isNonBindingSuggestion=true, isPricingOrPromotionMutated=false")
    void tc06_architecturalInvariants() {
        StayHistoryRecordDto stay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(1).roomTypeCode("DLX").roomTypeName("Deluxe")
                .checkInDate(LocalDate.of(2026, 8, 1)).checkOutDate(LocalDate.of(2026, 8, 3))
                .build();

        GuestPersonalizationResultDto result = service.personalize(GuestPersonalizationRequestDto.builder()
                .guestId(105L).consentGranted(true).referenceDate(refDate).stayHistory(List.of(stay)).build());

        assertThat(result.isNonBindingSuggestion()).isTrue();
        assertThat(result.isPricingOrPromotionMutated()).isFalse();
    }

    // =====================================================================
    // TC-07: Trích xuất và xếp hạng đặc tính và dịch vụ yêu thích
    // =====================================================================

    @Test
    @DisplayName("TC-07: Gợi ý đặc tính và dịch vụ yêu thích xếp hạng theo điểm số affinity giảm dần")
    void tc07_featureAndServiceSuggestions() {
        StayHistoryRecordDto stay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(1).roomTypeCode("DLX").roomTypeName("Deluxe")
                .checkInDate(LocalDate.of(2026, 8, 1)).checkOutDate(LocalDate.of(2026, 8, 3))
                .preferredFeatureCodes(List.of("SEA_VIEW", "BATHTUB"))
                .usedServiceCodes(List.of("BREAKFAST", "SPA"))
                .build();

        GuestPersonalizationResultDto result = service.personalize(GuestPersonalizationRequestDto.builder()
                .guestId(106L).consentGranted(true).referenceDate(refDate).stayHistory(List.of(stay)).build());

        assertThat(result.getFeatureSuggestions()).isNotEmpty();
        assertThat(result.getFeatureSuggestions().get(0).getFeatureCode()).isIn("SEA_VIEW", "BATHTUB");
        assertThat(result.getFeatureSuggestions().get(0).getAffinityScore()).isGreaterThan(0.0);

        assertThat(result.getServiceSuggestions()).isNotEmpty();
        assertThat(result.getServiceSuggestions().get(0).getServiceCode()).isIn("BREAKFAST", "SPA");
        assertThat(result.getServiceSuggestions().get(0).getAffinityScore()).isGreaterThan(0.0);
    }

    // =====================================================================
    // TC-08: Toàn bộ tín hiệu sở thích đầu vào đều nhạy cảm -> SENSITIVE_INPUT_REJECTED
    // =====================================================================

    @Test
    @DisplayName("TC-08: Toàn bộ tín hiệu sở thích đầu vào đều là thuộc tính nhạy cảm -> SENSITIVE_INPUT_REJECTED")
    void tc08_allSensitiveSignals_shouldReturnSensitiveInputRejected() {
        StayHistoryRecordDto stay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(1).roomTypeCode("DLX").roomTypeName("Deluxe")
                .checkInDate(LocalDate.of(2026, 8, 1)).checkOutDate(LocalDate.of(2026, 8, 3))
                .preferredFeatureCodes(List.of("HALAL_FOOD", "PHAT_GIAO"))
                .usedServiceCodes(List.of("PREGNANT_CARE", "WHEELCHAIR_ACCESS"))
                .guestPreferenceNote("Khách ăn chay theo đạo phật và đang mang thai")
                .build();

        GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
                .guestId(107L)
                .consentGranted(true)
                .referenceDate(refDate)
                .stayHistory(List.of(stay))
                .build();

        GuestPersonalizationResultDto result = service.personalize(request);

        assertThat(result.getStatus()).isEqualTo(PersonalizationStatus.SENSITIVE_INPUT_REJECTED);
        assertThat(result.getRoomTypeSuggestions()).isEmpty();
        assertThat(result.getFeatureSuggestions()).isEmpty();
        assertThat(result.getServiceSuggestions()).isEmpty();
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("SENSITIVE_INPUT_REJECTED"));
    }
}
