package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String message) {
        super(message);
    }
}
