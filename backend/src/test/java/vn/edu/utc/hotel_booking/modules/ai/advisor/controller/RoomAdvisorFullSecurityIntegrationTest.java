package vn.edu.utc.hotel_booking.modules.ai.advisor.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import vn.edu.utc.hotel_booking.common.config.JwtBlacklistFilter;
import vn.edu.utc.hotel_booking.common.config.SecurityConfig;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorChatRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.AdvisorSecurityProperties;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorPrincipalExtractor;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorRateLimiter;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorSessionCookieFactory;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorSessionTokenService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.RoomAdvisorOrchestrationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionNotFoundException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionSecurityException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionStorageUnavailableException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionVersionConflictException;
import vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-006: Comprehensive End-to-End Security Integration & Regression Suite.
 *
 * <p>Validates all acceptance criteria of TASK-006:
 * <ul>
 *   <li>AC-6.1: Cross-owner session access returns 404 indistinguishable from missing sessions across chat/search/refresh</li>
 *   <li>AC-6.2: Forged X-Session-Owner & X-Internal-Gateway-Key change nothing; controller source contains neither literal</li>
 *   <li>AC-6.3: Rate limit burst yields 429 with numeric code, DEC-06-01 error envelope, and positive Retry-After</li>
 *   <li>AC-6.4: Zero-write invariant verified via dependency/type scan and advisor runtime isolation</li>
 *   <li>AC-6.7: Exact cookie security attributes, rolling renewal, test-only secret injection, and error status mapping</li>
 * </ul>
 * </p>
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@TestPropertySource(properties =
        "hotel.ai.advisor.security.anon-token-secret=" + RoomAdvisorFullSecurityIntegrationTest.TEST_SECRET)
@ContextConfiguration(classes = {
        PublicRoomAdvisorController.class,
        SecurityConfig.class,
        RoomAdvisorFullSecurityIntegrationTest.TestSecurityContextConfig.class
})
class RoomAdvisorFullSecurityIntegrationTest {

    static final String TEST_SECRET = "T12-test-only-7f3c1a9e5b2d8460c4f19a3e7b2d5c80";
    private static final String RATE_LIMIT_TEST_ADDRESS = "203.0.113.88";

    private static final Set<String> FORBIDDEN_TYPE_NAMES = Set.of(
            "BookingService",
            "BookingSessionService",
            "PublicBookingController",
            "PublicBookingSessionController",
            "BookingController",
            "RoomInventoryHoldService",
            "RoomAvailabilityService",
            "PublicAvailabilityController",
            "PaymentService",
            "InvoiceService",
            "PaymentController",
            "InvoiceController",
            "StayGuestService",
            "LodgingQueueService",
            "ServiceOrderService",
            "BookingRepository",
            "RoomInventoryHoldRepository",
            "PaymentRepository",
            "StayGuestRepository",
            "HotelRepository",
            "RoomTypeRepository"
    );

    @Configuration
    @EnableWebMvc
    @EnableConfigurationProperties(AdvisorSecurityProperties.class)
    @Import(JwtBlacklistFilter.class)
    static class TestSecurityContextConfig {

        @Bean
        public RoomAdvisorSessionTokenService roomAdvisorSessionTokenService(
                AdvisorSecurityProperties properties) {
            return new RoomAdvisorSessionTokenService(
                    properties.getAnonTokenSecret(), properties.getAnonTokenSecondarySecret());
        }

        @Bean
        public RoomAdvisorSessionCookieFactory roomAdvisorSessionCookieFactory() {
            return new RoomAdvisorSessionCookieFactory();
        }

        @Bean
        public RoomAdvisorRateLimiter roomAdvisorRateLimiter() {
            return new RoomAdvisorRateLimiter(Clock.systemUTC());
        }

        @Bean
        public RoomAdvisorPrincipalExtractor roomAdvisorPrincipalExtractor(
                RoomAdvisorSessionTokenService tokenService) {
            return new RoomAdvisorPrincipalExtractor(tokenService, Clock.systemUTC());
        }

