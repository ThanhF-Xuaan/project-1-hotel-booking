package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

/**
 * Raised when a Room Advisor request exceeds a bounded rate limit (ADR-04).
 *
 * <p>The exception deliberately carries only the remaining wait time. It never contains the owner
 * key, the client address or the limit that was exhausted, so the HTTP layer cannot leak which
 * budget was hit or whether the caller is authenticated.</p>
 */
public class RateLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfterSeconds) {
        super("Advisor request rate limit exceeded");
        if (retryAfterSeconds < 1) {
            throw new IllegalArgumentException("retryAfterSeconds must be a positive integer");
        }
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Remaining wait time in whole seconds; always greater than zero. */
    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
