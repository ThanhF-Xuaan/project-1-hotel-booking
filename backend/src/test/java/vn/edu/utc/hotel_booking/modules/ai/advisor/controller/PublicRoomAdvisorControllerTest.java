package vn.edu.utc.hotel_booking.modules.ai.advisor.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorChatRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorRefreshRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorSearchRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorPrincipalExtractor;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorRateLimiter;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorSessionCookieFactory;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorSessionTokenService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.RoomAdvisorOrchestrationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionNotFoundException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionSecurityException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionStorageUnavailableException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionVersionConflictException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-boundary tests for TASK-002.
 *
 * <p>The extractor and token service are real objects with a fixed clock, so cookie issuance,
 * renewal and zero-trust ownership are exercised end to end without loading Spring Security. The
 * frozen schema is read from the repository; it is never edited here.</p>
 */
@ExtendWith(MockitoExtension.class)
class PublicRoomAdvisorControllerTest {

    private static final String TEST_SECRET = "T12-test-only-7f3c1a9e5b2d8460c4f19a3e7b2d5c80";
    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final UUID ANON_OWNER = UUID.fromString("3f1c5a7e-2b4d-4e86-9a10-7c2d8e5f4a31");
    private static final String ANON_OWNER_KEY = "anon:" + ANON_OWNER;
    private static final String SESSION_COOKIE = RoomAdvisorSessionCookieFactory.COOKIE_NAME;
    private static final int SESSION_TTL_SECONDS = (int) RoomAdvisorSessionTokenService.DEFAULT_SESSION_TTL.toSeconds();
    private static final String SCHEMA_RELATIVE_PATH =
            "docs/t12/r1-00/v2.0.0-draft.1/schemas/advisor.schema.json";
    private static final String OPENAPI_RELATIVE_PATH =
            "docs/t12/r1-00/v2.0.0-draft.1/openapi.json";
    private static final String CONTROLLER_RELATIVE_PATH =
            "backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/controller/PublicRoomAdvisorController.java";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RoomAdvisorSessionTokenService tokenService;
    private RoomAdvisorSessionCookieFactory cookieFactory;
    private RoomAdvisorRateLimiter rateLimiter;

