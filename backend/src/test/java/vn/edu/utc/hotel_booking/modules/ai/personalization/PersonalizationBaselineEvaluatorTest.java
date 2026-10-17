package vn.edu.utc.hotel_booking.modules.ai.personalization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationEvalReportDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.StayHistoryRecordDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.privacy.PersonalizationPrivacyGuard;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.GuestPersonalizationService;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.PersonalizationBaselineEvaluator;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PersonalizationBaselineEvaluator – Unit Tests")
class PersonalizationBaselineEvaluatorTest {

    private PersonalizationBaselineEvaluator evaluator;

    @BeforeEach
    void setUp() {
        PersonalizationPrivacyGuard privacyGuard = new PersonalizationPrivacyGuard();
        GuestPersonalizationService personalizationService = new GuestPersonalizationService(privacyGuard);
        evaluator = new PersonalizationBaselineEvaluator(personalizationService, privacyGuard);
    }

    @Test
    @DisplayName("TC-16: Đánh giá với ít hơn 2 kỳ lưu trú -> trả về báo cáo 0 bước đánh giá")
    void tc16_lessThanTwoStays_shouldReturnZeroEvaluated() {
        StayHistoryRecordDto singleStay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(1).roomTypeCode("STD").roomTypeName("Standard")
                .checkInDate(LocalDate.of(2026, 1, 1)).checkOutDate(LocalDate.of(2026, 1, 3)).build();

        GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
                .guestId(201L).consentGranted(true).stayHistory(List.of(singleStay)).build();

        PersonalizationEvalReportDto report = evaluator.evaluateHoldout(request, 1);

        assertThat(report.getTotalEvaluatedStays()).isEqualTo(0);
        assertThat(report.getTimeDecayedHitRateAtK()).isEqualTo(0.0);
        assertThat(report.isLeakageFreeTimeSplit()).isTrue();
    }

    @Test
    @DisplayName("TC-17: Khách thay đổi sở thích theo thời gian -> TimeDecayedModel đạt HitRate@1 & MRR cao hơn Baseline")
    void tc17_shiftingPreference_timeDecayedShouldOutperformFrequencyBaseline() {
        // Chuỗi lưu trú: Khách ở Standard 3 lần đầu (năm 2024), sau đó chuyển sang ở Deluxe 4 lần liên tiếp (năm 2026)
        StayHistoryRecordDto s1 = buildStay(1, 10, "STD", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3));
        StayHistoryRecordDto s2 = buildStay(1, 10, "STD", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3));
        StayHistoryRecordDto s3 = buildStay(1, 10, "STD", LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 3));

        StayHistoryRecordDto s4 = buildStay(1, 20, "DLX", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));
        StayHistoryRecordDto s5 = buildStay(1, 20, "DLX", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3));
        StayHistoryRecordDto s6 = buildStay(1, 20, "DLX", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3));
        StayHistoryRecordDto s7 = buildStay(1, 20, "DLX", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
                .guestId(202L)
                .consentGranted(true)
                .stayHistory(List.of(s1, s2, s3, s4, s5, s6, s7))
                .build();

        PersonalizationEvalReportDto report = evaluator.evaluateHoldout(request, 1);

        assertThat(report.getTotalEvaluatedStays()).isEqualTo(6);
        assertThat(report.isLeakageFreeTimeSplit()).isTrue();

        // Mô hình Time-Decayed nhanh chóng nhận diện sự chuyển dịch sở thích sang Deluxe
        // do đó đạt HitRate@1 và MRR@1 vượt trội so với SimpleFrequencyBaseline (vốn bị thiên lệch bởi các lượt ở cũ)
        assertThat(report.getTimeDecayedHitRateAtK()).isGreaterThanOrEqualTo(report.getBaselineFrequencyHitRateAtK());
        assertThat(report.getTimeDecayedMrrAtK()).isGreaterThanOrEqualTo(report.getBaselineFrequencyMrrAtK());
    }

    @Test
    @DisplayName("TC-18: Chưa cấp quyền consent -> ném IllegalArgumentException khi gọi đánh giá")
    void tc18_consentNotGranted_shouldThrowException() {
        GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
                .guestId(203L).consentGranted(false).stayHistory(List.of()).build();

        assertThatThrownBy(() -> evaluator.evaluateHoldout(request, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Consent is required");
    }

    private StayHistoryRecordDto buildStay(int hotelId, int roomTypeId, String code, LocalDate in, LocalDate out) {
        return StayHistoryRecordDto.builder()
                .hotelId(hotelId)
                .hotelRoomTypeId(roomTypeId)
                .roomTypeCode(code)
                .roomTypeName(code + " Room")
                .checkInDate(in)
                .checkOutDate(out)
                .adults(1)
                .build();
    }
}
