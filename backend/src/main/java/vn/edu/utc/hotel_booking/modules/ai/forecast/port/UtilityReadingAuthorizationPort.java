package vn.edu.utc.hotel_booking.modules.ai.forecast.port;

import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample.Unit;

import java.util.Optional;

/** Resolve meter ownership, region, unit and principal from trusted server state. */
public interface UtilityReadingAuthorizationPort {
    Optional<Scope> authorize(int hotelId, int meterId);
    record Scope(int hotelId, int regionId, int meterId, Unit unit) { }
}
