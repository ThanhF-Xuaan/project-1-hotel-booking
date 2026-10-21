package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Member 1 wiring for the advisor session security primitives.
 *
 * <p>Deliberately local to the advisor module: the shared {@code SecurityConfig} remains untouched.
 * The signing secret is enabled through {@link AdvisorSecurityProperties}, which fails fast when the
 * externally provisioned secret is missing or weak.</p>
 */
@Configuration
@EnableConfigurationProperties(AdvisorSecurityProperties.class)
public class RoomAdvisorSecurityConfig {

    @Bean
    public RoomAdvisorSessionTokenService roomAdvisorSessionTokenService(AdvisorSecurityProperties properties) {
        return new RoomAdvisorSessionTokenService(
                properties.getAnonTokenSecret(),
                properties.getAnonTokenSecondarySecret());
    }

    @Bean
    public RoomAdvisorSessionCookieFactory roomAdvisorSessionCookieFactory() {
        return new RoomAdvisorSessionCookieFactory();
    }

    @Bean
    public RoomAdvisorPrincipalExtractor roomAdvisorPrincipalExtractor(
            RoomAdvisorSessionTokenService roomAdvisorSessionTokenService) {
        return new RoomAdvisorPrincipalExtractor(roomAdvisorSessionTokenService, Clock.systemUTC());
    }
}
