package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Mints and verifies the server-signed anonymous advisor session token.
 *
 * <p>Wire format: {@code {uuid}.{expEpochSeconds}.{base64url(HMAC-SHA256(uuid + "." + exp, key))}}.
 * The UUID is the stable ownership identity and never changes on renewal; the {@code exp} claim is
 * the absolute UTC expiry of the token itself. Signature comparison is constant-time
 * ({@link MessageDigest#isEqual}). Tokens signed by the primary or the optional secondary key are
 * accepted; newly minted tokens always use the primary key. Expired, tampered, malformed and
 * unknown-key tokens are all rejected identically without leaking token material.</p>
 */
public class RoomAdvisorSessionTokenService {

    /** Session lifetime mirrored from the authoritative server-side session store (1800 s). */
    public static final Duration DEFAULT_SESSION_TTL = Duration.ofMinutes(30);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DELIMITER = ".";
    private static final int TOKEN_SEGMENTS = 3;

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final byte[] primaryKey;
    private final byte[] secondaryKey;

    /**
     * @param primarySecret   required signing key
     * @param secondarySecret optional previous key accepted only during rotation; {@code null}/blank disables it
     */
    public RoomAdvisorSessionTokenService(String primarySecret, String secondarySecret) {
        this.primaryKey = requireKey("primary", primarySecret);
        this.secondaryKey = (secondarySecret == null || secondarySecret.isBlank())
                ? null
                : requireKey("secondary", secondarySecret);
    }

    /** Result of a successful token verification. */
    public record VerifiedToken(UUID owner, Instant exp) {
    }

    /** Mints a token for a fresh UUID using the default session lifetime. */
    public String mint() {
        return mint(Instant.now().plus(DEFAULT_SESSION_TTL));
    }

    /** Mints a token for a fresh UUID with an explicit absolute expiry. */
    public String mint(Instant exp) {
        return mint(UUID.randomUUID(), exp);
    }

    /**
     * Mints a token for an existing owner UUID and explicit absolute expiry.
     * Renewal must pass the same UUID so the derived owner key stays stable.
     */
    public String mint(UUID owner, Instant exp) {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(exp, "exp must not be null");

        String payload = owner + DELIMITER + exp.getEpochSecond();
        String signature = BASE64_URL_ENCODER.encodeToString(hmac(primaryKey, payload));
        return payload + DELIMITER + signature;
    }

    /**
     * Verifies signature and absolute expiry. Returns empty for a bad HMAC, an unknown key, a
     * malformed token or an expired token ({@code exp <= now}).
     */
    public Optional<VerifiedToken> verify(String token, Instant now) {
        if (token == null || token.isBlank() || now == null) {
            return Optional.empty();
        }

        String[] segments = token.split("\\.", -1);
        if (segments.length != TOKEN_SEGMENTS || segments[0].isBlank() || segments[1].isBlank()
                || segments[2].isBlank()) {
            return Optional.empty();
        }

        UUID owner;
        long expEpochSeconds;
        try {
            owner = UUID.fromString(segments[0]);
            expEpochSeconds = Long.parseLong(segments[1]);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        byte[] providedSignature;
        try {
            providedSignature = BASE64_URL_DECODER.decode(segments[2]);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        Instant exp = Instant.ofEpochSecond(expEpochSeconds);
        if (!exp.isAfter(now)) {
            return Optional.empty();
        }

        String payload = segments[0] + DELIMITER + segments[1];
        boolean matches = MessageDigest.isEqual(hmac(primaryKey, payload), providedSignature);
        if (!matches && secondaryKey != null) {
            matches = MessageDigest.isEqual(hmac(secondaryKey, payload), providedSignature);
        }

        return matches ? Optional.of(new VerifiedToken(owner, exp)) : Optional.empty();
    }

    private static byte[] requireKey(String label, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Advisor token signing " + label + " secret must not be blank");
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] hmac(byte[] key, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute advisor token signature", e);
        }
    }
}
