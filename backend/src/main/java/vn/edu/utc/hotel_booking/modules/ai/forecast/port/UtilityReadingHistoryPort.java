package vn.edu.utc.hotel_booking.modules.ai.forecast.port;

import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingAuthorizationPort.Scope;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public interface UtilityReadingHistoryPort {
    ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    int MAX_DAYS = 42;
    Batch read(Query query);

    record Query(Scope scope, LocalDate fromInclusive, LocalDate toInclusive, Instant asOf) {
        public Query {
            Objects.requireNonNull(scope); Objects.requireNonNull(fromInclusive);
            Objects.requireNonNull(toInclusive); Objects.requireNonNull(asOf);
            long days = ChronoUnit.DAYS.between(fromInclusive, toInclusive) + 1;
            if (fromInclusive.equals(LocalDate.MIN) || days < 1 || days > MAX_DAYS || !toInclusive.isBefore(asOf.atZone(HOTEL_ZONE).toLocalDate())) {
                throw new IllegalArgumentException("Invalid completed utility window");
            }
        }
        public LocalDate anchorDate() { return fromInclusive.minusDays(1); }
        public Instant fromUtcInclusive() { return fromInclusive.atStartOfDay(HOTEL_ZONE).toInstant(); }
        public Instant toUtcExclusive() { return toInclusive.plusDays(1).atStartOfDay(HOTEL_ZONE).toInstant(); }
    }

    enum Coverage { COMPLETE, PARTIAL, UNKNOWN }
    record Batch(Scope scope, String dataMode, String basis, Coverage coverage, List<UtilityReadingSample> readings) {
        public Batch { if (readings != null) readings = List.copyOf(readings); }
    }
}
