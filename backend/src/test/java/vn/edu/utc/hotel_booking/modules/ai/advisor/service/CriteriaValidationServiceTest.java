package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.ValidationResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CriteriaValidationServiceTest {

    private CriteriaValidationService service;

    @BeforeEach
    void setUp() {
        service = new CriteriaValidationService();
    }

    @Test
    @DisplayName("Should detect out-of-scope write/payment/hold intents")
    void shouldDetectOutOfScopeIntents() {
        assertThat(service.isOutOfScopeIntent("Tôi muốn đặt phòng này ngay")).isTrue();
        assertThat(service.isOutOfScopeIntent("Hãy giữ phòng giúp tôi")).isTrue();
        assertThat(service.isOutOfScopeIntent("Thanh toán tiền phòng qua thẻ")).isTrue();
        assertThat(service.isOutOfScopeIntent("Tôi muốn hủy phòng đã đặt")).isTrue();
        assertThat(service.isOutOfScopeIntent("Hoàn tiền đặt phòng")).isTrue();
        assertThat(service.isOutOfScopeIntent("Chuyển tới trang admin quản trị")).isTrue();
        assertThat(service.isOutOfScopeIntent("book room now please")).isTrue();
        assertThat(service.isOutOfScopeIntent("hold this room for 15 minutes")).isTrue();

        // Valid search inquiries must not be detected as out-of-scope
        assertThat(service.isOutOfScopeIntent("Tìm giúp tôi phòng đôi ở Đà Nẵng")).isFalse();
        assertThat(service.isOutOfScopeIntent("Có phòng nào giá dưới 1 triệu không?")).isFalse();
        assertThat(service.isOutOfScopeIntent("Phòng có ban công hướng biển")).isFalse();
    }

    @Test
    @DisplayName("Should reject multi-room search requests with NEED_INFO (One-room MVP)")
    void shouldRejectMultiRoomSearch() {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Hà Nội")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(2)
                .build();

        ValidationResult result = service.validateForSearch(criteria, new ArrayList<>());

        assertThat(result.isValid()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NEED_INFO);
        assertThat(result.getMissingFields()).containsExactly("roomsCount");
        assertThat(result.getReply()).contains("Hiện chỉ hỗ trợ tìm một phòng cho mỗi phương án");
    }

    @Test
    @DisplayName("Should detect invalid date ranges where checkout is before or equal to checkin")
    void shouldDetectInvalidDateRanges() {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Hà Nội")
                .checkInDate(LocalDate.of(2026, 11, 22))
                .checkOutDate(LocalDate.of(2026, 11, 20))
                .adults(2)
                .roomsCount(1)
                .build();

        ValidationResult result = service.validateForSearch(criteria, new ArrayList<>());

        assertThat(result.isValid()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NEED_INFO);
        assertThat(result.getReply()).contains("Ngày trả phòng phải sau ngày nhận phòng");
    }

    @Test
    @DisplayName("Should detect missing destination and guest counts")
    void shouldDetectMissingRequiredFields() {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .build();

        ValidationResult result = service.validateForSearch(criteria, new ArrayList<>());

        assertThat(result.isValid()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NEED_INFO);
        assertThat(result.getMissingFields()).contains("locationKeyword", "adults");
    }

    @Test
    @DisplayName("Should merge patch correctly and record provenance")
    void shouldMergeCriteriaPatch() {
        RoomSearchCriteriaDto existing = RoomSearchCriteriaDto.builder()
                .locationKeyword("Đà Nẵng")
                .adults(1)
                .roomsCount(1)
                .build();

        CriteriaPatchDto patch = CriteriaPatchDto.builder()
                .adults(2)
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .preferredFeatureCodes(List.of("WIFI", "BALCONY"))
                .build();

        List<CriteriaEvidenceDto> provenance = new ArrayList<>();
        RoomSearchCriteriaDto merged = service.mergeCriteria(existing, patch, provenance);

        assertThat(merged.getLocationKeyword()).isEqualTo("Đà Nẵng");
        assertThat(merged.getAdults()).isEqualTo(2);
        assertThat(merged.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(merged.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 11, 22));
        assertThat(merged.getPreferredFeatureCodes()).containsExactly("WIFI", "BALCONY");

        assertThat(provenance).anyMatch(e -> "adults".equals(e.getField()) && e.getOrigin() == CriteriaOrigin.USER_PROVIDED);
        assertThat(provenance).anyMatch(e -> "checkInDate".equals(e.getField()) && e.getOrigin() == CriteriaOrigin.USER_PROVIDED);
    }

    @Test
    @DisplayName("Should validate successfully when all required search fields are present")
    void shouldValidateSuccessfully() {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .build();

        ValidationResult result = service.validateForSearch(criteria, new ArrayList<>());

        assertThat(result.isValid()).isTrue();
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.RESULTS);
        assertThat(result.getMissingFields()).isEmpty();
        assertThat(result.getCriteria().getChildren()).isEqualTo(0);
        assertThat(result.getCriteria().getExtraBeds()).isEqualTo(0);
    }
}
