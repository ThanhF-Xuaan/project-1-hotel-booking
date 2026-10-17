package vn.edu.utc.hotel_booking.modules.ai.personalization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.StayHistoryRecordDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.enums.PersonalizationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.privacy.PersonalizationPrivacyGuard;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.GuestPersonalizationService;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.PersonalizationBaselineEvaluator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GuestPersonalizationController – Unit Tests (MockMvc)")
class GuestPersonalizationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PersonalizationPrivacyGuard privacyGuard = new PersonalizationPrivacyGuard();
        GuestPersonalizationService personalizationService = new GuestPersonalizationService(privacyGuard);
        PersonalizationBaselineEvaluator baselineEvaluator = new PersonalizationBaselineEvaluator(personalizationService, privacyGuard);

        GuestPersonalizationController controller = new GuestPersonalizationController(personalizationService, baselineEvaluator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("TC-20: POST /api/v1/ai/personalization/recommend thành công -> trả về 200 và kết quả phân tích")
    void tc20_recommendEndpoint_shouldReturnSuccess() throws Exception {
        GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
                .guestId(301L)
                .consentGranted(true)
                .referenceDate(LocalDate.of(2026, 10, 1))
                .stayHistory(List.of(
                        StayHistoryRecordDto.builder()
                                .hotelId(1).hotelRoomTypeId(10).roomTypeCode("DLX").roomTypeName("Deluxe")
                                .checkInDate(LocalDate.of(2026, 8, 1)).checkOutDate(LocalDate.of(2026, 8, 3))
                                .adults(2)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/ai/personalization/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.guestId").value(301))
                .andExpect(jsonPath("$.result.status").value(PersonalizationStatus.PERSONALIZED_READY.name()))
                .andExpect(jsonPath("$.result.nonBindingSuggestion").value(true))
                .andExpect(jsonPath("$.result.pricingOrPromotionMutated").value(false));
    }

    @Test
    @DisplayName("TC-21: POST /api/v1/ai/personalization/evaluate-baseline thành công -> trả về 200 và báo cáo holdout")
    void tc21_evaluateBaselineEndpoint_shouldReturnSuccess() throws Exception {
        GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
                .guestId(302L)
                .consentGranted(true)
                .stayHistory(List.of(
                        StayHistoryRecordDto.builder()
                                .hotelId(1).hotelRoomTypeId(10).roomTypeCode("DLX").roomTypeName("Deluxe")
                                .checkInDate(LocalDate.of(2026, 5, 1)).checkOutDate(LocalDate.of(2026, 5, 3))
                                .build(),
                        StayHistoryRecordDto.builder()
                                .hotelId(1).hotelRoomTypeId(20).roomTypeCode("STE").roomTypeName("Suite")
                                .checkInDate(LocalDate.of(2026, 7, 1)).checkOutDate(LocalDate.of(2026, 7, 3))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/ai/personalization/evaluate-baseline?k=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.totalEvaluatedStays").value(1))
                .andExpect(jsonPath("$.result.leakageFreeTimeSplit").value(true));
    }

    @Test
    @DisplayName("TC-22: Bean Validation lỗi (thiếu guestId hoặc maxSuggestions <= 0) -> trả về 400 Bad Request")
    void tc22_validationFailure_shouldReturn400() throws Exception {
        // Request không có guestId và maxSuggestions = 0
        String invalidJson = """
                {
                  "consentGranted": true,
                  "maxSuggestions": 0
                }
                """;

        mockMvc.perform(post("/api/v1/ai/personalization/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("TC-23: Khai báo @PreAuthorize đầy đủ các quyền nghiệp vụ yêu cầu")
    void tc23_preAuthorizeAnnotationVerification() {
        PreAuthorize preAuth = GuestPersonalizationController.class.getAnnotation(PreAuthorize.class);
        assertThat(preAuth).isNotNull();
        String val = preAuth.value();
        assertThat(val).contains("CHAIN_ADMIN");
        assertThat(val).contains("PROPERTY_MANAGER");
        assertThat(val).contains("RECEPTIONIST");
        assertThat(val).contains("SALES_GROUP");
        assertThat(val).contains("GUEST");
    }

    @Test
    @DisplayName("TC-24: Bean Validation lỗi khi stayHistory vượt quá 100 bản ghi -> 400 Bad Request")
    void tc24_stayHistoryExceeds100_shouldReturn400BadRequest() throws Exception {
        List<StayHistoryRecordDto> largeList = new ArrayList<>();
        for (int i = 1; i <= 101; i++) {
            largeList.add(StayHistoryRecordDto.builder()
                    .hotelId(1)
                    .hotelRoomTypeId(1)
                    .roomTypeCode("STD")
                    .roomTypeName("Standard")
                    .checkInDate(LocalDate.of(2026, 1, 1).plusDays(i))
                    .checkOutDate(LocalDate.of(2026, 1, 2).plusDays(i))
                    .build());
        }

        GuestPersonalizationRequestDto request = GuestPersonalizationRequestDto.builder()
                .guestId(999L)
                .consentGranted(true)
                .stayHistory(largeList)
                .build();

        mockMvc.perform(post("/api/v1/ai/personalization/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("stayHistory cannot exceed 100 records")));
    }
}
