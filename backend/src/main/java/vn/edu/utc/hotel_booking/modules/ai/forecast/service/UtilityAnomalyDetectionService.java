package vn.edu.utc.hotel_booking.modules.ai.forecast.service;

import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.UtilityAnomalyResponse;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.UtilityAnomalyResponse.*;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample.Marker;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingAuthorizationPort;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingAuthorizationPort.Scope;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingHistoryPort;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingHistoryPort.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Pure local detector. No automatic bean, live provider, business write or raw-data logging. */
public class UtilityAnomalyDetectionService {
    private static final BigDecimal MAX_READING = new BigDecimal("999999999.999");
    private static final BigDecimal MAD_MULTIPLIER = new BigDecimal("3.5").multiply(new BigDecimal("1.4826"));
    private static final BigDecimal RELATIVE_FLOOR = new BigDecimal("0.2");
    private static final BigDecimal ABSOLUTE_FLOOR = new BigDecimal("0.001");
    private final UtilityReadingAuthorizationPort authorization;
    private final UtilityReadingHistoryPort history;
    private final Clock clock;

    public UtilityAnomalyDetectionService(UtilityReadingAuthorizationPort authorization, UtilityReadingHistoryPort history, Clock clock) {
        this.authorization = Objects.requireNonNull(authorization); this.history = Objects.requireNonNull(history); this.clock = Objects.requireNonNull(clock);
    }

    public UtilityAnomalyResponse analyze(Integer hotel, Integer meter, LocalDate from, LocalDate to) {
        Instant asOf = clock.instant();
        LocalDate today = asOf.atZone(UtilityReadingHistoryPort.HOTEL_ZONE).toLocalDate();
        if (hotel == null || hotel < 1 || hotel > Short.MAX_VALUE || meter == null || meter < 1 || from == null || to == null
                || from.equals(LocalDate.MIN) || to.isBefore(from) || !from.isBefore(today) || to.isAfter(today)
                || ChronoUnit.DAYS.between(from, to) + 1 > UtilityReadingHistoryPort.MAX_DAYS) {
            return failure(Status.INVALID_REQUEST, hotel, meter, from, to, asOf);
        }
        LocalDate end = to.equals(today) ? today.minusDays(1) : to;
        try {
            Optional<Scope> initial = authorization.authorize(hotel, meter);
            if (initial == null) return failure(Status.DEPENDENCY_UNAVAILABLE, hotel, meter, from, end, asOf);
            if (initial.isEmpty()) return failure(Status.ACCESS_DENIED, hotel, meter, from, end, asOf);
            Scope scope = initial.get();
            if (scope.hotelId() != hotel || scope.meterId() != meter || scope.regionId() < 1 || scope.regionId() > Short.MAX_VALUE || scope.unit() == null) {
                return failure(Status.DEPENDENCY_UNAVAILABLE, hotel, meter, from, end, asOf);
            }
            Query query = new Query(scope, from, end, asOf);
            Batch batch = history.read(query);
            Map<LocalDate, UtilityReadingSample> readings = validate(query, batch);
            List<Point> points = detect(query, readings);
            Optional<Scope> current = authorization.authorize(hotel, meter);
            if (current == null) return failure(Status.DEPENDENCY_UNAVAILABLE, hotel, meter, from, end, asOf);
            if (!initial.equals(current)) return failure(Status.ACCESS_DENIED, hotel, meter, from, end, asOf);
            boolean evaluated = points.stream().anyMatch(p -> p.status() == PointStatus.NORMAL || p.status() == PointStatus.NEEDS_REVIEW);
            return new UtilityAnomalyResponse(evaluated ? Status.ANALYZED : Status.INSUFFICIENT_COVERAGE,
                    evaluated ? null : "INSUFFICIENT_VALID_REFERENCE", hotel, meter, scope.unit(), batch.dataMode(),
                    "u22-causal-median-mad-v1", asOf, from, end, points);
        } catch (RuntimeException exception) {
            return failure(Status.DEPENDENCY_UNAVAILABLE, hotel, meter, from, end, asOf);
        }
    }