        @Bean
        public RoomAdvisorOrchestrationService roomAdvisorOrchestrationService() {
            RoomAdvisorOrchestrationService mock = mock(RoomAdvisorOrchestrationService.class);
            AdvisorResultDto dummyResult = AdvisorResultDto.builder()
                    .contractVersion("2.0")
                    .requestId(UUID.randomUUID())
                    .sessionId(UUID.randomUUID())
                    .sessionVersion(1L)
                    .status(AdvisorStatus.RESULTS)
                    .reply("Tư vấn thành công")
                    .build();
            when(mock.chat(any(), any())).thenReturn(dummyResult);
            when(mock.search(any(), any())).thenReturn(dummyResult);
            when(mock.refresh(any(), any())).thenReturn(dummyResult);
            return mock;
        }

        @Bean
        public TokenBlacklistService tokenBlacklistService() {
            return mock(TokenBlacklistService.class);
        }

        @Bean
        public JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper;
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomAdvisorOrchestrationService orchestrationService;

    @Autowired
    private RoomAdvisorSessionTokenService tokenService;

    @Autowired
    private AdvisorSecurityProperties securityProperties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(orchestrationService);
        stubOrchestration();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private void stubOrchestration() {
        AdvisorResultDto dummyResult = AdvisorResultDto.builder()
                .contractVersion("2.0")
                .requestId(UUID.randomUUID())
                .sessionId(UUID.randomUUID())
                .sessionVersion(1L)
                .status(AdvisorStatus.RESULTS)
                .reply("Tư vấn thành công")
                .build();
        when(orchestrationService.chat(any(), any())).thenReturn(dummyResult);
        when(orchestrationService.search(any(), any())).thenReturn(dummyResult);
        when(orchestrationService.refresh(any(), any())).thenReturn(dummyResult);
    }

    // =========================================================================
    // AC-6.1: Cross-Owner Session Access Returns 404 Indistinguishable
    // =========================================================================

    @Test
    @DisplayName("AC-6.1: Cross-owner session access on /chat returns 404 indistinguishable from missing session")
    void crossOwnerChatReturns404IndistinguishableFromMissing() throws Exception {
        UUID targetSessionId = UUID.randomUUID();
        String chatBody = String.format("""
                {
                  "contractVersion": "2.0",
                  "sessionId": "%s",
                  "message": "Xem chi tiết phòng"
                }
                """, targetSessionId);

        // 1. Foreign-owner access attempt -> SessionSecurityException
        when(orchestrationService.chat(any(), any()))
                .thenThrow(new SessionSecurityException("Access denied: session owner mismatch"));

        MockHttpServletResponse crossOwnerResp = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody))
                .andExpect(status().isNotFound())
                .andReturn().getResponse();

        // 2. Missing session attempt -> SessionNotFoundException
        reset(orchestrationService);
        when(orchestrationService.chat(any(), any()))
                .thenThrow(new SessionNotFoundException("Session not found: " + targetSessionId));

