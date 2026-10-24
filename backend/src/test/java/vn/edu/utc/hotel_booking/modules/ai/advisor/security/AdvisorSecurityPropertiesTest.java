package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies externalized secret provisioning and fail-fast behaviour for the advisor security
 * properties. The test secret is synthetic and exists only inside this test class; no production
 * default or test-profile bypass is introduced.
 */
@SpringJUnitConfig(RoomAdvisorSecurityConfig.class)
@TestPropertySource(properties =
        "hotel.ai.advisor.security.anon-token-secret=" + AdvisorSecurityPropertiesTest.TEST_SECRET)
class AdvisorSecurityPropertiesTest {

    static final String TEST_SECRET = "T12-test-only-7f3c1a9e5b2d8460c4f19a3e7b2d5c80";
    static final String TEST_SECONDARY_SECRET = "T12-test-secondary-2b8e4d6a1c3f5e7b9d0a2c4e6f8b1d35";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RoomAdvisorSecurityConfig.class);

    @Autowired
    private AdvisorSecurityProperties springProperties;

    @Test
    @DisplayName("AC-1.8: a strong synthetic secret is loaded through @TestPropertySource")
    void strongSecretIsLoadedThroughTestPropertySource() {
        assertThat(springProperties.getAnonTokenSecret()).isEqualTo(TEST_SECRET);
        assertThat(springProperties.getAnonTokenSecondarySecret()).isNull();
    }

    @Test
    @DisplayName("AC-1.5: a strong runtime-provided secret starts the context and exposes the security beans")
    void strongSecretStartsContext() {
        runner.withPropertyValues("hotel.ai.advisor.security.anon-token-secret=" + TEST_SECRET)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AdvisorSecurityProperties.class);
                    assertThat(context).hasSingleBean(RoomAdvisorSessionTokenService.class);
                    assertThat(context).hasSingleBean(RoomAdvisorSessionCookieFactory.class);
                    assertThat(context).hasSingleBean(RoomAdvisorPrincipalExtractor.class);
                    assertThat(context.getBean(AdvisorSecurityProperties.class).getAnonTokenSecret())
                            .isEqualTo(TEST_SECRET);
                });
    }

    @Test
    @DisplayName("AC-1.4/AC-1.5: a strong secondary rotation secret starts the context and is bound")
    void strongSecondarySecretStartsContext() {
        runner.withPropertyValues(
                        "hotel.ai.advisor.security.anon-token-secret=" + TEST_SECRET,
                        "hotel.ai.advisor.security.anon-token-secondary-secret=" + TEST_SECONDARY_SECRET)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AdvisorSecurityProperties.class).getAnonTokenSecondarySecret())
                            .isEqualTo(TEST_SECONDARY_SECRET);
                });
    }

    @Test
    @DisplayName("AC-1.5: a missing secret aborts startup with IllegalStateException")
    void missingSecretFailsFast() {
        assertFailsFast(null, "anon-token-secret");
    }

    @Test
    @DisplayName("AC-1.5: a secret shorter than 32 bytes aborts startup with IllegalStateException")
    void tooShortSecretFailsFast() {
        assertFailsFast(TEST_SECRET.substring(0, AdvisorSecurityProperties.MIN_SECRET_BYTES - 1),
                "anon-token-secret");
    }

    @Test
    @DisplayName("AC-1.5: a denylisted placeholder aborts startup with IllegalStateException")
    void denylistedPlaceholderFailsFast() {
        assertFailsFast("ChangeMe-0123456789-abcdefghij-ABCDEFGH", "anon-token-secret");
    }

    @Test
    @DisplayName("AC-1.5: a low-diversity secret aborts startup with IllegalStateException")
    void lowDistinctCharacterSecretFailsFast() {
        assertFailsFast("abababababababababababababababababab", "anon-token-secret");
    }

    @Test
    @DisplayName("AC-1.4/AC-1.5: a weak secondary rotation secret also aborts startup")
    void weakSecondarySecretFailsFast() {
        runner.withPropertyValues(
                        "hotel.ai.advisor.security.anon-token-secret=" + TEST_SECRET,
                        "hotel.ai.advisor.security.anon-token-secondary-secret=changeme")
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable rootCause = rootCauseOf(context.getStartupFailure());
                    assertThat(rootCause).isInstanceOf(IllegalStateException.class);
                    assertThat(rootCause.getMessage()).contains("anon-token-secondary-secret");
                    assertThat(rootCause.getMessage()).doesNotContain(TEST_SECRET);
                });
    }

    private void assertFailsFast(String secret, String expectedPropertyName) {
        ApplicationContextRunner failingRunner = runner;
        if (secret != null) {
            failingRunner = failingRunner.withPropertyValues(
                    "hotel.ai.advisor.security." + expectedPropertyName + "=" + secret);
        }

        failingRunner.run(context -> {
            assertThat(context).hasFailed();
            Throwable rootCause = rootCauseOf(context.getStartupFailure());
            assertThat(rootCause).isInstanceOf(IllegalStateException.class);
            assertThat(rootCause.getMessage()).contains(expectedPropertyName);
            if (secret != null) {
                assertThat(rootCause.getMessage()).doesNotContain(secret);
            }
        });
    }

    private static Throwable rootCauseOf(Throwable failure) {
        assertThat(failure).isNotNull();
        Throwable cursor = failure;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor;
    }
}
