package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the advisor owner without trusting any client-supplied identity material.
 *
 * <p>Two and only two sources are accepted:</p>
 * <ol>
 *     <li>a validated {@link Jwt} principal placed in the Spring Security context by the resource
 *     server; the owner key is derived from its stable {@code sub} claim;</li>
 *     <li>a server-signed anonymous cookie whose HMAC and absolute expiry both verify.</li>
 * </ol>
 *
 * <p>Identity headers are never read. Client IP, user agent, raw {@code Authorization} values and
 * every custom header have no influence on the resolved owner. Anonymous renewal re-mints the same
 * UUID when the remaining lifetime is at or below half of the sliding session TTL, so the owner key
 * and the server-side session stay stable.</p>
 *
 * <p>This extractor deliberately does not consult the session store. Cookie issuance is performed by
 * the controller boundary only after a successful orchestration call, so a request whose server-side
 * session is absent, expired or foreign receives a 404 and no {@code Set-Cookie}.</p>
 */
public class RoomAdvisorPrincipalExtractor {

    private static final String AUTHENTICATED_OWNER_PREFIX = "auth:keycloak:";
    private static final String ANONYMOUS_OWNER_PREFIX = "anon:";
    private static final String SESSION_COOKIE_NAME = RoomAdvisorSessionCookieFactory.COOKIE_NAME;

    private final RoomAdvisorSessionTokenService tokenService;
    private final Clock clock;
    private final Duration sessionTtl;

    public RoomAdvisorPrincipalExtractor(RoomAdvisorSessionTokenService tokenService, Clock clock) {
        this(tokenService, clock, RoomAdvisorSessionTokenService.DEFAULT_SESSION_TTL);
    }

    public RoomAdvisorPrincipalExtractor(RoomAdvisorSessionTokenService tokenService, Clock clock, Duration sessionTtl) {
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.sessionTtl = Objects.requireNonNull(sessionTtl, "sessionTtl must not be null");
        if (sessionTtl.isZero() || sessionTtl.isNegative()) {
            throw new IllegalArgumentException("sessionTtl must be positive");
        }
    }

    /**
     * Resolves the owning identity for the current request.
     *
     * @param request current servlet request; only its {@code t12_session} cookie is inspected
     */
    public ResolvedAdvisorOwner resolveOwner(HttpServletRequest request) {
        Instant now = clock.instant();

        Optional<ResolvedAdvisorOwner> authenticated = resolveAuthenticatedOwner();
        if (authenticated.isPresent()) {
            return authenticated.get();
        }

        String presentedToken = readSessionCookie(request);
        if (presentedToken != null) {
            Optional<RoomAdvisorSessionTokenService.VerifiedToken> verified = tokenService.verify(presentedToken, now);
            if (verified.isPresent()) {
                RoomAdvisorSessionTokenService.VerifiedToken token = verified.get();
                String ownerKey = ANONYMOUS_OWNER_PREFIX + token.owner();
                Duration remaining = Duration.between(now, token.exp());

                if (remaining.compareTo(sessionTtl.dividedBy(2)) > 0) {
                    return new ResolvedAdvisorOwner(
                            ownerKey, Optional.empty(), ResolvedAdvisorOwner.TokenSource.ANON_EXISTING);
                }

                String renewedToken = tokenService.mint(token.owner(), expiryFrom(now));
                return new ResolvedAdvisorOwner(
                        ownerKey, Optional.of(renewedToken), ResolvedAdvisorOwner.TokenSource.ANON_RENEWED);
            }
        }

        UUID owner = UUID.randomUUID();
        String mintedToken = tokenService.mint(owner, expiryFrom(now));
        return new ResolvedAdvisorOwner(
                ANONYMOUS_OWNER_PREFIX + owner,
                Optional.of(mintedToken),
                ResolvedAdvisorOwner.TokenSource.ANON_MINTED);
    }

    private Optional<ResolvedAdvisorOwner> resolveAuthenticatedOwner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new ResolvedAdvisorOwner(
                AUTHENTICATED_OWNER_PREFIX + subject,
                Optional.empty(),
                ResolvedAdvisorOwner.TokenSource.AUTHENTICATED));
    }

    private static String readSessionCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private Instant expiryFrom(Instant now) {
        return now.plus(sessionTtl).truncatedTo(ChronoUnit.SECONDS);
    }
}
