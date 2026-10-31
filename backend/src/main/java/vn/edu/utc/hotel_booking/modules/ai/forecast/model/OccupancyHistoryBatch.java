package vn.edu.utc.hotel_booking.modules.ai.forecast.model;

import java.util.List;
import java.util.Objects;

/** Provenance supplied by a history adapter, never inferred from numeric values. */
public record OccupancyHistoryBatch(DataMode dataMode, List<DailyOccupancySample> samples) {
    public enum DataMode { SYNTHETIC, AUTHORITATIVE }

    public OccupancyHistoryBatch {
        Objects.requireNonNull(dataMode, "Occupancy provenance required");
        samples = List.copyOf(samples);
    }
}
