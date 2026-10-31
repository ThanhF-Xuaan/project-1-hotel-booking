package vn.edu.utc.hotel_booking.modules.ai.forecast.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record OccupancyHistoryResult(
        Status status,
        Integer hotelId,
        LocalDate fromInclusive,
        LocalDate toInclusive,
        Instant asOf,
        OccupancyHistoryBatch.DataMode dataMode,
        List<DailyOccupancySample> samples
) {
    public enum Status { COMPLETE, PARTIAL, INVALID_REQUEST, ACCESS_DENIED, DEPENDENCY_UNAVAILABLE }

    public OccupancyHistoryResult {
        Objects.requireNonNull(status, "Occupancy result status required");
        Objects.requireNonNull(asOf, "Occupancy reference instant required");
        samples = List.copyOf(samples);
        if (status == Status.COMPLETE || status == Status.PARTIAL) {
            Objects.requireNonNull(dataMode, "Occupancy provenance required");
        } else if (dataMode != null || !samples.isEmpty()) {
            throw new IllegalArgumentException("Failed occupancy result must not contain data");
        }
    }

    public long completeDays() {
        return samples.stream().filter(sample -> sample.coverage() == DailyOccupancySample.Coverage.COMPLETE).count();
    }

    public long usableDays() {
        return samples.stream().filter(sample -> sample.occupancyPercent().isPresent()).count();
    }
}
