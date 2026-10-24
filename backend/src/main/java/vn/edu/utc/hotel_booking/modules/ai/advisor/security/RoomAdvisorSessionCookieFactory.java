package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Single source of truth for the advisor session cookie attributes.
 *
 * <p>The cookie name stays {@code t12_session} for wire compatibility with the frozen R1-00
 * contract; only the value becomes a server-signed token. The narrowly scoped path prevents the
 * advisor cookie from interfering with the booking web session.</p>
 */
public class RoomAdvisorSessionCookieFactory {

    public static final String COOKIE_NAME = "t12_session";
    public static final String COOKIE_PATH = "/api/v1/public/ai/room-advisor";
    public static final String SAME_SITE = "Lax";
    public static final Duration MAX_COOKIE_TTL = Duration.ofMinutes(30);

    /**
     * Builds the session cookie for both initial mint and renewal.
     *
     * @param rawToken signed anonymous token
     * @param ttl      remaining session lifetime, mirrored into {@code Max-Age}
     */
    public ResponseCookie build(String rawToken, Duration ttl) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Advisor session token must not be blank");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Advisor session cookie Max-Age must be positive");
        }
        if (ttl.compareTo(MAX_COOKIE_TTL) > 0) {
            throw new IllegalArgumentException(
                    "Advisor session cookie Max-Age must not exceed " + MAX_COOKIE_TTL.toSeconds() + " seconds");
        }

        return ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(ttl)
                .build();
    }
}
