package vn.edu.utc.hotel_booking.modules.ai.advisor.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaPatchDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HybridRoomAdvisorLlmServiceTest {

    private HybridRoomAdvisorLlmService llmService;

    @BeforeEach
    void setUp() {
        llmService = new HybridRoomAdvisorLlmService();
    }

    @Test
    @DisplayName("Should extract dates, guests, destination, and budget from Vietnamese message")
    void shouldExtractCriteriaFromVietnameseMessage() {
        String msg = "Tìm giúp tôi phòng ở Đà Nẵng từ 2026-11-20 đến 2026-11-22 cho 2 người lớn và 1 trẻ em, ngân sách dưới 2 triệu, có ban công";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        assertThat(patch.getLocationKeyword()).isEqualTo("Đà Nẵng");
        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(patch.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 11, 22));
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getChildren()).isEqualTo(1);
        assertThat(patch.getPreferredFeatureCodes()).contains("BALCONY");
        assertThat(patch.getBudget()).isNotNull();
        assertThat(patch.getBudget().getAmount()).isEqualTo("2000000");
    }

    @Test
    @DisplayName("Should extract dd/MM/yyyy date format")
    void shouldExtractVnDateFormat() {
        String msg = "Mình cần phòng tại Hà Nội từ 15/12/2026 đến 18/12/2026 cho 1 người lớn";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        assertThat(patch.getLocationKeyword()).isEqualTo("Hà Nội");
        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(patch.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 12, 18));
        assertThat(patch.getAdults()).isEqualTo(1);
    }
}