        MockHttpServletResponse missingResp = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody))
                .andExpect(status().isNotFound())
                .andReturn().getResponse();

        assert404Indistinguishability(crossOwnerResp, missingResp);
    }

    @Test
    @DisplayName("AC-6.1: Cross-owner session access on /search returns 404 indistinguishable from missing session")
    void crossOwnerSearchReturns404IndistinguishableFromMissing() throws Exception {
        UUID targetSessionId = UUID.randomUUID();
        String searchBody = String.format("""
                {
                  "contractVersion": "2.0",
                  "sessionId": "%s",
                  "criteria": {
                    "hotelId": 1,
                    "checkInDate": "2026-11-20",
                    "checkOutDate": "2026-11-22",
                    "adults": 2,
                    "roomsCount": 1
                  }
                }
                """, targetSessionId);

        // 1. Foreign-owner access attempt
        when(orchestrationService.search(any(), any()))
                .thenThrow(new SessionSecurityException("Access denied: session owner mismatch"));

        MockHttpServletResponse crossOwnerResp = mockMvc.perform(post("/api/v1/public/ai/room-advisor/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody))
                .andExpect(status().isNotFound())
                .andReturn().getResponse();

        // 2. Missing session attempt
        reset(orchestrationService);
        when(orchestrationService.search(any(), any()))
                .thenThrow(new SessionNotFoundException("Session not found: " + targetSessionId));

        MockHttpServletResponse missingResp = mockMvc.perform(post("/api/v1/public/ai/room-advisor/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody))
                .andExpect(status().isNotFound())
                .andReturn().getResponse();

        assert404Indistinguishability(crossOwnerResp, missingResp);
    }

    @Test
    @DisplayName("AC-6.1: Cross-owner session access on /refresh returns 404 indistinguishable from missing session")
    void crossOwnerRefreshReturns404IndistinguishableFromMissing() throws Exception {
        UUID targetSessionId = UUID.randomUUID();
        String refreshBody = String.format("""
                {
                  "contractVersion": "2.0",
                  "sessionId": "%s",
                  "expectedSessionVersion": 1
                }
                """, targetSessionId);

        // 1. Foreign-owner access attempt
        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new SessionSecurityException("Access denied: session owner mismatch"));

        MockHttpServletResponse crossOwnerResp = mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isNotFound())
                .andReturn().getResponse();

        // 2. Missing session attempt
        reset(orchestrationService);
        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new SessionNotFoundException("Session not found: " + targetSessionId));

        MockHttpServletResponse missingResp = mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isNotFound())
                .andReturn().getResponse();

        assert404Indistinguishability(crossOwnerResp, missingResp);
    }

    private void assert404Indistinguishability(MockHttpServletResponse resp1, MockHttpServletResponse resp2)
            throws Exception {
        assertThat(resp1.getStatus()).isEqualTo(404);
        assertThat(resp2.getStatus()).isEqualTo(404);

        // No Set-Cookie on 404
        assertThat(resp1.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        assertThat(resp2.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();

        JsonNode json1 = objectMapper.readTree(resp1.getContentAsString());
        JsonNode json2 = objectMapper.readTree(resp2.getContentAsString());

        // Identical error envelope structure
        assertThat(json1.path("code").asInt()).isEqualTo(404);
        assertThat(json2.path("code").asInt()).isEqualTo(404);
        assertThat(json1.path("message").asText()).isEqualTo("Không tìm thấy phiên tư vấn. Vui lòng bắt đầu phiên mới.");
        assertThat(json2.path("message").asText()).isEqualTo("Không tìm thấy phiên tư vấn. Vui lòng bắt đầu phiên mới.");

        JsonNode err1 = json1.path("error");
        JsonNode err2 = json2.path("error");
        assertThat(err1.path("contractVersion").asText()).isEqualTo("2.0");
        assertThat(err2.path("contractVersion").asText()).isEqualTo("2.0");
        assertThat(err1.path("reason").asText()).isEqualTo("SESSION_NOT_FOUND");
        assertThat(err2.path("reason").asText()).isEqualTo("SESSION_NOT_FOUND");
        assertThat(err1.path("retryable").asBoolean()).isFalse();
        assertThat(err2.path("retryable").asBoolean()).isFalse();

        // Unique UUID requestId on both
        UUID reqId1 = UUID.fromString(err1.path("requestId").asText());
        UUID reqId2 = UUID.fromString(err2.path("requestId").asText());
        assertThat(reqId1).isNotNull();
        assertThat(reqId2).isNotNull();

        // No internal error detail disclosure
        assertThat(resp1.getContentAsString()).doesNotContain("owner mismatch").doesNotContain("SessionSecurityException");
        assertThat(resp2.getContentAsString()).doesNotContain("owner mismatch").doesNotContain("SessionSecurityException");
    }

    // =========================================================================
    // AC-6.2: Forged Ownership Headers Stripped & Ignored
    // =========================================================================

    @Test
    @DisplayName("AC-6.2: Forged X-Session-Owner & X-Internal-Gateway-Key change nothing about ownership")
    void forgedOwnershipHeadersDoNotChangeOwnership() throws Exception {
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng tại Đà Nẵng")
                .build();
        String body = objectMapper.writeValueAsString(request);

        // 1. Authenticated user with forged headers -> ownership remains JWT subject
        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("verified-jwt-user-999")))
                        .header("X-Session-Owner", "attacker-forged-owner")
                        .header("X-Internal-Gateway-Key", "master-bypass-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // 2. Anonymous user with forged headers -> ownership remains signed cookie UUID
        UUID anonUuid = UUID.randomUUID();
        String anonToken = tokenService.mint(anonUuid, Instant.now().plus(Duration.ofMinutes(25)));

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .cookie(new Cookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME, anonToken))
                        .header("X-Session-Owner", "attacker-forged-owner")
                        .header("X-Internal-Gateway-Key", "master-bypass-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
        verify(orchestrationService, times(2)).chat(any(), ownerCaptor.capture());

        List<String> capturedOwners = ownerCaptor.getAllValues();
        assertThat(capturedOwners.get(0)).isEqualTo("auth:keycloak:verified-jwt-user-999");
        assertThat(capturedOwners.get(1)).isEqualTo("anon:" + anonUuid);
    }

    @Test
    @DisplayName("AC-6.2: Controller source contains neither X-Session-Owner nor X-Internal-Gateway-Key literal")
    void controllerSourceContainsNoForbiddenHeaderLiterals() throws Exception {
        Path controllerPath = Path.of(
                "src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/controller/PublicRoomAdvisorController.java");
        assertThat(Files.exists(controllerPath)).as("Controller source must exist").isTrue();

        String source = Files.readString(controllerPath);
        assertThat(source)
                .as("Controller source must never reference X-Session-Owner")
                .doesNotContain("X-Session-Owner");
        assertThat(source)
                .as("Controller source must never reference X-Internal-Gateway-Key")
                .doesNotContain("X-Internal-Gateway-Key");
    }

    // =========================================================================
    // AC-6.3: Rate Limit Burst Yields 429 With Error Envelope & Retry-After
    // =========================================================================

    @Test
    @DisplayName("AC-6.3: Burst traffic beyond budget yields 429 with numeric code=429, error envelope, and positive Retry-After")
    void burstRateLimitingYields429WithErrorEnvelopeAndRetryAfter() throws Exception {
        String token = tokenService.mint(UUID.randomUUID(), Instant.now().plus(Duration.ofMinutes(20)));
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng tại Đà Nẵng")
                .build();
        String body = objectMapper.writeValueAsString(request);

        // Exhaust the 15 burst requests for anonymous user
        for (int i = 0; i < RoomAdvisorRateLimiter.ANONYMOUS_BURST; i++) {
            mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .cookie(new Cookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME, token))
                            .with(servletReq -> {
                                servletReq.setRemoteAddr(RATE_LIMIT_TEST_ADDRESS);
                                return servletReq;
                            }))
                    .andExpect(status().isOk());
        }

        // 16th request must yield 429 Too Many Requests
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new Cookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME, token))
                        .with(servletReq -> {
                            servletReq.setRemoteAddr(RATE_LIMIT_TEST_ADDRESS);
                            return servletReq;
                        }))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();

        // 1. No Set-Cookie on 429
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();

        // 2. Positive numeric Retry-After header
        String retryAfter = response.getHeader("Retry-After");
        assertThat(retryAfter).isNotNull();
        int retryAfterSec = Integer.parseInt(retryAfter);
        assertThat(retryAfterSec).isPositive();

        // 3. Error response envelope conforms strictly to frozen ErrorResponse
        JsonNode root = objectMapper.readTree(response.getContentAsString());
        assertThat(root.path("code").asInt()).isEqualTo(429);
        assertThat(root.path("message").asText()).isEqualTo("Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.");

        JsonNode err = root.path("error");
        assertThat(err.path("contractVersion").asText()).isEqualTo("2.0");
        assertThat(err.path("reason").asText()).isEqualTo("RATE_LIMITED");
        assertThat(err.path("retryable").asBoolean()).isTrue();
        assertThat(UUID.fromString(err.path("requestId").asText())).isNotNull();

        // 4. Privacy: no leak of ownerKey or client IP
        assertThat(response.getContentAsString())
                .doesNotContain("ownerKey")
                .doesNotContain(RATE_LIMIT_TEST_ADDRESS);
    }

    // =========================================================================
    // AC-6.4: Zero-Write Invariant Verified
    // =========================================================================

    @Test
    @DisplayName("AC-6.4: Zero-Write Invariant - No advisor class references forbidden write services/repositories")
    void zeroWriteArchitectureScan() throws Exception {
        List<Class<?>> advisorClasses = scanAdvisorClasses();
        assertThat(advisorClasses).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (Class<?> clazz : advisorClasses) {
            for (Field field : clazz.getDeclaredFields()) {
                checkType(clazz, "field " + field.getName(), field.getType(), violations);
            }
            for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                for (Class<?> paramType : ctor.getParameterTypes()) {
                    checkType(clazz, "constructor parameter", paramType, violations);
                }
            }
            for (Method method : clazz.getDeclaredMethods()) {
                checkType(clazz, "method return " + method.getName(), method.getReturnType(), violations);
                for (Class<?> paramType : method.getParameterTypes()) {
                    checkType(clazz, "method param " + method.getName(), paramType, violations);
                }
            }
        }

        assertThat(violations)
                .withFailMessage("Zero-Write architecture violations detected in advisor module:\n" + String.join("\n", violations))
                .isEmpty();
    }

    private void checkType(Class<?> sourceClass, String context, Class<?> targetType, List<String> violations) {
        if (targetType == null) {
            return;
        }
        if (FORBIDDEN_TYPE_NAMES.contains(targetType.getSimpleName())) {
            violations.add(sourceClass.getName() + " -> " + context + " references forbidden write type: " + targetType.getName());
        }
    }

    private List<Class<?>> scanAdvisorClasses() throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String packageName = "vn.edu.utc.hotel_booking.modules.ai.advisor";
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);

        if (resource != null) {
            File directory = new File(resource.toURI());
            if (directory.exists()) {
                scanDirectory(directory, packageName, classes);
            }
        }
        return classes;
    }

    private void scanDirectory(File directory, String packageName, List<Class<?>> classes) throws ClassNotFoundException {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }
    }

    // =========================================================================
    // AC-6.7: Error Status Codes Conform to Advisor Error Envelope
    // =========================================================================

    @Test
    @DisplayName("AC-6.7: HTTP 400 Bad Request maps to INVALID_REQUEST (retryable=false)")
    void errorMapping400() throws Exception {
        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ malformed json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Yêu cầu không hợp lệ. Vui lòng kiểm tra lại dữ liệu đã gửi."))
                .andExpect(jsonPath("$.error.reason").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.contractVersion").value("2.0"))
                .andExpect(jsonPath("$.error.retryable").value(false))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("AC-6.7: HTTP 403 Forbidden maps exclusively to SCOPE_DENIED (retryable=false)")
    void errorMapping403() throws Exception {
        when(orchestrationService.chat(any(), any()))
                .thenThrow(new AccessDeniedException("Hotel scope is restricted"));

        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng")
                .build();

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Phạm vi tìm kiếm không được phép."))
                .andExpect(jsonPath("$.error.reason").value("SCOPE_DENIED"))
                .andExpect(jsonPath("$.error.contractVersion").value("2.0"))
                .andExpect(jsonPath("$.error.retryable").value(false))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("AC-6.7: HTTP 409 Conflict maps to SESSION_VERSION_CONFLICT (retryable=false)")
    void errorMapping409() throws Exception {
        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new SessionVersionConflictException("Session version conflict: expected 1, found 2"));

        String refreshBody = String.format("""
                {
                  "contractVersion": "2.0",
                  "sessionId": "%s",
                  "expectedSessionVersion": 1
                }
                """, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Phiên tư vấn đã thay đổi. Vui lòng tải lại trước khi tiếp tục."))
                .andExpect(jsonPath("$.error.reason").value("SESSION_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.error.contractVersion").value("2.0"))
                .andExpect(jsonPath("$.error.retryable").value(false))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("AC-6.7: HTTP 503 Service Unavailable maps to UPSTREAM_UNAVAILABLE (retryable=true)")
    void errorMapping503() throws Exception {
        when(orchestrationService.chat(any(), any()))
                .thenThrow(new SessionStorageUnavailableException("Redis cluster is temporarily unavailable"));

        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng")
                .build();

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau."))
                .andExpect(jsonPath("$.error.reason").value("UPSTREAM_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.contractVersion").value("2.0"))
                .andExpect(jsonPath("$.error.retryable").value(true))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    // =========================================================================
    // AC-6.7: Hardened Cookie Security Attributes & Rolling Renewal
    // =========================================================================

    @Test
    @DisplayName("AC-6.7: Hardened Cookie Attributes - Path, HttpOnly, Secure, SameSite=Lax")
    void hardenedCookieAttributes() throws Exception {
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng tại Đà Nẵng")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        Cookie cookie = response.getCookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo(RoomAdvisorSessionCookieFactory.COOKIE_PATH);
        assertThat(cookie.getMaxAge()).isPositive().isLessThanOrEqualTo(1800);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("AC-6.7: Rolling cookie renewal when remaining TTL <= 900s preserves session UUID")
    void rollingCookieRenewalWhenHalfLifeReached() throws Exception {
        UUID sessionUuid = UUID.randomUUID();
        // Token expiring in 10 minutes (600s <= 900s half-life)
        String nearExpiryToken = tokenService.mint(sessionUuid, Instant.now().plus(Duration.ofMinutes(10)));

        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tin nhắn kế tiếp")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .cookie(new Cookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME, nearExpiryToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        Cookie renewedCookie = response.getCookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME);
        assertThat(renewedCookie).isNotNull();

        // Verify renewed token preserves UUID and has full 1800s TTL
        var parsed = tokenService.verify(renewedCookie.getValue(), Instant.now());
        assertThat(parsed).isPresent();
        assertThat(parsed.get().owner()).isEqualTo(sessionUuid);
        assertThat(renewedCookie.getMaxAge()).isEqualTo(1800);
    }

    @Test
    @DisplayName("AC-6.7: No cookie renewal when remaining TTL > 900s")
    void noCookieRenewalWhenTtlFresh() throws Exception {
        UUID sessionUuid = UUID.randomUUID();
        // Token expiring in 25 minutes (1500s > 900s)
        String freshToken = tokenService.mint(sessionUuid, Instant.now().plus(Duration.ofMinutes(25)));

        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tin nhắn khi token còn mới")
                .build();

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .cookie(new Cookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME, freshToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    @DisplayName("AC-6.7: Test-only secret injection verified from properties")
    void testOnlySecretInjectionVerified() {
        assertThat(securityProperties.getAnonTokenSecret()).isEqualTo(TEST_SECRET);
        assertThat(securityProperties.getAnonTokenSecret().length()).isGreaterThanOrEqualTo(32);
    }
}
