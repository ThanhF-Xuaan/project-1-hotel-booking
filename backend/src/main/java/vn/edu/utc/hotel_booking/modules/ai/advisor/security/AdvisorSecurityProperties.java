package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Externally provisioned secrets for the Room Advisor session security primitives.
 *
 * <p>Bound from {@code hotel.ai.advisor.security.*} (environment variables
 * {@code HOTEL_AI_ADVISOR_SECURITY_ANON_TOKEN_SECRET} and
 * {@code HOTEL_AI_ADVISOR_SECURITY_ANON_TOKEN_SECONDARY_SECRET} through Spring relaxed binding).
 * There is deliberately no default value and no runtime fallback secret: a missing or weak secret
 * must abort application start-up in every environment.</p>
 *
 * <p>The configured value is a provisioning contract: it must be generated with a CSPRNG using at
 * least 32 random bytes (for example {@code openssl rand -base64 48}). Length is not entropy, so the
 * checks below are conservative guards only and never substitute for CSPRNG provisioning.</p>
 */
@ConfigurationProperties(prefix = AdvisorSecurityProperties.PREFIX)
public class AdvisorSecurityProperties {

    public static final String PREFIX = "hotel.ai.advisor.security";

    public static final String PRIMARY_SECRET_PROPERTY = "anon-token-secret";
    public static final String SECONDARY_SECRET_PROPERTY = "anon-token-secondary-secret";

    /** Minimum UTF-8 length of a provisioned secret, in bytes. */
    public static final int MIN_SECRET_BYTES = 32;

    /** Conservative low-entropy guard: a usable secret must use at least this many distinct characters. */
    public static final int MIN_DISTINCT_CHARACTERS = 16;

    private static final List<String> DENYLISTED_EXACT_VALUES =
            List.of("changeme", "secret", "password", "test", "dev", "t12");

    private static final List<String> DENYLISTED_FRAGMENTS =
            List.of("changeme", "password", "placeholder");

    private String anonTokenSecret;

    private String anonTokenSecondarySecret;

    /**
     * Validates both secrets during context initialization. Any violation fails fast with
     * {@link IllegalStateException}; the secret value is never included in the message.
     */
    @PostConstruct
    public void validate() {
        this.anonTokenSecret = requireStrongSecret(PRIMARY_SECRET_PROPERTY, this.anonTokenSecret);
        if (this.anonTokenSecondarySecret != null && !this.anonTokenSecondarySecret.isBlank()) {
            this.anonTokenSecondarySecret =
                    requireStrongSecret(SECONDARY_SECRET_PROPERTY, this.anonTokenSecondarySecret);
        } else {
            this.anonTokenSecondarySecret = null;
        }
    }

    private static String requireStrongSecret(String propertyName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required advisor security secret '" + PREFIX + "." + propertyName + "'");
        }

        String candidate = value.trim();
        if (candidate.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "Advisor security secret '" + PREFIX + "." + propertyName + "' must be at least "
                            + MIN_SECRET_BYTES + " bytes of CSPRNG output");
        }

        String normalized = candidate.toLowerCase(Locale.ROOT);
        boolean denylisted = DENYLISTED_EXACT_VALUES.contains(normalized)
                || DENYLISTED_FRAGMENTS.stream().anyMatch(normalized::contains);
        if (denylisted) {
            throw new IllegalStateException(
                    "Advisor security secret '" + PREFIX + "." + propertyName
                            + "' is a denylisted placeholder value; provision a CSPRNG-generated secret");
        }

        long distinctCharacters = normalized.chars().distinct().count();
        if (distinctCharacters < MIN_DISTINCT_CHARACTERS) {
            throw new IllegalStateException(
                    "Advisor security secret '" + PREFIX + "." + propertyName
                            + "' has insufficient character diversity; provision a CSPRNG-generated secret");
        }

        return candidate;
    }

    public String getAnonTokenSecret() {
        return anonTokenSecret;
    }

    public void setAnonTokenSecret(String anonTokenSecret) {
        this.anonTokenSecret = anonTokenSecret;
    }

    public String getAnonTokenSecondarySecret() {
        return anonTokenSecondarySecret;
    }

    public void setAnonTokenSecondarySecret(String anonTokenSecondarySecret) {
        this.anonTokenSecondarySecret = anonTokenSecondarySecret;
    }
}
