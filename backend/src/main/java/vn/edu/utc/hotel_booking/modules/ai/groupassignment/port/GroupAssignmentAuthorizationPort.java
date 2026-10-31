package vn.edu.utc.hotel_booking.modules.ai.groupassignment.port;

import java.util.Optional;

/** Resolve principal membership from trusted server state; selectors are not authorization. */
public interface GroupAssignmentAuthorizationPort {
    Optional<Scope> authorize(int hotelId, long contractId);
    record Scope(int hotelId, int regionId, long contractId) { }
}
