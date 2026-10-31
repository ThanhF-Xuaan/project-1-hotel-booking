package vn.edu.utc.hotel_booking.modules.ai.forecast.service;

import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryResult;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryResult.Status;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryAuthorizationPort;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryAuthorizationPort.AuthorizedHotelScope;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryPort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Pure application boundary; wiring waits for a verified history source and IAM adapter. */
public class OccupancyHistoryService {
    private final OccupancyHistoryAuthorizationPort authorization;
    private final OccupancyHistoryPort history;
    private final Clock clock;

    public OccupancyHistoryService(OccupancyHistoryAuthorizationPort authorization, OccupancyHistoryPort history, Clock clock) {
        this.authorization = Objects.requireNonNull(authorization);
        this.history = Objects.requireNonNull(history);
        this.clock = Objects.requireNonNull(clock);
    }

    public OccupancyHistoryResult read(Integer hotelId, LocalDate fromInclusive, LocalDate toInclusive) {
        Instant asOf = clock.instant();
        LocalDate today = asOf.atZone(OccupancyHistoryPort.HOTEL_ZONE).toLocalDate();
        if (hotelId == null || hotelId < 1 || hotelId > Short.MAX_VALUE
                || fromInclusive == null || toInclusive == null || toInclusive.isBefore(fromInclusive)
                || !fromInclusive.isBefore(today) || toInclusive.isAfter(today)
                || ChronoUnit.DAYS.between(fromInclusive, toInclusive) + 1 > OccupancyHistoryPort.MAX_HISTORY_DAYS) {
            return failure(Status.INVALID_REQUEST, hotelId, fromInclusive, toInclusive, asOf);
        }
        LocalDate end = toInclusive.equals(today) ? today.minusDays(1) : toInclusive;
        Optional<AuthorizedHotelScope> initial = authorize(hotelId);
        if (initial == null) return failure(Status.DEPENDENCY_UNAVAILABLE, hotelId, fromInclusive, end, asOf);
        if (initial.isEmpty()) return failure(Status.ACCESS_DENIED, hotelId, fromInclusive, end, asOf);
        if (initial.get().hotelId() != hotelId) {
            return failure(Status.DEPENDENCY_UNAVAILABLE, hotelId, fromInclusive, end, asOf);
        }

        OccupancyHistoryBatch batch;
        Map<LocalDate, DailyOccupancySample> byDate = new HashMap<>();
        long days = ChronoUnit.DAYS.between(fromInclusive, end) + 1;
        try {
            batch = history.read(new OccupancyHistoryPort.Query(initial.get(), fromInclusive, end, asOf));
            if (batch == null || batch.samples().size() > days) {
                return failure(Status.DEPENDENCY_UNAVAILABLE, hotelId, fromInclusive, end, asOf);
            }
            for (DailyOccupancySample sample : batch.samples()) {
                if (sample.hotelId() != hotelId || sample.date().isBefore(fromInclusive) || sample.date().isAfter(end)
                        || byDate.putIfAbsent(sample.date(), sample) != null) {
                    return failure(Status.DEPENDENCY_UNAVAILABLE, hotelId, fromInclusive, end, asOf);
                }
            }
        } catch (RuntimeException exception) {
            // Dependency payloads and exception messages may be sensitive: return a stable code only.
            return failure(Status.DEPENDENCY_UNAVAILABLE, hotelId, fromInclusive, end, asOf);
        }

        Optional<AuthorizedHotelScope> current = authorize(hotelId);
        if (current == null) return failure(Status.DEPENDENCY_UNAVAILABLE, hotelId, fromInclusive, end, asOf);
        if (current.isEmpty() || !initial.equals(current)) {
            return failure(Status.ACCESS_DENIED, hotelId, fromInclusive, end, asOf);
        }
        List<DailyOccupancySample> samples = new ArrayList<>((int) days);
        boolean complete = true;
        for (int offset = 0; offset < days; offset++) {
            LocalDate date = fromInclusive.plusDays(offset);
            DailyOccupancySample sample = byDate.getOrDefault(date, DailyOccupancySample.missing(hotelId, date));
            samples.add(sample);
            complete &= sample.coverage() == DailyOccupancySample.Coverage.COMPLETE;
        }
        return new OccupancyHistoryResult(complete ? Status.COMPLETE : Status.PARTIAL, hotelId,
                fromInclusive, end, asOf, batch.dataMode(), samples);
    }

    private Optional<AuthorizedHotelScope> authorize(int hotelId) {
        try { return authorization.authorize(hotelId); }
        catch (RuntimeException exception) { return null; }
    }

    private OccupancyHistoryResult failure(Status status, Integer hotel, LocalDate from, LocalDate to, Instant asOf) {
        return new OccupancyHistoryResult(status, hotel, from, to, asOf, null, List.of());
    }
}
