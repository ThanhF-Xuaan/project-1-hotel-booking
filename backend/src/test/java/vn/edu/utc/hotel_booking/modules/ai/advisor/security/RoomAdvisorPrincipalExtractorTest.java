package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.http.ResponseCookie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extractor-level ownership and zero-trust coverage for TASK-001.
 *
 * <p>All requests are plain servlet requests with a fixed {@link Clock}; no Spring context is
 * loaded. The synthetic signing secret is constructed explicitly.</p>
 */
class RoomAdvisorPrincipalExtractorTest {

    private static final String TEST_SECRET = "T12-test-only-7f3c1a9e5b2d8460c4f19a3e7b2d5c80";
    private static final String KEYCLOAK_SUBJECT = "9f2c7a4e-1b6d-4c85-9a03-5d8e7f102b34";

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final Duration TTL = RoomAdvisorSessionTokenService.DEFAULT_SESSION_TTL;
    private static final Duration REMAINING_ABOVE_HALF_LIFE = Duration.ofMinutes(20);
    private static final Duration REMAINING_BELOW_HALF_LIFE = Duration.ofMinutes(10);

    private RoomAdvisorSessionTokenService tokenService;
    private RoomAdvisorSessionCookieFactory cookieFactory;
    private RoomAdvisorPrincipalExtractor extractor;

