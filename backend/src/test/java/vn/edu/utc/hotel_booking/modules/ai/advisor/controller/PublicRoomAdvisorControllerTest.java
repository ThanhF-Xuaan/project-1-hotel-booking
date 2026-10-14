package vn.edu.utc.hotel_booking.modules.ai.advisor.controller;

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
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorChatRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorRefreshRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorSearchRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.RoomAdvisorOrchestrationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionSecurityException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionVersionConflictException;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PublicRoomAdvisorControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RoomAdvisorOrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicRoomAdvisorController(orchestrationService))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/public/ai/room-advisor/chat should return 200 with ApiResponse")
    void shouldHandleChatRequest() throws Exception {
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng tại Đà Nẵng")
                .build();

        AdvisorResultDto mockResult = AdvisorResultDto.builder()
                .contractVersion("2.0")
                .requestId(UUID.randomUUID())
                .sessionId(UUID.randomUUID())
                .sessionVersion(1L)
                .status(AdvisorStatus.RESULTS)
                .reply("Tìm thấy phòng phù hợp.")
                .build();

        when(orchestrationService.chat(any(), any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Session-Owner", "test-user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.contractVersion").value("2.0"))
                .andExpect(jsonPath("$.result.status").value("RESULTS"));
    }

    @Test
    @DisplayName("POST /api/v1/public/ai/room-advisor/search should return 200")
    void shouldHandleSearchRequest() throws Exception {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .hotelId(1)
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .build();

        AdvisorSearchRequestDto request = AdvisorSearchRequestDto.builder()
                .contractVersion("2.0")
                .criteria(criteria)
                .build();

        AdvisorResultDto mockResult = AdvisorResultDto.builder()
                .contractVersion("2.0")
                .requestId(UUID.randomUUID())
                .sessionId(UUID.randomUUID())
                .sessionVersion(1L)
                .status(AdvisorStatus.RESULTS)
                .build();

        when(orchestrationService.search(any(), any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("RESULTS"));
    }

    @Test
    @DisplayName("POST /api/v1/public/ai/room-advisor/refresh should return 200")
    void shouldHandleRefreshRequest() throws Exception {
        AdvisorRefreshRequestDto request = AdvisorRefreshRequestDto.builder()
                .contractVersion("2.0")
                .sessionId(UUID.randomUUID())
                .expectedSessionVersion(1L)
                .build();

        AdvisorResultDto mockResult = AdvisorResultDto.builder()
                .contractVersion("2.0")
                .requestId(UUID.randomUUID())
                .sessionId(request.getSessionId())
                .sessionVersion(2L)
                .status(AdvisorStatus.RESULTS)
                .build();

        when(orchestrationService.refresh(any(), any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.sessionVersion").value(2));
    }

    @Test
    @DisplayName("Should handle SessionSecurityException with 403 Forbidden")
    void shouldHandleSessionSecurityException() throws Exception {
        AdvisorRefreshRequestDto request = AdvisorRefreshRequestDto.builder()
                .contractVersion("2.0")
                .sessionId(UUID.randomUUID())
                .build();

        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new SessionSecurityException("Access denied: session owner mismatch"));

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: session owner mismatch"));
    }

    @Test
    @DisplayName("Should handle SessionVersionConflictException with 409 Conflict")
    void shouldHandleSessionVersionConflictException() throws Exception {
        AdvisorRefreshRequestDto request = AdvisorRefreshRequestDto.builder()
                .contractVersion("2.0")
                .sessionId(UUID.randomUUID())
                .expectedSessionVersion(5L)
                .build();

        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new SessionVersionConflictException("Session version conflict"));

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }
}
