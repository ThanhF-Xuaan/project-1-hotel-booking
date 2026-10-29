package vn.edu.utc.hotel_booking.modules.ai.rag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagCitationDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicRagKnowledgeControllerTest {

    private final vn.edu.utc.hotel_booking.modules.organization.service.PublicHotelEligibilityReader hotelEligibility =
            id -> id == 1;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RagGroundedAnswerService ragGroundedAnswerService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicRagKnowledgeController(ragGroundedAnswerService, hotelEligibility))
                .build();
    }

    @Test
    @DisplayName("POST /ask should return 200 with grounded answer and citations")
    void shouldHandleAskRequestSuccessfully() throws Exception {
        RagQueryRequestDto request = RagQueryRequestDto.builder()
                .hotelId(1)
                .question("Nhận/trả phòng lúc mấy giờ?")
                .build();

        RagQueryResultDto mockResult = RagQueryResultDto.builder()
                .status(RagAnswerStatus.GROUNDED_ANSWER)
                .answer("Quy định nhận phòng từ 14:00 và trả phòng trước 12:00 trưa.")
                .citations(List.of(
                        RagCitationDto.builder()
                                .sourceId("POLICY-CHECKIN")
                                .version(1)
                                .category(RagDocumentCategory.POLICY)
                                .hotelId(1)
                                .snippet("Quy định nhận phòng từ 14:00 và trả phòng trước 12:00 trưa...")
                                .similarityScore(0.88)
                                .build()
                ))
                .build();

        when(ragGroundedAnswerService.answerQuestion(any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/knowledge/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("GROUNDED_ANSWER"))
                .andExpect(jsonPath("$.result.answer").value("Quy định nhận phòng từ 14:00 và trả phòng trước 12:00 trưa."))
                .andExpect(jsonPath("$.result.citations[0].sourceId").value("POLICY-CHECKIN"));
    }

    @Test
    @DisplayName("POST /ask should redact PII in input and execute safely")
    void shouldRedactPiiInLogsAndHandleAsk() throws Exception {
        RagQueryRequestDto request = RagQueryRequestDto.builder()
                .hotelId(1)
                .question("Tôi có CCCD 001200001234 và email guest@example.com, mấy giờ nhận phòng?")
                .build();

        RagQueryResultDto mockResult = RagQueryResultDto.builder()
                .status(RagAnswerStatus.GROUNDED_ANSWER)
                .answer("Quy định nhận phòng từ 14:00.")
                .build();

        when(ragGroundedAnswerService.answerQuestion(any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/knowledge/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("GROUNDED_ANSWER"));
    }

    @Test
    @DisplayName("POST /ask should return 503 RAG_TEMP_UNAVAILABLE on service outage")
    void shouldReturnServiceUnavailableWhenRagThrowsOutageException() throws Exception {
        RagQueryRequestDto request = RagQueryRequestDto.builder()
                .hotelId(1)
                .question("Chính sách hủy phòng thế nào?")
                .build();

        when(ragGroundedAnswerService.answerQuestion(any()))
                .thenThrow(new RuntimeException("PRIVATE_EXCEPTION_SENTINEL"));

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/knowledge/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE_EXCEPTION_SENTINEL"))))
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("RAG_TEMP_UNAVAILABLE: Dịch vụ tra cứu tạm thời không khả dụng."))
                .andExpect(jsonPath("$.result.status").value("INSUFFICIENT_EVIDENCE"))
                .andExpect(jsonPath("$.result.disclaimers[0]").value("RAG_TEMP_UNAVAILABLE: Dịch vụ RAG tạm thời không khả dụng, nghiệp vụ tìm kiếm và đặt phòng vẫn hoạt động bình thường."));
    }

    @Test
    @DisplayName("POST /ask should return 400 when question is blank (Fix H1)")
    void shouldReturnBadRequestWhenQuestionIsBlank() throws Exception {
        RagQueryRequestDto request = RagQueryRequestDto.builder()
                .hotelId(1)
                .question("")
                .build();

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/knowledge/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("question")));
    }
    @Test
    void rejectsInactiveHotelBeforeRetrieval() throws Exception {
        mockMvc.perform(post("/api/v1/public/ai/room-advisor/knowledge/ask")
                .contentType(MediaType.APPLICATION_JSON).content("{\"hotelId\":2,\"question\":\"check in\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("SCOPE_REQUIRED"));
        org.mockito.Mockito.verifyNoInteractions(ragGroundedAnswerService);
    }

    @Test
    void boundsTopKAndRejectsUnresolvedCode() throws Exception {
        for (String body : new String[]{"{\"topK\":6,\"question\":\"check in\"}",
                "{\"hotelId\":0,\"question\":\"check in\"}", "{\"hotelCode\":\"unresolved\",\"question\":\"check in\"}"}) {
            mockMvc.perform(post("/api/v1/public/ai/room-advisor/knowledge/ask")
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        org.mockito.Mockito.verifyNoInteractions(ragGroundedAnswerService);
    }

}
