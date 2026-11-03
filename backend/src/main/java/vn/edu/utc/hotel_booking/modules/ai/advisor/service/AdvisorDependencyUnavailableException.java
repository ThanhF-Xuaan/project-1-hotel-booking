package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

/** Fixed diagnostic only: dependency messages can contain private request data. */
public class AdvisorDependencyUnavailableException extends RuntimeException {
    public AdvisorDependencyUnavailableException() { super("Advisor dependency unavailable"); }
}
