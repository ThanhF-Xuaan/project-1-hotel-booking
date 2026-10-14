package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

public class SessionVersionConflictException extends RuntimeException {
    public SessionVersionConflictException(String message) {
        super(message);
    }
}
