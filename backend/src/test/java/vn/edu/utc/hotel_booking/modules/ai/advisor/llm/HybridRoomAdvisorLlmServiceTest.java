package vn.edu.utc.hotel_booking.modules.ai.advisor.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaPatchDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetBasis;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCoverage;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

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

    @Test
    @DisplayName("Should extract relative dates from Vietnamese message: từ ngày mai đến 3 ngày sau")
    void shouldExtractRelativeDatesFromVietnameseMessage() {
        String msg = "Tìm phòng đôi ở Đà Nẵng từ ngày mai đến 3 ngày sau cho 2 người lớn";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        assertThat(patch.getLocationKeyword()).isEqualTo("Đà Nẵng");
        LocalDate today = LocalDate.now();
        assertThat(patch.getCheckInDate()).isEqualTo(today.plusDays(1));
        assertThat(patch.getCheckOutDate()).isEqualTo(today.plusDays(4));
        assertThat(patch.getAdults()).isEqualTo(2);
    }

    @Test
    void extractsWeekendAsSaturdayToMondayUsingVietnameseClock() {
        Clock wednesday = Clock.fixed(Instant.parse("2026-11-11T02:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        var service = new HybridRoomAdvisorLlmService(wednesday);
        var patch = service.extractCriteriaPatch(
                "Tìm phòng đôi ở Đà Nẵng cuối tuần này cho 2 người lớn và 1 bé 4 tuổi", null);

        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(patch.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getChildren()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should extract relative range between two relative tokens: từ ngày mai đến ngày kia")
    void shouldExtractRelativeRangeTokens() {
        String msg = "Tìm phòng tại Đà Nẵng từ ngày mai đến ngày kia cho 1 người lớn";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        LocalDate today = LocalDate.now();
        assertThat(patch.getCheckInDate()).isEqualTo(today.plusDays(1));
        assertThat(patch.getCheckOutDate()).isEqualTo(today.plusDays(2));
    }

    @Test
    @DisplayName("Should extract relative check-in with stay duration: ngày mai ở 3 đêm")
    void shouldExtractRelativeCheckInWithStayDuration() {
        String msg = "Tìm phòng tại Đà Nẵng ngày mai ở 3 đêm cho 2 người lớn";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        LocalDate today = LocalDate.now();
        assertThat(patch.getCheckInDate()).isEqualTo(today.plusDays(1));
        assertThat(patch.getCheckOutDate()).isEqualTo(today.plusDays(4));
    }

    @Test
    @DisplayName("Should extract colloquial Vietnamese couple and date range in current month")
    void shouldExtractColloquialVietnameseCoupleAndThisMonthDates() {
        String msg = "Alo shop ơi mình với người yêu đi Đà Nẵng từ 20 đến 22 tháng này có phòng view biển không";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        LocalDate today = LocalDate.now();
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getLocationKeyword()).isEqualTo("Đà Nẵng");
        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(today.getYear(), today.getMonthValue(), 20));
        assertThat(patch.getCheckOutDate()).isEqualTo(LocalDate.of(today.getYear(), today.getMonthValue(), 22));
        assertThat(patch.getPreferredFeatureCodes()).contains("SEA_VIEW");
    }

    @Test
    @DisplayName("Should extract family with young kids and infants in colloquial Vietnamese")
    void shouldExtractColloquialFamilyWithChildrenAndInfants() {
        String msg = "Nhà mình 2 vợ chồng với 1 cu con 5 tuổi với 1 bé sơ sinh 6 tháng đi Hạ Long 2 đêm";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getChildren()).isEqualTo(1);
        assertThat(patch.getInfants()).isEqualTo(1);
        assertThat(patch.getLocationKeyword()).isEqualTo("Hạ Long");
    }

    @Test
    @DisplayName("Should extract single date in current month with stay duration")
    void shouldExtractSingleDateThisMonthWithDuration() {
        String msg = "Tìm phòng ở Nha Trang ngày 25 tháng này ở 2 đêm cho 2 người lớn";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        LocalDate today = LocalDate.now();
        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(today.getYear(), today.getMonthValue(), 25));
        assertThat(patch.getCheckOutDate()).isEqualTo(LocalDate.of(today.getYear(), today.getMonthValue(), 27));
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getLocationKeyword()).isEqualTo("Nha Trang");
    }

    @Test
    @DisplayName("Should extract Vietnamese slang budget 'củ', bathtub and sea view")
    void shouldExtractVietnameseSlangBudgetCuAndFeatures() {
        String msg = "Tìm phòng ở Đà Lạt cho hai đứa mình ngày 20 đến 22 tháng này giá tầm 1 củ rưỡi có bồn tắm nằm và view biển";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getLocationKeyword()).isEqualTo("Đà Lạt");
        assertThat(patch.getBudget()).isNotNull();
        assertThat(patch.getBudget().getAmount()).isEqualTo("1500000");
        assertThat(patch.getPreferredFeatureCodes()).contains("BATHTUB", "SEA_VIEW");
    }

    @Test
    @DisplayName("Should extract single adult, Saigon destination, and hard budget with 'đổ lại'")
    void shouldExtractSingleAdultSlangAndHardBudget() {
        String msg = "Mình đi một mình cần phòng ở Sài Gòn ngày 20 đến 22 tháng này giá dưới 2 củ đổ lại có hồ bơi";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        assertThat(patch.getAdults()).isEqualTo(1);
        assertThat(patch.getLocationKeyword()).isEqualTo("Hồ Chí Minh");
        LocalDate today = LocalDate.now();
        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(today.getYear(), today.getMonthValue(), 20));
        assertThat(patch.getCheckOutDate()).isEqualTo(LocalDate.of(today.getYear(), today.getMonthValue(), 22));
        assertThat(patch.getBudget()).isNotNull();
        assertThat(patch.getBudget().getAmount()).isEqualTo("2000000");
        assertThat(patch.getBudget().isHard()).isTrue();
        assertThat(patch.getPreferredFeatureCodes()).contains("POOL");
    }

    @Test
    @DisplayName("Should extract date range with explicit month without year")
    void shouldExtractDateRangeWithExplicitMonthWithoutYear() {
        String msg = "Tìm phòng ở Phú Quốc từ 15 đến 18 tháng 12 cho 2 người lớn";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        LocalDate today = LocalDate.now();
        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(today.getYear(), 12, 15));
        assertThat(patch.getCheckOutDate()).isEqualTo(LocalDate.of(today.getYear(), 12, 18));
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getLocationKeyword()).isEqualTo("Phú Quốc");
    }

    @Test
    @DisplayName("Should extract Vietnamese family with Ho Guom alias, 2 adults, 1 child, 1 infant")
    void shouldExtractFamilyWithHoGuomAndInfantSlang() {
        String msg = "Cuối tuần này mình muốn dắt cả nhà đi chơi quanh Hồ Gươm ngắm rùa, nhà có 2 vợ chồng với 2 đứa nhỏ (1 đứa 4 tuổi, 1 đứa ẵm ngửa) từ ngày 20 đến 22 tháng 11";
        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);
        assertThat(patch).isNotNull();
        assertThat(patch.getLocationKeyword()).isEqualTo("Hà Nội");
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getChildren()).isEqualTo(1);
        assertThat(patch.getInfants()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should extract colloquial landmarks to canonical cities in deterministic fallback")
    void shouldExtractColloquialLandmarksToCanonicalCitiesInDeterministicFallback() {
        // Hồ Gươm, Tháp Rùa, Bờ Hồ -> Hà Nội
        CriteriaPatchDto p1 = llmService.extractCriteriaPatch("đi ngắm rùa quanh Hồ Gươm 2 đêm", null);
        assertThat(p1.getLocationKeyword()).isEqualTo("Hà Nội");

        CriteriaPatchDto p2 = llmService.extractCriteriaPatch("muốn đi dạo bờ hồ ngắm tháp rùa từ 20 đến 22 tháng 11", null);
        assertThat(p2.getLocationKeyword()).isEqualTo("Hà Nội");

        // Bãi Cháy, Tuần Châu -> Hạ Long
        CriteriaPatchDto p3 = llmService.extractCriteriaPatch("ở gần bãi cháy ngắm vịnh cho 2 người", null);
        assertThat(p3.getLocationKeyword()).isEqualTo("Hạ Long");

        // Cầu Rồng, Mỹ Khê -> Đà Nẵng
        CriteriaPatchDto p4 = llmService.extractCriteriaPatch("đi tắm biển mỹ khê xem cầu rồng phun lửa", null);
        assertThat(p4.getLocationKeyword()).isEqualTo("Đà Nẵng");

        // Fansipan, Cát Cát -> Sa Pa
        CriteriaPatchDto p5 = llmService.extractCriteriaPatch("chinh phục đỉnh fansipan và đi bản cát cát", null);
        assertThat(p5.getLocationKeyword()).isEqualTo("Sa Pa");

        // Chợ Bến Thành, Quận 1 -> Hồ Chí Minh
        CriteriaPatchDto p6 = llmService.extractCriteriaPatch("đi dạo phố đi bộ nguyễn huệ ghé chợ bến thành", null);
        assertThat(p6.getLocationKeyword()).isEqualTo("Hồ Chí Minh");
    }

    @Test
    @DisplayName("Should build Gemini extraction prompt containing Vietnamese colloquial and slang rules")
    void shouldBuildGeminiExtractionPromptWithColloquialRules() {
        LocalDate date = LocalDate.of(2026, 11, 12);
        String prompt = HybridRoomAdvisorLlmService.buildGeminiExtractionPrompt(date, "Tìm phòng giá tầm 2 củ");

        assertThat(prompt).contains("Today is 2026-11-12");
        assertThat(prompt).contains("Vietnamese colloquial phrases and slang rules");
        assertThat(prompt).contains("củ");
        assertThat(prompt).contains("tháng này");
        assertThat(prompt).contains("Tìm phòng giá tầm 2 củ");
    }

    @Test
    void parsesVietnameseGroupedBudgetWithoutAssumingNightlyBasis() {
        String request = "Tìm phòng tại Hà Nội, nhận phòng 2026-11-09, trả phòng 2026-11-11, "
                + "2 người lớn, ngân sách tối đa 1.500.000 VND";

        var nightly = llmService.extractCriteriaPatch(request + " mỗi phòng mỗi đêm", null).getBudget();
        assertThat(nightly.getAmount()).isEqualTo("1500000");
        assertThat(nightly.getBasis()).isNull();

        var average = llmService.extractCriteriaPatch(request + " trung bình mỗi đêm theo tổng API", null).getBudget();
        assertThat(average.getAmount()).isEqualTo("1500000");
        assertThat(average.getBasis()).isEqualTo(BudgetBasis.AVERAGE_PER_NIGHT);
    }

    @Test
    void groupedStayBudgetRequiresExplicitQuoteCoverage() {
        assertThat(llmService.extractCriteriaPatch("ngân sách 500.000 VND", null).getBudget().getAmount()).isEqualTo("500000");
        assertThat(llmService.extractCriteriaPatch("ngân sách 500k", null).getBudget().getAmount()).isEqualTo("500000");
        var unspecifiedCoverage = llmService.extractCriteriaPatch(
                "Tìm phòng 2026-11-13 đến 2026-11-15 cho 2 người lớn, ngân sách tối đa 4.990.000 VND cả kỳ", null)
                .getBudget();
        assertThat(unspecifiedCoverage.getAmount()).isEqualTo("4990000");
        assertThat(unspecifiedCoverage.getCoverage()).isNull();

        var quotedTotal = llmService.extractCriteriaPatch(
                "Tìm phòng 2026-11-13 đến 2026-11-15 cho 2 người lớn, ngân sách tối đa 4.990.000 VND cả kỳ theo báo giá hệ thống", null)
                .getBudget();
        assertThat(quotedTotal.getAmount()).isEqualTo("4990000");
        assertThat(quotedTotal.getCoverage()).isEqualTo(BudgetCoverage.BACKEND_QUOTED_TOTAL);
    }

    @Test
    void treatsSingleDatesAsOneFieldAndRejectsInvalidCalendarDates() {
        var arrival = llmService.extractCriteriaPatch("Nhận phòng 2026-11-20", null);
        assertThat(arrival.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(arrival.getCheckOutDate()).isNull();

        var stay = RoomSearchCriteriaDto.builder().checkInDate(arrival.getCheckInDate()).build();
        var departure = llmService.extractCriteriaPatch("22/11/2026", stay);
        assertThat(departure.getCheckInDate()).isNull();
        assertThat(departure.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 11, 22));
        assertThat(llmService.extractCriteriaPatch("Trả phòng 31/02/2026", stay)).isNull();

        var mixed = llmService.extractCriteriaPatch("Nhận phòng 20/11/2026, trả phòng 2026-11-22", null);
        assertThat(mixed.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(mixed.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 11, 22));
    }

    @Test
    @DisplayName("Should extract zero children when message specifies không trẻ em")
    void shouldExtractZeroChildrenWhenNoChildrenSpecified() {
        String msg = "Tìm 1 phòng cho 2 người lớn, không trẻ em tại Synthetic City, 20–22/11/2026, tổng tối đa 950000 VND theo tổng API";

        CriteriaPatchDto patch = llmService.extractCriteriaPatch(msg, null);

        assertThat(patch).isNotNull();
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getChildren()).isEqualTo(0);
        assertThat(patch.getLocationKeyword()).isEqualTo("Synthetic City");
    }

    private static final String MESSAGE = "Tìm phòng tại Hà Nội cho 2 người lớn";

    @Test
    void normalizesSyntheticVietnameseExamples() throws Exception {
        var method = HybridRoomAdvisorLlmService.class.getDeclaredMethod("normalizeVietnameseSearch", String.class);
        method.setAccessible(true);
        assertThat((String) method.invoke(null, "hai người lớn và một bé")).isEqualTo("2 người lớn và 1 bé");
        assertThat((String) method.invoke(null, "1tr5")).isEqualTo("1.5 triệu");
        assertThat((String) method.invoke(null, "có điểm tâm")).isEqualTo("có ăn sáng");
    }

    @Test
    void understandsVietnameseTravelShorthandWithoutChangingNightCount() {
        var service = new HybridRoomAdvisorLlmService(Clock.fixed(Instant.parse("2026-11-11T02:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
        for (String duration : List.of("3 ngày 2 đêm", "3N2Đ", "3n2d")) {
            var patch = service.extractCriteriaPatch("Tìm phòng Đà Nẵng ngày mai " + duration + " cho hai người lớn và một bé", null);
            assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 12));
            assertThat(patch.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 11, 14));
            assertThat(patch.getAdults()).isEqualTo(2);
            assertThat(patch.getChildren()).isEqualTo(1);
        }
    }

    @Test
    void understandsUnaccentedDestinationGuestAbbreviationsAndCompactMoney() {
        var patch = llmService.extractCriteriaPatch("Tim phong Da Nang 2026-11-20 den 2026-11-22 cho 2NL1TE ngan sach 1tr5 theo báo giá hệ thống", null);
        assertThat(patch.getLocationKeyword()).isEqualTo("Đà Nẵng");
        assertThat(patch.getAdults()).isEqualTo(2);
        assertThat(patch.getChildren()).isEqualTo(1);
        assertThat(patch.getBudget().getAmount()).isEqualTo("1500000");
    }

    @Test
    void explicitDatesAndWeekendDurationTakePrecedenceOverDefaultWeekendStay() {
        var explicit = llmService.extractCriteriaPatch("Cuối tuần này ở Đà Nẵng 2026-11-20 đến 2026-11-21 cho 2 người lớn", null);
        assertThat(explicit.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(explicit.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 11, 21));
        var weekend = llmService.extractCriteriaPatch("Cuối tuần này ở Đà Nẵng 1 đêm cho hai vợ chồng", null);
        assertThat(weekend.getCheckOutDate()).isEqualTo(weekend.getCheckInDate().plusDays(1));
        assertThat(weekend.getAdults()).isEqualTo(2);
    }

    @Test
    void doesNotConfuseNearBeachOrNegatedAmenitiesWithPreferredRoomFeatures() {
        var patch = llmService.extractCriteriaPatch("Tìm phòng Đà Nẵng gần biển, không cần hồ bơi, có điểm tâm và wi-fi", null);
        assertThat(patch.getPreferredFeatureCodes()).contains("BREAKFAST", "WIFI").doesNotContain("POOL", "SEA_VIEW");
    }
    private static final String TEST_KEY = "synthetic-provider-key-never-sent-to-network";

    @Test
    void disabledProviderNeverReadsAnAvailableCredentialOrCallsTransport() {
        HttpClient client = mock(HttpClient.class);
        @SuppressWarnings("unchecked") Supplier<String> key = mock(Supplier.class);
        HybridRoomAdvisorLlmService service = new HybridRoomAdvisorLlmService(new AdvisorProviderProperties(), client, key);

        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verifyNoInteractions(client, key);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void enabledProviderStillRequiresANonblankKey(String key) {
        HttpClient client = mock(HttpClient.class);
        HybridRoomAdvisorLlmService service = new HybridRoomAdvisorLlmService(enabled(), client, () -> key);
        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verifyNoInteractions(client);
    }

    @Test
    void defaultAndExplicitPropertiesBindAndValidateInSpring() {
        ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            AdvisorProviderProperties properties = context.getBean(AdvisorProviderProperties.class);
            assertThat(properties.isGeminiEnabled()).isFalse();
            assertThat(properties.getTimeoutMillis()).isEqualTo(4000);
            assertThat(properties.getMaxOutputTokens()).isEqualTo(512);
        });
        runner.withPropertyValues("hotel.ai.advisor.provider.gemini-enabled=true",
                "hotel.ai.advisor.provider.timeout-millis=500", "hotel.ai.advisor.provider.max-output-tokens=256")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AdvisorProviderProperties properties = context.getBean(AdvisorProviderProperties.class);
                    assertThat(properties.permitsExternalCall()).isTrue();
                    assertThat(properties.getTimeoutMillis()).isEqualTo(500);
                    assertThat(properties.getMaxOutputTokens()).isEqualTo(256);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeout-millis=0", "timeout-millis=4001", "max-output-tokens=0", "max-output-tokens=1025"})
    void invalidProviderBoundsFailSpringStartup(String invalid) {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class)
                .withPropertyValues("hotel.ai.advisor.provider.gemini-enabled=true", "hotel.ai.advisor.provider." + invalid)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void canonicalEnvironmentOptInBindsThroughSpringRelaxedBinding() {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class)
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource("systemEnvironment", Map.of(
                                "HOTEL_AI_ADVISOR_PROVIDER_GEMINIENABLED", "true"))))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AdvisorProviderProperties.class).isGeminiEnabled()).isTrue();
                });
    }

    @Test
    void springInjectsTheBoundPropertiesWhileNoArgEvaluationRemainsOffline() {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class, HybridRoomAdvisorLlmService.class)
                .withPropertyValues("hotel.ai.advisor.provider.gemini-enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(org.springframework.test.util.ReflectionTestUtils.getField(
                            context.getBean(HybridRoomAdvisorLlmService.class), "provider"))
                            .isSameAs(context.getBean(AdvisorProviderProperties.class));
                    assertThat(context.getBean(AdvisorProviderProperties.class).isGeminiEnabled()).isTrue();
                });
        assertThat(new HybridRoomAdvisorLlmService().extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
    }

    @Test
    void manuallyInvalidBoundsFailClosedBeforeReadingCredentials() {
        AdvisorProviderProperties properties = enabled();
        properties.setMaxOutputTokens(1025);
        HttpClient client = mock(HttpClient.class);
        HybridRoomAdvisorLlmService service = new HybridRoomAdvisorLlmService(properties, client,
                () -> { throw new AssertionError("Invalid config must not read the key"); });
        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verifyNoInteractions(client);
    }

    @Test
    void explicitOptInSendsOnlyRedactedTextWithHeaderKeyAndTokenBudget() throws Exception {
        HttpClient client = mock(HttpClient.class);
        stubResponse(client, 200, candidate("STOP", "{\"adults\":4,\"checkInDate\":\"2026-11-20\"}"));
        AdvisorProviderProperties properties = enabled();
        properties.setTimeoutMillis(500);
        properties.setMaxOutputTokens(256);
        HybridRoomAdvisorLlmService service = new HybridRoomAdvisorLlmService(properties, client, () -> TEST_KEY);

        CriteriaPatchDto patch = service.extractCriteriaPatch(MESSAGE + " email synthetic@example.test CCCD 079190012345", null);
        assertThat(patch.getAdults()).isEqualTo(4);
        assertThat(patch.getCheckInDate()).isEqualTo(LocalDate.of(2026, 11, 20));

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).sendAsync(request.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        HttpRequest sent = request.getValue();
        assertThat(sent.uri().getHost()).isEqualTo("generativelanguage.googleapis.com");
        assertThat(sent.uri().getQuery()).isNull();
        assertThat(sent.uri().toString()).contains("/models/gemini-3.5-flash-lite:generateContent");
        assertThat(sent.uri().toString()).doesNotContain(TEST_KEY);
        assertThat(sent.headers().firstValue("x-goog-api-key")).contains(TEST_KEY);
        assertThat(sent.timeout()).contains(java.time.Duration.ofMillis(500));
        HttpResponse.BodySubscriber<String> payloadReader = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        sent.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override public void onSubscribe(Flow.Subscription subscription) { payloadReader.onSubscribe(subscription); }
            @Override public void onNext(ByteBuffer buffer) { payloadReader.onNext(List.of(buffer)); }
            @Override public void onError(Throwable error) { payloadReader.onError(error); }
            @Override public void onComplete() { payloadReader.onComplete(); }
        });
        String body = payloadReader.getBody().toCompletableFuture().get(1, TimeUnit.SECONDS);
        assertThat(body).doesNotContain("synthetic@example.test", "079190012345")
                .contains("[REDACTED_EMAIL]", "[REDACTED_CCCD]");
        JsonNode json = new ObjectMapper().readTree(body);
        assertThat(json.path("generationConfig").path("maxOutputTokens").asInt()).isEqualTo(256);
        assertThat(json.path("generationConfig").path("candidateCount").asInt()).isEqualTo(1);
        assertThat(json.path("generationConfig").path("responseMimeType").asText()).isEqualTo("application/json");
        assertThat(json.has("tools")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{}", "{\"adults\":\"not-a-number\"}", "{\"price\":100}"})
    void malformedOrUnexpectedModelOutputUsesDeterministicFallback(String modelJson) throws Exception {
        HttpClient client = mock(HttpClient.class);
        stubResponse(client, 200, candidate("STOP", modelJson));
        CriteriaPatchDto patch = new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY)
                .extractCriteriaPatch(MESSAGE, null);
        // An empty patch is valid JSON but cannot replace meaningful deterministic criteria.
        assertThat(patch.getAdults()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAX_TOKENS", "SAFETY"})
    void incompleteOrBlockedCandidatesCannotBecomeCriteria(String reason) throws Exception {
        HttpClient client = mock(HttpClient.class);
        stubResponse(client, 200, candidate(reason, "{\"adults\":4}"));
        assertThat(new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY)
                .extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void providerErrorsDoNotLogKeysPromptsOrResponseBodies(CapturedOutput output) {
        HttpClient client = mock(HttpClient.class);
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.failedFuture(new IOException(TEST_KEY + " synthetic@example.test")));
        assertThat(new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY)
                .extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        assertThat(output.getAll()).doesNotContain(TEST_KEY, "synthetic@example.test", MESSAGE);
        verify(client, times(1)).sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void nonSuccessHttpResponseFallsBackWithoutRetry() {
        HttpClient client = mock(HttpClient.class);
        stubResponse(client, 429, TEST_KEY);
        assertThat(new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY)
                .extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verify(client, times(1)).sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    @org.junit.jupiter.api.Timeout(3)
    void timeoutCancelsThePendingHttpCallAndFallsBack() {
        AdvisorProviderProperties properties = enabled();
        properties.setTimeoutMillis(100);
        HttpClient client = mock(HttpClient.class);
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(pending);
        assertThat(new HybridRoomAdvisorLlmService(properties, client, () -> TEST_KEY)
                .extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        assertThat(pending.isCancelled()).isTrue();
    }

    @Test
    void interruptCancelsThePendingCallAndPreservesTheInterruptFlag() throws Exception {
        HttpClient client = mock(HttpClient.class);
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        CountDownLatch started = new CountDownLatch(1);
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(invocation -> { started.countDown(); return pending; });
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean fallbackReturned = new AtomicBoolean();
        Thread worker = new Thread(() -> {
            CriteriaPatchDto patch = new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY).extractCriteriaPatch(MESSAGE, null);
            fallbackReturned.set(patch.getAdults() == 2);
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        worker.setDaemon(true);
        try {
            worker.start();
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            worker.interrupt();
            worker.join(2000);
            assertThat(worker.isAlive()).isFalse();
            assertThat(interrupted).isTrue();
            assertThat(fallbackReturned).isTrue();
            assertThat(pending.isCancelled()).isTrue();
        } finally {
            worker.interrupt();
            worker.join(2000);
        }
    }

    @Test
    void responseBodyIsBoundedDuringReceptionAndOverflowCancelsSubscription() {
        var subscriber = new HybridRoomAdvisorLlmService.BoundedBodySubscriber();
        Flow.Subscription subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[HybridRoomAdvisorLlmService.MAX_RESPONSE_BYTES])));
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1})));
        subscriber.onComplete();
        verify(subscription).cancel();
        assertThatThrownBy(() -> subscriber.getBody().toCompletableFuture().join()).hasCauseInstanceOf(IOException.class);
    }

    @Test
    void boundedResponsePreservesUtf8AcrossChunks() {
        var subscriber = new HybridRoomAdvisorLlmService.BoundedBodySubscriber();
        subscriber.onSubscribe(mock(Flow.Subscription.class));
        byte[] text = "Hà Nội".getBytes(StandardCharsets.UTF_8);
        subscriber.onNext(List.of(ByteBuffer.wrap(text, 0, 2), ByteBuffer.wrap(text, 2, text.length - 2)));
        subscriber.onComplete();
        assertThat(subscriber.getBody().toCompletableFuture().join()).isEqualTo("Hà Nội");
    }

    private AdvisorProviderProperties enabled() {
        AdvisorProviderProperties properties = new AdvisorProviderProperties();
        properties.setGeminiEnabled(true);
        return properties;
    }

    @Test void exhaustedSharedCampaignBlocksHttpBeforeSending() {
        HttpClient client = mock(HttpClient.class);
        var service = new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY, payload -> {
            throw new IllegalStateException("Synthetic cap reached");
        });
        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verifyNoInteractions(client);
    }

    @Test void reservationPrecedesHttpAndKnownUsageIsSettledWithoutCredentials() throws Exception {
        HttpClient client = mock(HttpClient.class);
        var reservation = mock(vn.edu.utc.hotel_booking.modules.ai.rag.service.RagOutboundLedger.Reservation.class);
        var reserved = new AtomicBoolean();
        String body = candidate("STOP", "{\"adults\":3}");
        var json = new ObjectMapper().readTree(body);
        ((com.fasterxml.jackson.databind.node.ObjectNode) json).set("usageMetadata",
                new ObjectMapper().valueToTree(Map.of("promptTokenCount", 10, "candidatesTokenCount", 5, "totalTokenCount", 18)));
        stubResponse(client, 200, json.toString());
        var service = new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY, payload -> {
            verifyNoInteractions(client);
            assertThat(payload).doesNotContain(TEST_KEY);
            reserved.set(true);
            return reservation;
        });
        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(3);
        assertThat(reserved).isTrue();
        verify(reservation).complete(10, 5, 18);
        verifyNoMoreInteractions(reservation);
    }

    @Test void unknownUsageCannotBeReportedAsSuccessfulProviderExtraction() throws Exception {
        HttpClient client = mock(HttpClient.class);
        var reservation = mock(vn.edu.utc.hotel_booking.modules.ai.rag.service.RagOutboundLedger.Reservation.class);
        doThrow(new IllegalStateException("Synthetic missing usage")).when(reservation).complete(-1, -1, -1);
        stubResponse(client, 200, candidate("STOP", "{\"adults\":3}"));
        var service = new HybridRoomAdvisorLlmService(enabled(), client, () -> TEST_KEY, payload -> reservation);
        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verify(reservation).complete(-1, -1, -1);
    }

    @Test void timeoutSettlesUnknownUsageAndCancelsHttp() {
        var properties = enabled(); properties.setTimeoutMillis(100);
        HttpClient client = mock(HttpClient.class);
        var pending = new CompletableFuture<HttpResponse<String>>();
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(pending);
        var reservation = mock(vn.edu.utc.hotel_booking.modules.ai.rag.service.RagOutboundLedger.Reservation.class);
        var service = new HybridRoomAdvisorLlmService(properties, client, () -> TEST_KEY, payload -> reservation);
        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verify(reservation).complete(-1, -1, -1);
        assertThat(pending.isCancelled()).isTrue();
    }

    @Test void offlineProviderDoesNotTouchCampaignOrHttp() {
        HttpClient client = mock(HttpClient.class);
        var service = new HybridRoomAdvisorLlmService(new AdvisorProviderProperties(), client, () -> TEST_KEY, payload -> {
            throw new AssertionError("Offline must not reserve");
        });
        assertThat(service.extractCriteriaPatch(MESSAGE, null).getAdults()).isEqualTo(2);
        verifyNoInteractions(client);
    }

    private String candidate(String finishReason, String modelJson) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of("candidates", List.of(Map.of(
                "finishReason", finishReason, "content", Map.of("parts", List.of(Map.of("text", modelJson)))))));
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(HttpClient client, int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(client.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AdvisorProviderProperties.class)
    static class PropertiesConfig { }
}
