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
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionSecurityException;
import vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Security slice for the advisor endpoints.
 *
 * <p>Uses the real read-only {@link SecurityConfig} CORS configuration and a synthetic signing
 * secret supplied through a test property. Browser cookie behaviour is explicitly not claimed.</p>
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@TestPropertySource(properties =
        "hotel.ai.advisor.security.anon-token-secret=" + PublicRoomAdvisorSecurityIntegrationTest.TEST_SECRET)
@ContextConfiguration(classes = {
        PublicRoomAdvisorController.class,
        SecurityConfig.class,
        PublicRoomAdvisorSecurityIntegrationTest.TestSecurityContextConfig.class
})
class PublicRoomAdvisorSecurityIntegrationTest {

    static final String TEST_SECRET = "T12-test-only-7f3c1a9e5b2d8460c4f19a3e7b2d5c80";

    /** Dedicated client address so the rate-limit test never shares the IP budget of other tests. */
    private static final String RATE_LIMIT_TEST_ADDRESS = "203.0.113.77";

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

    // ------------------------------------------------------------------
    // Existing anonymous endpoint behaviour
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Canonical /api/v1/public/ai/room-advisor/chat allows anonymous access without CSRF token (200 OK)")
    void canonicalChatEndpointAllowsAnonymousAccessWithoutCsrf() throws Exception {
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng tại Đà Nẵng")
                .build();

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("Canonical /api/v1/public/ai/room-advisor/search allows anonymous access without CSRF token (200 OK)")
    void canonicalSearchEndpointAllowsAnonymousAccessWithoutCsrf() throws Exception {
        String searchBody = """
                {
                  "contractVersion": "2.0",
                  "criteria": {
                    "hotelId": 1,
                    "checkInDate": "2026-11-20",
                    "checkOutDate": "2026-11-22",
                    "adults": 2,
                    "roomsCount": 1
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("Undocumented /api/v1/ai/room-advisor/chat is blocked from anonymous access (401 Unauthorized)")
    void undocumentedAliasIsBlockedFromAnonymousAccess() throws Exception {
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng trái phép qua alias")
                .build();

        mockMvc.perform(post("/api/v1/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("Canonical /api/v1/public/ai/room-advisor/refresh allows anonymous access without CSRF token (200 OK)")
    void canonicalRefreshEndpointAllowsAnonymousAccessWithoutCsrf() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("Undocumented /api/v1/ai/room-advisor/refresh is blocked from anonymous access (401 Unauthorized)")
    void undocumentedRefreshAliasIsBlockedFromAnonymousAccess() throws Exception {
        String refreshBody = String.format("""
                {
                  "contractVersion": "2.0",
                  "sessionId": "%s"
                }
                """, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    // ------------------------------------------------------------------
    // AC-2.6 / AC-2.8 / AC-2.11: wiring, cookie and CORS
    // ------------------------------------------------------------------

    @Test
    @DisplayName("anonymous successful chat receives exactly one hardened Set-Cookie")
    void anonymousChatReceivesOneHardenedCookie() throws Exception {
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
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(1);
        Cookie cookie = response.getCookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo(RoomAdvisorSessionCookieFactory.COOKIE_PATH);
        assertThat(cookie.getMaxAge()).isEqualTo(
                (int) RoomAdvisorSessionTokenService.DEFAULT_SESSION_TTL.toSeconds());
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("a foreign-owner session is reported as the same 404 and never writes a cookie")
    void foreignOwnerSessionReturns404WithoutCookie() throws Exception {
        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new SessionSecurityException("Access denied: session owner mismatch"));

        String refreshBody = String.format("""
                {
                  "contractVersion": "2.0",
                  "sessionId": "%s",
                  "expectedSessionVersion": 1
                }
                """, UUID.randomUUID());

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.error.reason").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.contractVersion").value("2.0"))
                .andExpect(jsonPath("$.error.retryable").value(false))
                .andReturn()
                .getResponse();

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        assertThat(response.getContentAsString()).doesNotContain("owner mismatch");
    }

    @Test
    @DisplayName("forged ownership headers do not change the JWT-derived owner")
    void forgedOwnershipHeadersDoNotChangeJwtOwner() throws Exception {
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng tại Đà Nẵng")
                .build();
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("keycloak-subject-42")))
                        .header("X-Session-Owner", "forged-owner")
                        .header("X-Internal-Gateway-Key", "forged-gateway")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject("keycloak-subject-42")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<String> owners = ArgumentCaptor.forClass(String.class);
        verify(orchestrationService, times(2)).chat(any(), owners.capture());
        assertThat(owners.getAllValues())
                .containsExactly("auth:keycloak:keycloak-subject-42", "auth:keycloak:keycloak-subject-42");
    }

    @Test
    @DisplayName("allowed origin credentialed preflight succeeds with the real CORS configuration")
    void allowedOriginCredentialedPreflightSucceeds() throws Exception {
        mockMvc.perform(options("/api/v1/public/ai/room-advisor/chat")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("denied origin is rejected and receives no allow-origin header")
    void deniedOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/public/ai/room-advisor/chat")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("the three documented advisor operations remain POST-only")
    void threePostOperationsArePresent() throws Exception {
        String chatBody = """
                {"contractVersion":"2.0","message":"Tìm phòng tại Đà Nẵng"}
                """;
        String searchBody = """
                {"contractVersion":"2.0","criteria":{"hotelId":1,"checkInDate":"2026-11-20","checkOutDate":"2026-11-22","adults":2,"roomsCount":1}}
                """;
        String refreshBody = String.format("""
                {"contractVersion":"2.0","sessionId":"%s","expectedSessionVersion":1}
                """, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON).content(chatBody))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/public/ai/room-advisor/search")
                        .contentType(MediaType.APPLICATION_JSON).content(searchBody))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // AC-3.1 / AC-3.3 / AC-3.6: bounded rate limiting with the real security chain
    // ------------------------------------------------------------------

    @Test
    @DisplayName("AC-3.6: an exhausted owner budget answers 429 with Retry-After and the advisor envelope")
    void rateLimitedRequestIsRejectedWithRetryAfter() throws Exception {
        String token = tokenService.mint(UUID.randomUUID(), Instant.now().plus(Duration.ofMinutes(20)));
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng tại Đà Nẵng")
                .build();
        String body = objectMapper.writeValueAsString(request);

        for (int attempt = 0; attempt < RoomAdvisorRateLimiter.ANONYMOUS_BURST; attempt++) {
            mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .cookie(new Cookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME, token))
                            .with(servletRequest -> {
                                servletRequest.setRemoteAddr(RATE_LIMIT_TEST_ADDRESS);
                                return servletRequest;
                            }))
                    .andExpect(status().isOk());
        }

        MvcResult result = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(new Cookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME, token))
                        .with(servletRequest -> {
                            servletRequest.setRemoteAddr(RATE_LIMIT_TEST_ADDRESS);
                            return servletRequest;
                        }))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        String retryAfter = response.getHeader("Retry-After");
        assertThat(retryAfter).isNotNull();
        assertThat(Integer.parseInt(retryAfter)).isPositive();

        JsonNode errorBody = objectMapper.readTree(response.getContentAsString());
        assertThat(errorBody.path("code").asInt()).isEqualTo(429);
        assertThat(errorBody.path("error").path("reason").asText()).isEqualTo("RATE_LIMITED");
        assertThat(errorBody.path("error").path("contractVersion").asText()).isEqualTo("2.0");
        assertThat(errorBody.path("error").path("retryable").isBoolean()).isTrue();
        assertThat(UUID.fromString(errorBody.path("error").path("requestId").asText())).isNotNull();
        assertThat(response.getContentAsString()).doesNotContain("ownerKey").doesNotContain(RATE_LIMIT_TEST_ADDRESS);
    }
}
