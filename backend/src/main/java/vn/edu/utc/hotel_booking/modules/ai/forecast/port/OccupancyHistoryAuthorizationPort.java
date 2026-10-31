package vn.edu.utc.hotel_booking.modules.ai.forecast.port;

import java.util.Optional;

/** Adapter must resolve the principal and hotel/region membership from trusted server state. */
public interface OccupancyHistoryAuthorizationPort {
    /** Empty means denied; null or an exception means authorization dependency unavailable. */
    Optional<AuthorizedHotelScope> authorize(int hotelId);

    record AuthorizedHotelScope(int hotelId, int regionId) {
        public AuthorizedHotelScope {
            if (hotelId < 1 || hotelId > Short.MAX_VALUE || regionId < 1 || regionId > Short.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid authorized occupancy scope");
            }
        }
    }
}
