package vn.edu.utc.hotel_booking.modules.ai.personalization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.StayHistoryRecordDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.privacy.PersonalizationPrivacyGuard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PersonalizationPrivacyGuard – Unit Tests")
class PersonalizationPrivacyGuardTest {

    private PersonalizationPrivacyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new PersonalizationPrivacyGuard();
    }

    @Test
    @DisplayName("TC-11: Chặn và loại bỏ thuộc tính tôn giáo / tín ngưỡng (halal, phật giáo, kosher)")
    void tc11_shouldBlockReligionAttributes() {
        StayHistoryRecordDto stay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(1).roomTypeCode("DLX").roomTypeName("Deluxe")
                .checkInDate(LocalDate.of(2026, 8, 1)).checkOutDate(LocalDate.of(2026, 8, 3))
                .preferredFeatureCodes(List.of("SEA_VIEW", "PHAT_GIAO", "HALAL_FOOD"))
                .guestPreferenceNote("Khách yêu cầu đồ ăn Halal và phòng hướng đạo Phật")
                .build();

        List<String> warnings = new ArrayList<>();
        StayHistoryRecordDto sanitized = guard.sanitizeStayRecord(stay, warnings);

        assertThat(sanitized.getPreferredFeatureCodes()).containsExactly("SEA_VIEW");
        assertThat(sanitized.getPreferredFeatureCodes()).doesNotContain("PHAT_GIAO", "HALAL_FOOD");
        assertThat(sanitized.getGuestPreferenceNote()).isEqualTo("[REDACTED_SENSITIVE_PREFERENCE]");
        assertThat(warnings).anyMatch(w -> w.contains("SENSITIVE_ATTRIBUTE_BLOCKED"));
    }

    @Test
    @DisplayName("TC-12: Chặn và loại bỏ thuộc tính sức khỏe / bệnh lý / mang thai / khuyết tật")
    void tc12_shouldBlockHealthAndDisabilityAttributes() {
        StayHistoryRecordDto stay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(1).roomTypeCode("DLX").roomTypeName("Deluxe")
                .checkInDate(LocalDate.of(2026, 8, 1)).checkOutDate(LocalDate.of(2026, 8, 3))
                .preferredFeatureCodes(List.of("BALCONY", "WHEELCHAIR_ACCESS", "PREGNANT_CARE"))
                .guestPreferenceNote("Khách đang mang thai tháng thứ 7 và cần hỗ trợ xe lăn")
                .build();

        List<String> warnings = new ArrayList<>();
        StayHistoryRecordDto sanitized = guard.sanitizeStayRecord(stay, warnings);

        assertThat(sanitized.getPreferredFeatureCodes()).containsExactly("BALCONY");
        assertThat(sanitized.getPreferredFeatureCodes()).doesNotContain("WHEELCHAIR_ACCESS", "PREGNANT_CARE");
        assertThat(sanitized.getGuestPreferenceNote()).isEqualTo("[REDACTED_SENSITIVE_PREFERENCE]");
        assertThat(warnings).anyMatch(w -> w.contains("SENSITIVE_ATTRIBUTE_BLOCKED"));
    }

    @Test
    @DisplayName("TC-13: Chặn và loại bỏ PII (CCCD, SĐT, Email, Passport) trong ghi chú sở thích")
    void tc13_shouldBlockPiiInPreferences() {
        StayHistoryRecordDto stay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(1).roomTypeCode("DLX").roomTypeName("Deluxe")
                .checkInDate(LocalDate.of(2026, 8, 1)).checkOutDate(LocalDate.of(2026, 8, 3))
                .guestPreferenceNote("Số CCCD khách: 079190001234, SĐT liên hệ: 0901234567, email: test@domain.com")
                .build();

        List<String> warnings = new ArrayList<>();
        StayHistoryRecordDto sanitized = guard.sanitizeStayRecord(stay, warnings);

        assertThat(sanitized.getGuestPreferenceNote()).isEqualTo("[REDACTED_SENSITIVE_PREFERENCE]");
        assertThat(warnings).anyMatch(w -> w.contains("SENSITIVE_ATTRIBUTE_BLOCKED"));
    }

    @Test
    @DisplayName("TC-14: Các đặc tính và dịch vụ hợp lệ được giữ nguyên 100%, không sinh warning")
    void tc14_shouldPreserveLegitimateFeaturesWithoutWarning() {
        StayHistoryRecordDto stay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(1).roomTypeCode("DLX").roomTypeName("Deluxe")
                .checkInDate(LocalDate.of(2026, 8, 1)).checkOutDate(LocalDate.of(2026, 8, 3))
                .preferredFeatureCodes(List.of("SEA_VIEW", "BALCONY", "HIGH_FLOOR", "BATHTUB", "WORK_DESK", "QUIET_ROOM"))
                .usedServiceCodes(List.of("BREAKFAST", "SPA", "AIRPORT_TRANSFER", "LAUNDRY"))
                .guestPreferenceNote("Khách thích phòng yên tĩnh, tầng cao và view biển thoáng đãng")
                .build();

        List<String> warnings = new ArrayList<>();
        StayHistoryRecordDto sanitized = guard.sanitizeStayRecord(stay, warnings);

        assertThat(sanitized.getPreferredFeatureCodes()).hasSize(6);
        assertThat(sanitized.getUsedServiceCodes()).hasSize(4);
        assertThat(sanitized.getGuestPreferenceNote()).contains("phòng yên tĩnh");
        assertThat(warnings).isEmpty();
    }

    @Test
    @DisplayName("TC-15: Lọc bỏ triệt để null, chuỗi rỗng và trim() các mã đặc tính và dịch vụ")
    void tc15_shouldFilterNullAndBlankAndTrimCodes() {
        List<String> rawFeatures = new ArrayList<>();
        rawFeatures.add(null);
        rawFeatures.add("");
        rawFeatures.add("   ");
        rawFeatures.add("  SEA_VIEW  ");
        rawFeatures.add("BALCONY");

        List<String> rawServices = new ArrayList<>();
        rawServices.add(null);
        rawServices.add("  BREAKFAST  ");
        rawServices.add("   ");

        StayHistoryRecordDto stay = StayHistoryRecordDto.builder()
                .hotelId(1).hotelRoomTypeId(1).roomTypeCode("DLX").roomTypeName("Deluxe")
                .checkInDate(LocalDate.of(2026, 8, 1)).checkOutDate(LocalDate.of(2026, 8, 3))
                .preferredFeatureCodes(rawFeatures)
                .usedServiceCodes(rawServices)
                .guestPreferenceNote("   Khách muốn phòng view đẹp   ")
                .build();

        List<String> warnings = new ArrayList<>();
        StayHistoryRecordDto sanitized = guard.sanitizeStayRecord(stay, warnings);

        assertThat(sanitized.getPreferredFeatureCodes()).containsExactly("SEA_VIEW", "BALCONY");
        assertThat(sanitized.getUsedServiceCodes()).containsExactly("BREAKFAST");
        assertThat(sanitized.getGuestPreferenceNote()).isEqualTo("Khách muốn phòng view đẹp");
        assertThat(warnings).isEmpty();
    }
}
