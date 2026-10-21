package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Field-by-field assertions for the single source of advisor cookie attributes.
 */
class RoomAdvisorSessionCookieFactoryTest {

    private static final String RAW_TOKEN = "5b1f0f5a-3f9e-4e2b-9d47-2c1a6b8e0f31.1792484800.c2lnbmF0dXJl";
    private static final Duration TTL = RoomAdvisorSessionTokenService.DEFAULT_SESSION_TTL;

    private final RoomAdvisorSessionCookieFactory cookieFactory = new RoomAdvisorSessionCookieFactory();

    @Test
    @DisplayName("AC-1.3: cookie name, value and security attributes are exact")
    void buildsWireCompatibleCookie() {
        ResponseCookie cookie = cookieFactory.build(RAW_TOKEN, TTL);

        assertThat(cookie.getName()).isEqualTo("t12_session");
        assertThat(cookie.getValue()).isEqualTo(RAW_TOKEN);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/public/ai/room-advisor");
        assertThat(cookie.getMaxAge()).isEqualTo(TTL);
        assertThat(cookie.getMaxAge().toSeconds()).isLessThanOrEqualTo(1800L);
        assertThat(cookie.getDomain()).isNull();
    }

    @Test
    @DisplayName("AC-1.3: serialized Set-Cookie exposes HttpOnly, Secure, SameSite=Lax and the narrow path")
    void serializedCookieCarriesRequiredAttributes() {
        String header = cookieFactory.build(RAW_TOKEN, TTL).toString();

        assertThat(header).contains("t12_session=" + RAW_TOKEN);
        assertThat(header).contains("HttpOnly");
        assertThat(header).contains("Secure");
        assertThat(header).contains("SameSite=Lax");
        assertThat(header).contains("Path=/api/v1/public/ai/room-advisor");
        assertThat(header).contains("Max-Age=1800");
        assertThat(header).doesNotContain("Path=/;");
    }

    @Test
    @DisplayName("AC-1.3: cookie path must stay narrow and never be the root path")
    void pathIsNeverRoot() {
        ResponseCookie cookie = cookieFactory.build(RAW_TOKEN, Duration.ofMinutes(5));

        assertThat(cookie.getPath()).isNotEqualTo("/");
        assertThat(cookie.getPath()).startsWith("/api/v1/public/ai/");
    }

    @Test
    @DisplayName("AC-1.3: Max-Age mirrors a shorter renewal lifetime")
    void maxAgeMirrorsRenewalLifetime() {
        ResponseCookie cookie = cookieFactory.build(RAW_TOKEN, Duration.ofSeconds(900));

        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(900));
        assertThat(cookie.getMaxAge().toSeconds()).isLessThan(1800L);
    }

    @Test
    @DisplayName("AC-1.3: Max-Age above 1800 seconds is rejected")
    void maxAgeAboveLimitIsRejected() {
        assertThatThrownBy(() -> cookieFactory.build(RAW_TOKEN, Duration.ofSeconds(1801)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("AC-1.3: blank tokens and non-positive lifetimes are rejected")
    void invalidInputsAreRejected() {
        assertThatThrownBy(() -> cookieFactory.build(null, TTL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cookieFactory.build("  ", TTL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cookieFactory.build(RAW_TOKEN, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cookieFactory.build(RAW_TOKEN, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cookieFactory.build(RAW_TOKEN, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
