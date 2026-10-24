package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of resolving the current advisor owner.
 *
 * @param ownerKey      opaque server-side ownership key ({@code auth:keycloak:<sub>} or {@code anon:<uuid>})
 * @param tokenToWrite  raw signed cookie value to write on the response; present only when the extractor
 *                      minted a new anonymous session or renewed an existing one
 * @param tokenSource   how the owner was resolved; drives cookie issuance at the controller boundary
 */
public record ResolvedAdvisorOwner(String ownerKey, Optional<String> tokenToWrite, TokenSource tokenSource) {

    /** How the owner key was produced. */
    public enum TokenSource {
        AUTHENTICATED,
        ANON_EXISTING,
        ANON_MINTED,
        ANON_RENEWED
    }

    public ResolvedAdvisorOwner {
        Objects.requireNonNull(ownerKey, "ownerKey must not be null");
        Objects.requireNonNull(tokenSource, "tokenSource must not be null");
        tokenToWrite = tokenToWrite == null ? Optional.empty() : tokenToWrite;

        boolean tokenExpected = tokenSource == TokenSource.ANON_MINTED || tokenSource == TokenSource.ANON_RENEWED;
        if (tokenExpected != tokenToWrite.isPresent()) {
            throw new IllegalArgumentException(
                    "tokenToWrite presence is inconsistent with tokenSource " + tokenSource);
        }
    }
}
