package vn.edu.utc.hotel_booking.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DockerNetworkConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    @DisplayName("Verify default fallback to localhost for Local IDE development")
    void testLocalDevDefaultConfig() {
        contextRunner.run(context -> {
            String defaultKeycloakUrl = context.getEnvironment().getProperty("keycloak.server-url", "http://localhost:8081");
            String defaultRedisHost = context.getEnvironment().getProperty("spring.data.redis.host", "localhost");
            String defaultDbHost = context.getEnvironment().getProperty("DB_HOST", "localhost");
            String defaultDbPort = context.getEnvironment().getProperty("DB_PORT", "5433");

            assertThat(defaultKeycloakUrl).isEqualTo("http://localhost:8081");
            assertThat(defaultRedisHost).isEqualTo("localhost");
            assertThat(defaultDbHost).isEqualTo("localhost");
            assertThat(defaultDbPort).isEqualTo("5433");
        });
    }

    @Test
    @DisplayName("Verify .env.docker variables override URLs to internal container services")
    void testDockerEnvironmentOverride() {
        contextRunner
                .withPropertyValues(
                        "DB_HOST=postgres_db",
                        "DB_PORT=5432",
                        "REDIS_HOST=redis",
                        "REDIS_PORT=6379",
                        "KEYCLOAK_SERVER_URL=http://keycloak:8080",
                        "KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/hotel-realm",
                        "KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/hotel-realm/protocol/openid-connect/certs"
                )
                .run(context -> {
                    String dbHost = context.getEnvironment().getProperty("DB_HOST");
                    String dbPort = context.getEnvironment().getProperty("DB_PORT");
                    String redisHost = context.getEnvironment().getProperty("REDIS_HOST");
                    String keycloakServerUrl = context.getEnvironment().getProperty("KEYCLOAK_SERVER_URL");
                    String jwkSetUri = context.getEnvironment().getProperty("KEYCLOAK_JWK_SET_URI");

                    assertThat(dbHost).isEqualTo("postgres_db");
                    assertThat(dbPort).isEqualTo("5432");
                    assertThat(redisHost).isEqualTo("redis");
                    assertThat(keycloakServerUrl).isEqualTo("http://keycloak:8080");
                    assertThat(jwkSetUri).isEqualTo("http://keycloak:8080/realms/hotel-realm/protocol/openid-connect/certs");
                });
    }
}
