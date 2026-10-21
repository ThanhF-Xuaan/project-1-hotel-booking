package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

/**
 * Thrown when the Redis session storage is unavailable or fails during an operation.
 *
 * <p>Mapped to HTTP 503 UPSTREAM_UNAVAILABLE with advisor-local {@code RoomAdvisorErrorResponse}
 * at the controller boundary (ADR-05, AC-4.5).</p>
 */
public class SessionStorageUnavailableException extends RuntimeException {

    public SessionStorageUnavailableException(String message) {
        super(message);
    }

    public SessionStorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
