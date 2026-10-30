package vn.edu.utc.hotel_booking.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtBlacklistFilter jwtBlacklistFilter;

    @Test
    @DisplayName("corsConfigurationSource with default origins exposes Retry-After and credentials")
    void corsConfigurationSource_defaultOrigins_exposesRetryAfterAndCredentials() {
        SecurityConfig config = new SecurityConfig(jwtBlacklistFilter);

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration corsConfig = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowCredentials()).isTrue();
        assertThat(corsConfig.getAllowedOrigins()).containsExactlyInAnyOrder(
                "http://localhost",
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "https://localhost"
        );
        assertThat(corsConfig.getAllowedMethods()).containsExactlyInAnyOrder(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        );
        assertThat(corsConfig.getAllowedHeaders()).containsExactlyInAnyOrder(
                "Authorization", "Content-Type", "Accept", "X-Requested-With"
        );
        assertThat(corsConfig.getExposedHeaders()).contains(
                "Retry-After", "Authorization", "Link", "X-Total-Count"
        );
    }

    @Test
    @DisplayName("corsConfigurationSource incorporates deployment origins from properties/env")
    void corsConfigurationSource_customOrigins_incorporatesDeploymentOrigins() {
        SecurityConfig config = new SecurityConfig(jwtBlacklistFilter);
        ReflectionTestUtils.setField(config, "customAllowedOrigins",
                "https://hotel.example.com, https://admin.hotel.example.com");

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration corsConfig = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOrigins()).contains(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "https://hotel.example.com",
                "https://admin.hotel.example.com"
        );
        assertThat(corsConfig.getExposedHeaders()).contains("Retry-After");
    }

    @Test
    @DisplayName("corsConfigurationSource ignores blank or duplicate origins")
    void corsConfigurationSource_ignoresBlankOrDuplicateOrigins() {
        SecurityConfig config = new SecurityConfig(jwtBlacklistFilter);
        ReflectionTestUtils.setField(config, "customAllowedOrigins",
                "  , , http://localhost:3000, https://app.example.com ");

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration corsConfig = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOrigins()).contains(
                "http://localhost:3000",
                "https://app.example.com"
        );
        long localhost3000Count = corsConfig.getAllowedOrigins().stream()
                .filter("http://localhost:3000"::equals)
                .count();
        assertThat(localhost3000Count).isEqualTo(1);
    }

    @Test
    @DisplayName("DefaultCorsProcessor populates Access-Control-Expose-Headers including Retry-After for allowed origin")
    void defaultCorsProcessor_populatesAccessControlExposeHeaders() throws Exception {
        SecurityConfig config = new SecurityConfig(jwtBlacklistFilter);
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/ai/room-advisor/chat");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        DefaultCorsProcessor processor = new DefaultCorsProcessor();
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        boolean allowed = processor.processRequest(corsConfig, request, response);

        assertThat(allowed).isTrue();
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:5173");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).contains("Retry-After");
    }
}
