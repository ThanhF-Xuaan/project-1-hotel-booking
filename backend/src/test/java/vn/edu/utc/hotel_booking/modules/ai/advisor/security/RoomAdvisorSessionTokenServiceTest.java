package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit coverage for minting, verifying, expiring and rotating anonymous advisor tokens.
 * No Spring context is loaded; the signing secret is supplied explicitly.
 */
class RoomAdvisorSessionTokenServiceTest {

    private static final String TEST_SECRET = "T12-test-only-7f3c1a9e5b2d8460c4f19a3e7b2d5c80";
    private static final String TEST_SECONDARY_SECRET = "T12-test-secondary-2b8e4d6a1c3f5e7b9d0a2c4e6f8b1d35";
    private static final String FOREIGN_SECRET = "T12-foreign-key-93d1a7c5e2b8460f1a9c3e5d7b2f4a68";
    private static final String ROTATED_PRIMARY_SECRET = "T12-rotated-primary-5c8e2a4f6b1d9370a2c4e6f8b0d2f4a6";

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final Duration TTL = RoomAdvisorSessionTokenService.DEFAULT_SESSION_TTL;

    private RoomAdvisorSessionTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new RoomAdvisorSessionTokenService(TEST_SECRET, null);
    }

    @Test
    @DisplayName("AC-1.7: minted tokens carry the owner UUID and absolute exp claim")
    void mintedTokenCarriesOwnerAndExpiry() {
        UUID owner = UUID.randomUUID();
        Instant exp = NOW.plus(TTL);

        String token = tokenService.mint(owner, exp);

        assertThat(token.split("\\.", -1)).hasSize(3);
        assertThat(token.split("\\.", -1)[0]).isEqualTo(owner.toString());
        assertThat(token.split("\\.", -1)[1]).isEqualTo(String.valueOf(exp.getEpochSecond()));

        Optional<RoomAdvisorSessionTokenService.VerifiedToken> verified = tokenService.verify(token, NOW);
        assertThat(verified).isPresent();
        assertThat(verified.get().owner()).isEqualTo(owner);
        assertThat(verified.get().exp()).isEqualTo(exp);
    }

    @Test
    @DisplayName("AC-1.3: minting without an explicit owner generates a fresh UUID")
    void mintWithoutOwnerGeneratesFreshUuid() {
        String token = tokenService.mint(NOW.plus(TTL));

        Optional<RoomAdvisorSessionTokenService.VerifiedToken> verified = tokenService.verify(token, NOW);
        assertThat(verified).isPresent();
        assertThat(verified.get().owner()).isNotNull();
        assertThat(verified.get().exp()).isEqualTo(NOW.plus(TTL));
    }

    @Test
    @DisplayName("AC-1.7: exp at or before the verification instant is rejected")
    void expiredTokenIsRejected() {
        UUID owner = UUID.randomUUID();
        Instant exp = NOW.plus(Duration.ofMinutes(1));
        String token = tokenService.mint(owner, exp);

        assertThat(tokenService.verify(token, exp.minusSeconds(1))).isPresent();
        assertThat(tokenService.verify(token, exp)).isEmpty();
        assertThat(tokenService.verify(token, exp.plusSeconds(1))).isEmpty();
    }

    @Test
    @DisplayName("AC-1.7: tampered tokens are rejected")
    void tamperedTokenIsRejected() {
        UUID owner = UUID.randomUUID();
        String token = tokenService.mint(owner, NOW.plus(TTL));
        String[] segments = token.split("\\.", -1);

        String tamperedToken = tamperSignature(token);

        assertThat(tamperedToken).isNotEqualTo(token);
        assertThat(tokenService.verify(tamperedToken, NOW)).isEmpty();
        assertThat(tokenService.verify(UUID.randomUUID() + "." + segments[1] + "." + segments[2], NOW)).isEmpty();
        assertThat(tokenService.verify(segments[0] + "." + segments[1] + "0." + segments[2], NOW)).isEmpty();
    }

    @Test
    @DisplayName("AC-1.7: malformed tokens are rejected without throwing")
    void malformedTokenIsRejected() {
        String[] malformed = {
                null,
                "",
                "   ",
                "not-a-token",
                "a.b",
                "a.b.c.d",
                UUID.randomUUID() + ".notanumber.signature",
                "not-a-uuid.123.signature",
                UUID.randomUUID() + ".123.%%%not-base64%%%",
                UUID.randomUUID() + ".123."
        };

        for (String candidate : malformed) {
            assertThat(tokenService.verify(candidate, NOW))
                    .as("token '%s' must be rejected", candidate)
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("AC-1.4: tokens signed by a key that is neither primary nor secondary are rejected")
    void unknownSigningKeyIsRejected() {
        RoomAdvisorSessionTokenService foreignService = new RoomAdvisorSessionTokenService(FOREIGN_SECRET, null);
        String foreignToken = foreignService.mint(UUID.randomUUID(), NOW.plus(TTL));

        assertThat(tokenService.verify(foreignToken, NOW)).isEmpty();

        RoomAdvisorSessionTokenService rotated = new RoomAdvisorSessionTokenService(ROTATED_PRIMARY_SECRET, TEST_SECRET);
        assertThat(rotated.verify(foreignToken, NOW)).isEmpty();
    }

    @Test
    @DisplayName("AC-1.4: during rotation, primary and secondary tokens verify while new tokens use primary only")
    void rotationAcceptsOldAndNewKeys() {
        UUID owner = UUID.randomUUID();
        String oldToken = tokenService.mint(owner, NOW.plus(TTL));

        RoomAdvisorSessionTokenService rotated =
                new RoomAdvisorSessionTokenService(ROTATED_PRIMARY_SECRET, TEST_SECRET);

        assertThat(rotated.verify(oldToken, NOW)).isPresent()
                .get()
                .extracting(RoomAdvisorSessionTokenService.VerifiedToken::owner)
                .isEqualTo(owner);

        String newlyMinted = rotated.mint(owner, NOW.plus(TTL));

        RoomAdvisorSessionTokenService primaryOnly =
                new RoomAdvisorSessionTokenService(ROTATED_PRIMARY_SECRET, null);
        RoomAdvisorSessionTokenService retiredKeyOnly =
                new RoomAdvisorSessionTokenService(TEST_SECRET, null);

        assertThat(primaryOnly.verify(newlyMinted, NOW)).isPresent();
        assertThat(retiredKeyOnly.verify(newlyMinted, NOW)).isEmpty();
    }

    @Test
    @DisplayName("AC-1.4: removing the secondary key invalidates old-primary tokens within one TTL")
    void retiredSecondaryKeyInvalidatesOldTokens() {
        String oldToken = tokenService.mint(UUID.randomUUID(), NOW.plus(TTL));

        RoomAdvisorSessionTokenService rotated =
                new RoomAdvisorSessionTokenService(ROTATED_PRIMARY_SECRET, TEST_SECRET);
        assertThat(rotated.verify(oldToken, NOW.plusSeconds(1))).isPresent();

        RoomAdvisorSessionTokenService secondaryRemoved =
                new RoomAdvisorSessionTokenService(ROTATED_PRIMARY_SECRET, null);
        assertThat(secondaryRemoved.verify(oldToken, NOW.plusSeconds(TTL.toSeconds() - 1))).isEmpty();
    }

    @Test
    @DisplayName("AC-1.7: restart with the same secret still verifies tokens to the same owner")
    void restartWithSameSecretKeepsTokensValid() {
        UUID owner = UUID.randomUUID();
        String token = tokenService.mint(owner, NOW.plus(TTL));

        RoomAdvisorSessionTokenService restarted = new RoomAdvisorSessionTokenService(TEST_SECRET, null);

        assertThat(restarted.verify(token, NOW)).isPresent()
                .get()
                .extracting(RoomAdvisorSessionTokenService.VerifiedToken::owner)
                .isEqualTo(owner);
    }

    @Test
    @DisplayName("AC-1.7: a fresh instance with a different secret rejects previously issued tokens")
    void foreignSecretRejectsPreviouslyIssuedTokens() {
        String token = tokenService.mint(UUID.randomUUID(), NOW.plus(TTL));
        RoomAdvisorSessionTokenService restarted = new RoomAdvisorSessionTokenService(FOREIGN_SECRET, null);

        assertThat(restarted.verify(token, NOW)).isEmpty();
    }

    @Test
    @DisplayName("AC-1.5: blank signing secrets are rejected")
    void blankSigningSecretIsRejected() {
        assertThatThrownBy(() -> new RoomAdvisorSessionTokenService(null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RoomAdvisorSessionTokenService("   ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("AC-1.4: a blank secondary secret disables rotation instead of failing")
    void blankSecondarySecretDisablesRotation() {
        RoomAdvisorSessionTokenService serviceWithoutSecondary =
                new RoomAdvisorSessionTokenService(TEST_SECRET, "   ");
        String token = serviceWithoutSecondary.mint(UUID.randomUUID(), NOW.plus(TTL));

        assertThat(serviceWithoutSecondary.verify(token, NOW)).isPresent();
    }

    @Test
    @DisplayName("AC-1.5: signature comparison is constant-time")
    void signatureComparisonIsConstantTime() throws IOException {
        String source = readSecuritySource("RoomAdvisorSessionTokenService.java");

        assertThat(source).contains("MessageDigest.isEqual");
        assertThat(source).doesNotContain("Arrays.equals");
    }

    /**
     * Builds a token whose HMAC signature differs from the original at the decoded-byte level.
     *
     * <p>The final Base64URL character of a 32-byte signature only carries four significant bits;
     * the trailing padding bits are ignored by the decoder, so replacing that character can leave
     * the decoded signature byte-identical. This helper therefore flips one bit inside a decoded
     * signature byte and re-encodes it, which always yields a genuinely different signature.</p>
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