    private Map<LocalDate, UtilityReadingSample> validate(Query query, Batch batch) {
        long max = ChronoUnit.DAYS.between(query.fromInclusive(), query.toInclusive()) + 2;
        if (batch == null || !query.scope().equals(batch.scope()) || !"SYNTHETIC".equals(batch.dataMode())
                || !"SYNTHETIC_CUMULATIVE_DAILY_V1".equals(batch.basis()) || batch.coverage() != Coverage.COMPLETE
                || batch.readings() == null || batch.readings().size() > max) throw invalid();
        Map<LocalDate, UtilityReadingSample> readings = new TreeMap<>();
        for (var row : batch.readings()) {
            if (row.hotelId() != query.scope().hotelId() || row.meterId() != query.scope().meterId() || row.unit() != query.scope().unit()
                    || row.date() == null || row.date().isBefore(query.anchorDate()) || row.date().isAfter(query.toInclusive())
                    || row.cumulativeValue() == null || row.cumulativeValue().abs().compareTo(MAX_READING) > 0
                    || row.cumulativeValue().stripTrailingZeros().scale() > 3 || row.counterEpoch() == null
                    || !row.counterEpoch().matches("[A-Za-z0-9_-]{1,64}") || row.marker() == null
                    || readings.putIfAbsent(row.date(), row) != null) throw invalid();
        }
        String previousEpoch = null;
        for (var row : readings.values()) {
            if (previousEpoch != null && !previousEpoch.equals(row.counterEpoch()) && row.marker() == Marker.NORMAL) throw invalid();
            previousEpoch = row.counterEpoch();
        }
        return readings;
    }

    private List<Point> detect(Query query, Map<LocalDate, UtilityReadingSample> readings) {
        List<Point> points = new ArrayList<>();
        List<BigDecimal> reference = new ArrayList<>();
        for (LocalDate date = query.fromInclusive(); !date.isAfter(query.toInclusive()); date = date.plusDays(1)) {
            UtilityReadingSample current = readings.get(date), previous = readings.get(date.minusDays(1));
            if (current == null) { points.add(excluded(date, "MISSING_READING", reference.size())); continue; }
            if (current.marker() != Marker.NORMAL) { reference.clear(); points.add(excluded(date, current.marker().name(), 0)); continue; }
            if (current.cumulativeValue().signum() < 0) { reference.clear(); points.add(excluded(date, "NEGATIVE_READING", 0)); continue; }
            if (previous == null) { points.add(excluded(date, "MISSING_PREDECESSOR", reference.size())); continue; }
            if (previous.cumulativeValue().signum() < 0 || !previous.counterEpoch().equals(current.counterEpoch())) {
                reference.clear(); points.add(excluded(date, "INVALID_PREDECESSOR", 0)); continue;
            }
            BigDecimal usage = current.cumulativeValue().subtract(previous.cumulativeValue());
            if (usage.signum() < 0) { reference.clear(); points.add(excluded(date, "NEGATIVE_DELTA", 0)); continue; }
            int count = reference.size();
            if (count < 28) {
                points.add(new Point(date, PointStatus.INSUFFICIENT_COVERAGE, "FEWER_THAN_28_VALID_PRIOR_POINTS", usage, count, null, null, null, null));
            } else {
                BigDecimal median = median(reference);
                BigDecimal mad = median(reference.stream().map(value -> value.subtract(median).abs()).toList());
                BigDecimal threshold = mad.multiply(MAD_MULTIPLIER).max(median.abs().multiply(RELATIVE_FLOOR)).max(ABSOLUTE_FLOOR);
                boolean flag = usage.subtract(median).abs().compareTo(threshold) > 0;
                points.add(new Point(date, flag ? PointStatus.NEEDS_REVIEW : PointStatus.NORMAL, flag ? "ROBUST_DEVIATION" : null,
                        usage, count, median, mad, threshold, flag ? "Cần kiểm tra" : null));
            }
            reference.add(usage); // Current target never participates in its own baseline.
        }
        return List.copyOf(points);
    }

    private BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.valueOf(2));
    }
    private Point excluded(LocalDate date, String reason, int references) {
        return new Point(date, PointStatus.EXCLUDED, reason, null, references, null, null, null, null);
    }
    private IllegalArgumentException invalid() { return new IllegalArgumentException("INVALID_UTILITY_HISTORY"); }
    private UtilityAnomalyResponse failure(Status status, Integer hotel, Integer meter, LocalDate from, LocalDate to, Instant asOf) {
        return new UtilityAnomalyResponse(status, status.name(), hotel, meter, null, null, null, asOf, from, to, List.of());
    }
}