    @BeforeEach
    void setUp() {
        tokenService = new RoomAdvisorSessionTokenService(TEST_SECRET, null);
        cookieFactory = new RoomAdvisorSessionCookieFactory();
        extractor = new RoomAdvisorPrincipalExtractor(tokenService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // AC-1.1 — validated JWT subject only
    // ------------------------------------------------------------------

    @Test
    @DisplayName("AC-1.1: a validated Jwt principal yields auth:keycloak:<sub> with no cookie write")
    void authenticatedJwtYieldsSubjectOwner() {
        authenticateWithJwt(KEYCLOAK_SUBJECT, "raw-access-token-1");
        MockHttpServletRequest request = requestWithSession(
                tokenService.mint(UUID.randomUUID(), NOW.plus(REMAINING_BELOW_HALF_LIFE)));

        ResolvedAdvisorOwner resolved = extractor.resolveOwner(request);

        assertThat(resolved.ownerKey()).isEqualTo("auth:keycloak:" + KEYCLOAK_SUBJECT);
        assertThat(resolved.tokenSource()).isEqualTo(ResolvedAdvisorOwner.TokenSource.AUTHENTICATED);
        assertThat(resolved.tokenToWrite()).isEmpty();
    }

    @Test
    @DisplayName("AC-1.1: the raw Authorization string is never hashed into the owner key")
    void rawAuthorizationStringIsNeverUsed() {
        authenticateWithJwt(KEYCLOAK_SUBJECT, "raw-access-token-one");
        ResolvedAdvisorOwner first = extractor.resolveOwner(new MockHttpServletRequest());

        authenticateWithJwt(KEYCLOAK_SUBJECT, "raw-access-token-two");
        MockHttpServletRequest requestWithBearer = new MockHttpServletRequest();
        requestWithBearer.addHeader("Authorization", "Bearer raw-access-token-two");

        ResolvedAdvisorOwner second = extractor.resolveOwner(requestWithBearer);

        assertThat(first.ownerKey()).isEqualTo(second.ownerKey());
        assertThat(second.ownerKey()).isEqualTo("auth:keycloak:" + KEYCLOAK_SUBJECT);
        assertThat(second.ownerKey()).doesNotContain("raw-access-token-two");
        assertThat(second.ownerKey()).doesNotContain("Bearer");
    }

    @Test
    @DisplayName("AC-1.1/AC-1.3: a bare Authorization header without a validated principal mints an anonymous owner")
    void unvalidatedAuthorizationHeaderIsIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer unvalidated-token");

        ResolvedAdvisorOwner resolved = extractor.resolveOwner(request);

        assertThat(resolved.tokenSource()).isEqualTo(ResolvedAdvisorOwner.TokenSource.ANON_MINTED);
        assertThat(resolved.ownerKey()).startsWith("anon:");
        assertThat(resolved.ownerKey()).doesNotContain("unvalidated-token");
    }

    // ------------------------------------------------------------------
    // AC-1.2 — valid cookie above half-life is existing
    // ------------------------------------------------------------------

    @Test
    @DisplayName("AC-1.2: a valid cookie above TTL/2 yields ANON_EXISTING and no token to write")
    void validCookieAboveHalfLifeIsExisting() {
        UUID owner = UUID.randomUUID();
        String token = tokenService.mint(owner, NOW.plus(REMAINING_ABOVE_HALF_LIFE));

        ResolvedAdvisorOwner resolved = extractor.resolveOwner(requestWithSession(token));

        assertThat(resolved.ownerKey()).isEqualTo("anon:" + owner);
        assertThat(resolved.tokenSource()).isEqualTo(ResolvedAdvisorOwner.TokenSource.ANON_EXISTING);
        assertThat(resolved.tokenToWrite()).isEmpty();
    }

    // ------------------------------------------------------------------
    // AC-1.3 — minting on missing/malformed/tampered/expired cookie
    // ------------------------------------------------------------------

    @Test
    @DisplayName("AC-1.3: a missing cookie mints a signed token bound to a fresh owner")
    void missingCookieMintsNewSession() {
        ResolvedAdvisorOwner resolved = extractor.resolveOwner(new MockHttpServletRequest());

        assertMintedSession(resolved);
    }

    @Test
    @DisplayName("AC-1.3: a malformed cookie mints a signed token bound to a fresh owner")
    void malformedCookieMintsNewSession() {
        ResolvedAdvisorOwner resolved = extractor.resolveOwner(requestWithSession("not-a-token"));

        assertMintedSession(resolved);
    }

    @Test
    @DisplayName("AC-1.3: a tampered cookie mints a signed token bound to a fresh owner")
    void tamperedCookieMintsNewSession() {
        String token = tokenService.mint(UUID.randomUUID(), NOW.plus(REMAINING_ABOVE_HALF_LIFE));
        String tampered = tamperSignature(token);

        ResolvedAdvisorOwner resolved = extractor.resolveOwner(requestWithSession(tampered));

        assertMintedSession(resolved);
    }

    @Test
    @DisplayName("AC-1.3/AC-1.7: an expired cookie mints a new session exactly like a tampered one")
    void expiredCookieMintsNewSession() {
        ResolvedAdvisorOwner resolved = extractor.resolveOwner(
                requestWithSession(tokenService.mint(UUID.randomUUID(), NOW.minusSeconds(1))));

        assertMintedSession(resolved);
    }

    @Test
    @DisplayName("AC-1.3: the cookie built for a minted token carries the exact security attributes")
    void mintedTokenBuildsSecureCookie() {
        ResolvedAdvisorOwner resolved = extractor.resolveOwner(new MockHttpServletRequest());
        String token = resolved.tokenToWrite().orElseThrow();

        ResponseCookie cookie = cookieFactory.build(token, TTL);

        assertThat(cookie.getName()).isEqualTo("t12_session");
        assertThat(cookie.getValue()).isEqualTo(token);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/public/ai/room-advisor");
        assertThat(cookie.getMaxAge()).isEqualTo(TTL);
        assertThat(cookie.getMaxAge().toSeconds()).isLessThanOrEqualTo(1800L);
    }

    // ------------------------------------------------------------------
    // AC-1.6a — zero trust on client identity headers
    // ------------------------------------------------------------------

    @Test
    @DisplayName("AC-1.6a: client identity headers do not change owner key or token presence")
    void identityHeadersDoNotInfluenceResolution() {
        UUID owner = UUID.randomUUID();
        String token = tokenService.mint(owner, NOW.plus(REMAINING_ABOVE_HALF_LIFE));

        ResolvedAdvisorOwner baseline = extractor.resolveOwner(requestWithSession(token));

        MockHttpServletRequest forged = requestWithSession(token);
        forged.addHeader("X-Session-Owner", "victim-owner");
        forged.addHeader("X-Internal-Gateway-Key", "trusted-gateway");
        forged.addHeader("X-User-Id", "victim-user");
        forged.addHeader("X-Authenticated-User", "victim-authenticated");
        forged.addHeader("X-Forwarded-For", "203.0.113.9");

        ResolvedAdvisorOwner withHeaders = extractor.resolveOwner(forged);

        assertThat(withHeaders.ownerKey()).isEqualTo(baseline.ownerKey());
        assertThat(withHeaders.tokenSource()).isEqualTo(baseline.tokenSource());
        assertThat(withHeaders.tokenToWrite().isPresent()).isEqualTo(baseline.tokenToWrite().isPresent());
    }

    @Test
    @DisplayName("AC-1.6a: forged identity headers cannot override a validated JWT owner")
    void identityHeadersDoNotOverrideAuthenticatedOwner() {
        authenticateWithJwt(KEYCLOAK_SUBJECT, "raw-access-token");
        MockHttpServletRequest forged = new MockHttpServletRequest();
        forged.addHeader("X-Session-Owner", "victim-owner");
        forged.addHeader("X-Internal-Gateway-Key", "trusted-gateway");

        ResolvedAdvisorOwner resolved = extractor.resolveOwner(forged);

        assertThat(resolved.ownerKey()).isEqualTo("auth:keycloak:" + KEYCLOAK_SUBJECT);
        assertThat(resolved.tokenSource()).isEqualTo(ResolvedAdvisorOwner.TokenSource.AUTHENTICATED);
    }

    @Test
    @DisplayName("AC-1.6a: extractor/token/cookie sources contain no client identity header literals")
    void securitySourcesContainNoIdentityHeaders() throws IOException {
        for (String fileName : new String[]{
                "RoomAdvisorPrincipalExtractor.java",
                "RoomAdvisorSessionTokenService.java",
                "RoomAdvisorSessionCookieFactory.java"}) {
            String source = readSecuritySource(fileName);
            assertThat(source).as(fileName).doesNotContain("X-Session-Owner");
            assertThat(source).as(fileName).doesNotContain("X-Internal-Gateway-Key");
        }

        String extractorSource = readSecuritySource("RoomAdvisorPrincipalExtractor.java");
        assertThat(extractorSource).doesNotContain("getHeader(");
        assertThat(extractorSource).doesNotContain("getRemoteAddr");
        assertThat(extractorSource).doesNotContain("getUserAgent");
    }

    // ------------------------------------------------------------------
    // AC-1.7 — sliding-TTL renewal
    // ------------------------------------------------------------------

    @Test
    @DisplayName("AC-1.7: remaining lifetime at exactly TTL/2 renews the same owner")
    void renewalAtExactlyHalfLife() {
        UUID owner = UUID.randomUUID();
        String token = tokenService.mint(owner, NOW.plus(TTL.dividedBy(2)));

        ResolvedAdvisorOwner resolved = extractor.resolveOwner(requestWithSession(token));

        assertRenewed(resolved, owner);
    }

    @Test
    @DisplayName("AC-1.7: remaining lifetime below TTL/2 renews the same owner")
    void renewalBelowHalfLife() {
        UUID owner = UUID.randomUUID();
        String token = tokenService.mint(owner, NOW.plus(REMAINING_BELOW_HALF_LIFE));

        ResolvedAdvisorOwner resolved = extractor.resolveOwner(requestWithSession(token));

        assertRenewed(resolved, owner);
    }

    @Test
    @DisplayName("AC-1.7: remaining lifetime just above TTL/2 does not renew")
    void noRenewalJustAboveHalfLife() {
        UUID owner = UUID.randomUUID();
        String token = tokenService.mint(owner, NOW.plus(TTL.dividedBy(2)).plusSeconds(1));

        ResolvedAdvisorOwner resolved = extractor.resolveOwner(requestWithSession(token));

        assertThat(resolved.ownerKey()).isEqualTo("anon:" + owner);
        assertThat(resolved.tokenSource()).isEqualTo(ResolvedAdvisorOwner.TokenSource.ANON_EXISTING);
        assertThat(resolved.tokenToWrite()).isEmpty();
    }

    @Test
    @DisplayName("AC-1.7: renewal re-mints the same UUID with a refreshed, verifiable exp")
    void renewedTokenKeepsUuidAndRefreshesExpiry() {
        UUID owner = UUID.randomUUID();
        Instant originalExp = NOW.plus(REMAINING_BELOW_HALF_LIFE);
        String originalToken = tokenService.mint(owner, originalExp);

        ResolvedAdvisorOwner resolved = extractor.resolveOwner(requestWithSession(originalToken));
        String renewedToken = resolved.tokenToWrite().orElseThrow();

        RoomAdvisorSessionTokenService.VerifiedToken verified =
                tokenService.verify(renewedToken, NOW).orElseThrow();

        assertThat(verified.owner()).isEqualTo(owner);
        assertThat(verified.exp()).isAfter(originalExp);
        assertThat(verified.exp()).isEqualTo(NOW.plus(TTL));
        assertThat(resolved.ownerKey()).isEqualTo("anon:" + owner);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void authenticateWithJwt(String subject, String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .subject(subject)
                .issuedAt(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private static MockHttpServletRequest requestWithSession(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("t12_session", token));
        return request;
    }

    private void assertMintedSession(ResolvedAdvisorOwner resolved) {
        assertThat(resolved.tokenSource()).isEqualTo(ResolvedAdvisorOwner.TokenSource.ANON_MINTED);
        assertThat(resolved.ownerKey()).startsWith("anon:");

        UUID owner = UUID.fromString(resolved.ownerKey().substring("anon:".length()));
        String token = resolved.tokenToWrite().orElseThrow();

        Optional<RoomAdvisorSessionTokenService.VerifiedToken> verified = tokenService.verify(token, NOW);
        assertThat(verified).isPresent();
        assertThat(verified.get().owner()).isEqualTo(owner);
        assertThat(verified.get().exp()).isEqualTo(NOW.plus(TTL));
    }

    private void assertRenewed(ResolvedAdvisorOwner resolved, UUID expectedOwner) {
        assertThat(resolved.ownerKey()).isEqualTo("anon:" + expectedOwner);
        assertThat(resolved.tokenSource()).isEqualTo(ResolvedAdvisorOwner.TokenSource.ANON_RENEWED);

        String renewedToken = resolved.tokenToWrite().orElseThrow();
        RoomAdvisorSessionTokenService.VerifiedToken verified =
                tokenService.verify(renewedToken, NOW).orElseThrow();

        assertThat(verified.owner()).isEqualTo(expectedOwner);
        assertThat(verified.exp()).isEqualTo(NOW.plus(TTL));
    }

    /**
     * Builds a cookie value whose HMAC signature differs from the original at the decoded-byte
     * level.
     *
     * <p>The final Base64URL character of a 32-byte signature only carries four significant bits;
     * the trailing padding bits are ignored by the decoder, so replacing that character can leave
     * the decoded signature byte-identical and the "tampered" token still valid. Flipping one bit
     * inside a decoded signature byte and re-encoding it always produces a genuinely different
     * signature, independent of the original signature contents.</p>
     */
    private static String tamperSignature(String token) {
        String[] segments = token.split("\\.", -1);
        byte[] originalSignature = Base64.getUrlDecoder().decode(segments[2]);

        byte[] tamperedSignature = originalSignature.clone();
        tamperedSignature[0] ^= 0x01;
        String encodedTampered = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedSignature);

        assertThat(encodedTampered).isNotEqualTo(segments[2]);
        assertThat(Base64.getUrlDecoder().decode(encodedTampered)).isNotEqualTo(originalSignature);

        return segments[0] + "." + segments[1] + "." + encodedTampered;
    }

    private static String readSecuritySource(String fileName) throws IOException {
        Path sourcePath = Path.of("src", "main", "java", "vn", "edu", "utc", "hotel_booking",
                "modules", "ai", "advisor", "security", fileName);
        assertThat(sourcePath).as("expected advisor security source %s", fileName).exists();
        return Files.readString(sourcePath);
    }
}
