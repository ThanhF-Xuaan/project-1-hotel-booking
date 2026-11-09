package vn.edu.utc.hotel_booking.modules.ai.advisor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.UnifiedAdvisorRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.UnifiedAdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.UnifiedIntent;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RateLimitExceededException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorPrincipalExtractor;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorRateLimiter;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorSessionCookieFactory;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorSessionTokenService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.UnifiedAdvisorService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionNotFoundException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionVersionConflictException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PublicUnifiedAdvisorControllerTest {

    private static final String TEST_SECRET = "T12-test-only-7f3c1a9e5b2d8460c4f19a3e7b2d5c80";
    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RoomAdvisorSessionTokenService tokenService;
    private RoomAdvisorSessionCookieFactory cookieFactory;
    private RoomAdvisorRateLimiter rateLimiter;

    @Mock
    private UnifiedAdvisorService unifiedAdvisorService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        tokenService = new RoomAdvisorSessionTokenService(TEST_SECRET, null);
        cookieFactory = new RoomAdvisorSessionCookieFactory();
        RoomAdvisorPrincipalExtractor extractor =
                new RoomAdvisorPrincipalExtractor(tokenService, Clock.fixed(NOW, ZoneOffset.UTC));
        rateLimiter = new RoomAdvisorRateLimiter(Clock.fixed(NOW, ZoneOffset.UTC));

        mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicUnifiedAdvisorController(
                        unifiedAdvisorService, extractor, cookieFactory, rateLimiter))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Converse success returns 200 OK and sets session cookie")
    void converseSuccessReturnsEnvelopeAndCookie() throws Exception {
        UnifiedAdvisorResultDto sampleResult = UnifiedAdvisorResultDto.builder()
                .contractVersion("2.1-proposed")
                .requestId(UUID.randomUUID())
                .intent(UnifiedIntent.SEARCH_AND_POLICY)
                .build();

        when(unifiedAdvisorService.converse(any(), any())).thenReturn(sampleResult);

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng Đà Nẵng và ăn sáng mấy giờ?")
                .build();

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/converse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.result.contractVersion", is("2.1-proposed")))
                .andExpect(jsonPath("$.result.intent", is("SEARCH_AND_POLICY")));
    }

    @Test
    @DisplayName("Blank message returns 400 INVALID_REQUEST")
    void blankMessageReturns400() throws Exception {
        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("   ")
                .build();

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/converse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.reason", is("INVALID_REQUEST")));
    }

    @Test
    @DisplayName("SessionNotFoundException returns 404 SESSION_NOT_FOUND")
    void sessionNotFoundReturns404() throws Exception {
        when(unifiedAdvisorService.converse(any(), any()))
                .thenThrow(new SessionNotFoundException("Session expired"));

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Ăn sáng mấy giờ?")
                .sessionId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/converse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.reason", is("SESSION_NOT_FOUND")));
    }

    @Test
    @DisplayName("SessionVersionConflictException returns 409 SESSION_VERSION_CONFLICT")
    void versionConflictReturns409() throws Exception {
        when(unifiedAdvisorService.converse(any(), any()))
                .thenThrow(new SessionVersionConflictException("Conflict"));

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng")
                .sessionId(UUID.randomUUID())
                .expectedSessionVersion(1L)
                .build();

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/converse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.reason", is("SESSION_VERSION_CONFLICT")));
    }

    @Test
    @DisplayName("RateLimitExceededException returns 429 RATE_LIMITED with Retry-After header")
    void rateLimitReturns429WithHeader() throws Exception {
        when(unifiedAdvisorService.converse(any(), any()))
                .thenThrow(new RateLimitExceededException(10));

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng")
                .build();

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/converse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "10"))
                .andExpect(jsonPath("$.error.reason", is("RATE_LIMITED")));
    }
}
