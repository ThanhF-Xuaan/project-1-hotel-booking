package vn.edu.utc.hotel_booking.modules.ai.forecast.port;

import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryAuthorizationPort.AuthorizedHotelScope;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Read-only aggregate boundary. No production adapter or synthetic fallback is registered. */
public interface OccupancyHistoryPort {
    int MAX_HISTORY_DAYS = 420;
    ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    OccupancyHistoryBatch read(Query query);

    record Query(AuthorizedHotelScope scope, LocalDate fromInclusive, LocalDate toInclusive, Instant asOf) {
        public Query {
            Objects.requireNonNull(scope, "Authorized occupancy scope required");
            Objects.requireNonNull(fromInclusive, "Occupancy start required");
            Objects.requireNonNull(toInclusive, "Occupancy end required");
            Objects.requireNonNull(asOf, "Occupancy reference instant required");
            if (toInclusive.isBefore(fromInclusive)
                    || ChronoUnit.DAYS.between(fromInclusive, toInclusive) + 1 > MAX_HISTORY_DAYS
                    || !toInclusive.isBefore(asOf.atZone(HOTEL_ZONE).toLocalDate())) {
                throw new IllegalArgumentException("Invalid completed occupancy window");
            }
        }

        public Instant fromUtcInclusive() { return fromInclusive.atStartOfDay(HOTEL_ZONE).toInstant(); }
        public Instant toUtcExclusive() { return toInclusive.plusDays(1).atStartOfDay(HOTEL_ZONE).toInstant(); }
    }
}