    @Mock
    private RoomAdvisorOrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        tokenService = new RoomAdvisorSessionTokenService(TEST_SECRET, null);
        cookieFactory = new RoomAdvisorSessionCookieFactory();
        RoomAdvisorPrincipalExtractor extractor =
                new RoomAdvisorPrincipalExtractor(tokenService, Clock.fixed(NOW, ZoneOffset.UTC));
        rateLimiter = new RoomAdvisorRateLimiter(Clock.fixed(NOW, ZoneOffset.UTC));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicRoomAdvisorController(
                        orchestrationService, extractor, cookieFactory, rateLimiter))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // AC-2.8 / AC-2.9 / AC-2.11: success, cookie minting and renewal
    // ------------------------------------------------------------------

    @Test
    @DisplayName("chat success returns shared ApiResponse and exactly one hardened session cookie")
    void chatSuccessWritesExactlyOneHardenedCookie() throws Exception {
        when(orchestrationService.chat(any(), any())).thenReturn(result());

        MvcResult result = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.contractVersion").value("2.0"))
                .andExpect(jsonPath("$.result.status").value("RESULTS"))
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(1);
        assertHardenedCookie(response, SESSION_TTL_SECONDS);
        verify(orchestrationService).chat(any(), any());
    }

    @Test
    @DisplayName("search success preserves the shared ApiResponse and issues one cookie")
    void searchSuccessWritesExactlyOneCookie() throws Exception {
        when(orchestrationService.search(any(), any())).thenReturn(result());

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.status").value("RESULTS"))
                .andReturn()
                .getResponse();

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(1);
        assertHardenedCookie(response, SESSION_TTL_SECONDS);
    }

    @Test
    @DisplayName("refresh success preserves the shared ApiResponse and issues one cookie")
    void refreshSuccessWritesExactlyOneCookie() throws Exception {
        when(orchestrationService.refresh(any(), any())).thenReturn(result());

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.sessionVersion").value(1))
                .andReturn()
                .getResponse();

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(1);
        assertHardenedCookie(response, SESSION_TTL_SECONDS);
    }

    @Test
    @DisplayName("a token above half-life is reused without writing a cookie")
    void existingTokenAboveHalfLifeWritesNoCookie() throws Exception {
        when(orchestrationService.chat(any(), any())).thenReturn(result());
        String token = tokenService.mint(ANON_OWNER, NOW.plus(Duration.ofMinutes(20)));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest()))
                        .cookie(new Cookie(SESSION_COOKIE, token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        verify(orchestrationService).chat(any(), eq(ANON_OWNER_KEY));
    }

    @Test
    @DisplayName("a token at/below half-life is renewed with the same owner and a refreshed Max-Age")
    void renewalKeepsOwnerAndRefreshesCookieLifetime() throws Exception {
        when(orchestrationService.chat(any(), any())).thenReturn(result());
        String oldToken = tokenService.mint(ANON_OWNER, NOW.plus(Duration.ofMinutes(10)));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest()))
                        .cookie(new Cookie(SESSION_COOKIE, oldToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(1);
        Cookie renewed = response.getCookie(SESSION_COOKIE);
        assertThat(renewed).isNotNull();
        assertThat(renewed.getValue()).isNotEqualTo(oldToken);
        assertThat(renewed.getMaxAge()).isEqualTo(SESSION_TTL_SECONDS);

        RoomAdvisorSessionTokenService.VerifiedToken verified =
                tokenService.verify(renewed.getValue(), NOW).orElseThrow();
        assertThat(verified.owner()).isEqualTo(ANON_OWNER);
        assertThat(verified.exp()).isEqualTo(NOW.plusSeconds(SESSION_TTL_SECONDS));
        verify(orchestrationService).chat(any(), eq(ANON_OWNER_KEY));
    }

    @Test
    @DisplayName("authenticated JWT owner never receives an anonymous cookie")
    void authenticatedOwnerWritesNoCookie() throws Exception {
        when(orchestrationService.chat(any(), any())).thenReturn(result());
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("keycloak-subject-42")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        verify(orchestrationService).chat(any(), eq("auth:keycloak:keycloak-subject-42"));
    }

    // ------------------------------------------------------------------
    // AC-2.1 / AC-2.7: no client-controlled identity material
    // ------------------------------------------------------------------

    @Test
    @DisplayName("forged identity headers cannot become the owner")
    void forgedIdentityHeadersHaveNoEffect() throws Exception {
        when(orchestrationService.chat(any(), any())).thenReturn(result());

        MvcResult result = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest()))
                        .header("X-Session-Owner", "forged-owner")
                        .header("X-Internal-Gateway-Key", "forged-gateway")
                        .header("X-User-Id", "forged-user")
                        .header("X-Forwarded-For", "203.0.113.9"))
                .andExpect(status().isOk())
                .andReturn();

        String owner = captureChatOwner();
        assertThat(owner).startsWith("anon:");
        assertThat(owner).doesNotContain("forged");
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).hasSize(1);
    }

    @Test
    @DisplayName("controller source contains neither forged-header literal nor legacy owner helpers")
    void controllerSourceContainsNoLegacyOwnerMechanism() throws Exception {
        String source = Files.readString(controllerSource());

        assertThat(source)
                .doesNotContain("X-Session-Owner")
                .doesNotContain("X-Internal-Gateway-Key")
                .doesNotContain("extractOwnerKey")
                .doesNotContain("hashString")
                .doesNotContain("MessageDigest")
                .doesNotContain("Authorization");
    }

    @Test
    @DisplayName("controller has no session store dependency and three POST handlers only")
    void controllerIsThinAndKeepsThreePostEndpoints() {
        assertThat(Arrays.stream(PublicRoomAdvisorController.class.getDeclaredFields())
                .map(Field::getType)
                .noneMatch(RoomAdvisorSessionStore.class::isAssignableFrom)).isTrue();

        List<String> postedPaths = Arrays.stream(PublicRoomAdvisorController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))
                .map(method -> method.getAnnotation(PostMapping.class).value()[0])
                .sorted()
                .toList();

        assertThat(postedPaths).containsExactly("/chat", "/refresh", "/search");
    }

    // ------------------------------------------------------------------
    // AC-2.3 / AC-2.4: local error DTO and indistinguishable 404s
    // ------------------------------------------------------------------

    @Test
    @DisplayName("absent, expired and foreign-owner sessions share an identical 404 body except requestId")
    void absentExpiredAndForeignSessionsAreIndistinguishable() throws Exception {
        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new SessionNotFoundException("Session not found or expired"))
                .thenThrow(new SessionNotFoundException("Session not found or expired"))
                .thenThrow(new SessionSecurityException("Access denied: session owner mismatch"));

        String expiredToken = tokenService.mint(ANON_OWNER, NOW.minusSeconds(1));

        JsonNode absent = performRefreshError(null);
        JsonNode expired = performRefreshError(expiredToken);
        JsonNode foreign = performRefreshError(tokenService.mint(
                ANON_OWNER, NOW.plus(Duration.ofMinutes(10))));

        assertFrozenError(absent, 404, "SESSION_NOT_FOUND");
        assertThat(withoutRequestId(absent)).isEqualTo(withoutRequestId(expired));
        assertThat(withoutRequestId(absent)).isEqualTo(withoutRequestId(foreign));
        assertThat(UUID.fromString(absent.path("error").path("requestId").asText())).isNotNull();
        assertThat(UUID.fromString(expired.path("error").path("requestId").asText())).isNotNull();
        assertThat(UUID.fromString(foreign.path("error").path("requestId").asText())).isNotNull();
    }

    @Test
    @DisplayName("404 response never exposes the exception message and never writes a cookie")
    void notFoundResponseExposesNoSessionMetadataAndNoCookie() throws Exception {
        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new SessionNotFoundException("secret session-id 11111111-1111-1111-1111-111111111111"));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.error.reason").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.retryable").value(false))
                .andReturn()
                .getResponse();

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        assertThat(response.getContentAsString()).doesNotContain("secret session-id");
    }

    @Test
    @DisplayName("version conflicts use the local 409 SESSION_VERSION_CONFLICT body")
    void versionConflictUsesLocalErrorDto() throws Exception {
        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new SessionVersionConflictException("expected 5 but was 6"));

        MockHttpServletResponse response = performRefreshConflictResponse();

        assertFrozenError(objectMapper.readTree(response.getContentAsString()),
                409, "SESSION_VERSION_CONFLICT");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        assertThat(response.getContentAsString()).doesNotContain("expected 5 but was 6");
    }

    @Test
    @DisplayName("invalid request uses the local 400 INVALID_REQUEST body")
    void invalidRequestUsesLocalErrorDto() throws Exception {
        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new IllegalArgumentException("raw internal validation detail"));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse();

        assertFrozenError(objectMapper.readTree(response.getContentAsString()),
                400, "INVALID_REQUEST");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        assertThat(response.getContentAsString()).doesNotContain("raw internal validation detail");
    }

    @Test
    @DisplayName("malformed JSON uses the same local 400 INVALID_REQUEST body")
    void malformedJsonUsesLocalErrorDto() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse();

        assertFrozenError(objectMapper.readTree(response.getContentAsString()),
                400, "INVALID_REQUEST");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    @DisplayName("scope denial is the only 403 and maps to SCOPE_DENIED")
    void scopeDenialIsTheOnlyForbiddenMapping() throws Exception {
        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new AccessDeniedException("scope"));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse();

        assertFrozenError(objectMapper.readTree(response.getContentAsString()),
                403, "SCOPE_DENIED");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    // ------------------------------------------------------------------
    // AC-2.5 / AC-2.10: frozen public surface and serialized error constraints
    // ------------------------------------------------------------------

    @Test
    @DisplayName("frozen OpenAPI still exposes exactly three POST advisor paths with x-business-write false")
    void frozenPublicSurfaceRemainsUnchanged() throws Exception {
        JsonNode paths = objectMapper.readTree(Files.readString(repositoryRoot().resolve(OPENAPI_RELATIVE_PATH)))
                .path("paths");

        int advisorPaths = 0;
        Iterator<Map.Entry<String, JsonNode>> iterator = paths.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (!entry.getKey().startsWith("/api/v1/public/ai/room-advisor/")) {
                continue;
            }
            advisorPaths++;
            JsonNode pathItem = entry.getValue();
            assertThat(pathItem.has("post")).isTrue();
            assertThat(pathItem.size()).isEqualTo(1);
            assertThat(pathItem.path("post").path("x-business-write").asBoolean()).isFalse();
        }
        assertThat(advisorPaths).isEqualTo(3);
    }

    // ------------------------------------------------------------------
    // AC-3.1 / AC-3.3 / AC-3.6: bounded rate limiting at the HTTP boundary
    // ------------------------------------------------------------------

    @Test
    @DisplayName("AC-3.1/AC-3.6: an exhausted owner budget answers 429 with the advisor envelope and Retry-After")
    void exhaustedOwnerBudgetAnswers429WithRetryAfter() throws Exception {
        when(orchestrationService.chat(any(), any())).thenReturn(result());
        String token = tokenService.mint(ANON_OWNER, NOW.plus(Duration.ofMinutes(20)));

        for (int request = 0; request < RoomAdvisorRateLimiter.ANONYMOUS_BURST; request++) {
            mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(chatRequest()))
                            .cookie(new Cookie(SESSION_COOKIE, token)))
                    .andExpect(status().isOk());
        }

        MvcResult result = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest()))
                        .cookie(new Cookie(SESSION_COOKIE, token)))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        String retryAfter = response.getHeader(RETRY_AFTER_HEADER);
        assertThat(retryAfter).isNotNull();
        assertThat(Integer.parseInt(retryAfter)).isPositive();

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertFrozenError(body, 429, "RATE_LIMITED");
        assertThat(body.path("error").path("retryable").asBoolean()).isTrue();
        verify(orchestrationService, times(RoomAdvisorRateLimiter.ANONYMOUS_BURST)).chat(any(), any());
    }

    @Test
    @DisplayName("AC-3.6: chat, search and refresh all answer 429 once the owner budget is exhausted")
    void rateLimitAppliesToAllThreePostEndpoints() throws Exception {
        when(orchestrationService.chat(any(), any())).thenReturn(result());
        String token = tokenService.mint(ANON_OWNER, NOW.plus(Duration.ofMinutes(20)));

        for (int request = 0; request < RoomAdvisorRateLimiter.ANONYMOUS_BURST; request++) {
            mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(chatRequest()))
                            .cookie(new Cookie(SESSION_COOKIE, token)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest()))
                        .cookie(new Cookie(SESSION_COOKIE, token)))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(post("/api/v1/public/ai/room-advisor/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest()))
                        .cookie(new Cookie(SESSION_COOKIE, token)))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest()))
                        .cookie(new Cookie(SESSION_COOKIE, token)))
                .andExpect(status().isTooManyRequests());

        verify(orchestrationService, never()).search(any(), any());
        verify(orchestrationService, never()).refresh(any(), any());
        verify(orchestrationService, times(RoomAdvisorRateLimiter.ANONYMOUS_BURST)).chat(any(), any());
    }

    @Test
    @DisplayName("AC-3.3: forged forwarding headers do not change the applied budget")
    void forgedForwardingHeadersDoNotBypassTheLimit() throws Exception {
        when(orchestrationService.chat(any(), any())).thenReturn(result());
        String token = tokenService.mint(ANON_OWNER, NOW.plus(Duration.ofMinutes(20)));

        for (int request = 0; request < RoomAdvisorRateLimiter.ANONYMOUS_BURST; request++) {
            mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(chatRequest()))
                            .header("X-Forwarded-For", "10.0.0.1")
                            .header("X-Real-IP", "10.0.0.2")
                            .header("X-Client-IP", "10.0.0.3")
                            .cookie(new Cookie(SESSION_COOKIE, token)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest()))
                        .header("X-Forwarded-For", "10.0.0.9")
                        .cookie(new Cookie(SESSION_COOKIE, token)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("AC-3.1/AC-3.6: a rate limited request never reaches the orchestration service")
    void rateLimitedRequestDoesNotReachTheOrchestrationService() throws Exception {
        when(orchestrationService.chat(any(), any())).thenReturn(result());
        String token = tokenService.mint(ANON_OWNER, NOW.plus(Duration.ofMinutes(20)));

        for (int request = 0; request < RoomAdvisorRateLimiter.ANONYMOUS_BURST; request++) {
            mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(chatRequest()))
                            .cookie(new Cookie(SESSION_COOKIE, token)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest()))
                        .cookie(new Cookie(SESSION_COOKIE, token)))
                .andExpect(status().isTooManyRequests());

        verify(orchestrationService, times(RoomAdvisorRateLimiter.ANONYMOUS_BURST)).chat(any(), any());
    }

    @Test
    @DisplayName("AC-4.5: chat failure on SessionStorageUnavailableException answers HTTP 503 UPSTREAM_UNAVAILABLE")
    void storageUnavailableOnChatAnswers503() throws Exception {
        when(orchestrationService.chat(any(), any()))
                .thenThrow(new SessionStorageUnavailableException("Redis connection pool exhausted"));

        MvcResult result = mockMvc.perform(post("/api/v1/public/ai/room-advisor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest())))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFrozenError(body, 503, "UPSTREAM_UNAVAILABLE");
        assertThat(body.path("error").path("retryable").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("AC-4.5: search failure on SessionStorageUnavailableException answers HTTP 503 UPSTREAM_UNAVAILABLE")
    void storageUnavailableOnSearchAnswers503() throws Exception {
        when(orchestrationService.search(any(), any()))
                .thenThrow(new SessionStorageUnavailableException("Redis down"));

        MvcResult result = mockMvc.perform(post("/api/v1/public/ai/room-advisor/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest())))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFrozenError(body, 503, "UPSTREAM_UNAVAILABLE");
        assertThat(body.path("error").path("retryable").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("AC-4.5: refresh failure on SessionStorageUnavailableException answers HTTP 503 UPSTREAM_UNAVAILABLE")
    void storageUnavailableOnRefreshAnswers503() throws Exception {
        when(orchestrationService.refresh(any(), any()))
                .thenThrow(new SessionStorageUnavailableException("Redis connection timed out"));

        MvcResult result = mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFrozenError(body, 503, "UPSTREAM_UNAVAILABLE");
        assertThat(body.path("error").path("retryable").asBoolean()).isTrue();
    }

    private MockHttpServletResponse performRefreshConflictResponse() throws Exception {
        return mockMvc.perform(post("/api/v1/public/ai/room-advisor/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest())))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse();
    }

    private JsonNode performRefreshError(String cookieToken) throws Exception {
        var requestBuilder = post("/api/v1/public/ai/room-advisor/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest()));
        if (cookieToken != null) {
            requestBuilder.cookie(new Cookie(SESSION_COOKIE, cookieToken));
        }

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andReturn();
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String captureChatOwner() {
        org.mockito.ArgumentCaptor<String> captor =
                org.mockito.ArgumentCaptor.forClass(String.class);
        verify(orchestrationService).chat(any(), captor.capture());
        return captor.getValue();
    }

    private void assertHardenedCookie(MockHttpServletResponse response, int expectedMaxAge) {
        Cookie cookie = response.getCookie(SESSION_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo(RoomAdvisorSessionCookieFactory.COOKIE_PATH);
        assertThat(cookie.getPath()).isNotEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(expectedMaxAge);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax")
                .contains("Path=" + RoomAdvisorSessionCookieFactory.COOKIE_PATH)
                .contains("Max-Age=" + expectedMaxAge);
        assertThat(tokenService.verify(cookie.getValue(), NOW)).isPresent();
    }

    private void assertFrozenError(JsonNode body, int code, String reason) throws Exception {
        JsonNode schema = objectMapper.readTree(Files.readString(repositoryRoot().resolve(SCHEMA_RELATIVE_PATH)))
                .path("$defs").path("ErrorResponse");

        assertThat(fieldNames(body))
                .containsExactlyInAnyOrderElementsOf(fieldNames(schema.path("properties")));
        assertThat(body.path("code").isInt()).isTrue();
        assertThat(body.path("code").asInt()).isBetween(400, 599).isEqualTo(code);
        assertThat(body.path("message").asText()).isNotBlank().hasSizeLessThanOrEqualTo(200);
        assertThat(body.path("error").path("contractVersion").asText()).isEqualTo("2.0");
        assertThat(UUID.fromString(body.path("error").path("requestId").asText())).isNotNull();
        assertThat(body.path("error").path("reason").asText()).isEqualTo(reason);
        assertThat(body.path("error").path("retryable").isBoolean()).isTrue();
        assertThat(body.has("result")).isFalse();
        assertThat(body.has("data")).isFalse();
    }

    private static JsonNode withoutRequestId(JsonNode body) {
        ObjectNode copy = body.deepCopy();
        ((ObjectNode) copy.path("error")).remove("requestId");
        return copy;
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new java.util.LinkedHashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private AdvisorChatRequestDto chatRequest() {
        return AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("TÃ¬m phÃ²ng táº¡i ÄÃ  Náºµng")
                .build();
    }

    private AdvisorSearchRequestDto searchRequest() {
        return AdvisorSearchRequestDto.builder()
                .contractVersion("2.0")
                .criteria(RoomSearchCriteriaDto.builder()
                        .hotelId(1)
                        .checkInDate(LocalDate.of(2026, 11, 20))
                        .checkOutDate(LocalDate.of(2026, 11, 22))
                        .adults(2)
                        .roomsCount(1)
                        .build())
                .build();
    }

    private AdvisorRefreshRequestDto refreshRequest() {
        return AdvisorRefreshRequestDto.builder()
                .contractVersion("2.0")
                .sessionId(UUID.randomUUID())
                .expectedSessionVersion(1L)
                .build();
    }

    private AdvisorResultDto result() {
        return AdvisorResultDto.builder()
                .contractVersion("2.0")
                .requestId(UUID.randomUUID())
                .sessionId(UUID.randomUUID())
                .sessionVersion(1L)
                .status(AdvisorStatus.RESULTS)
                .reply("TÃ¬m tháº¥y phÃ²ng phÃ¹ há»£p.")
                .build();
    }

    private static Path controllerSource() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            Path fromRepositoryRoot = directory.resolve(CONTROLLER_RELATIVE_PATH);
            if (Files.isRegularFile(fromRepositoryRoot)) {
                return fromRepositoryRoot;
            }
            Path fromBackendModule = directory.resolve(CONTROLLER_RELATIVE_PATH.replace("backend/", ""));
            if (Files.isRegularFile(fromBackendModule)) {
                return fromBackendModule;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("Unable to locate PublicRoomAdvisorController.java");
    }

    private static Path repositoryRoot() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            if (Files.isRegularFile(directory.resolve(SCHEMA_RELATIVE_PATH))
                    && Files.isRegularFile(directory.resolve(CONTROLLER_RELATIVE_PATH))) {
                return directory;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("Unable to locate repository root");
    }
}
