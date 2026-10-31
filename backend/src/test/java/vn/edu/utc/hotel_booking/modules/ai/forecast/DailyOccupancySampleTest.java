package vn.edu.utc.hotel_booking.modules.ai.forecast;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample.Coverage;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch.DataMode;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryAuthorizationPort.AuthorizedHotelScope;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DailyOccupancySampleTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 29);

    @ParameterizedTest
    @CsvSource({"0,0", "0,100", "55,100", "100,100", "2147483647,2147483647"})
    void completeCountsHaveBoundedPercentAndZeroCapacityHasNoTarget(int occupied, int capacity) {
        var sample = new DailyOccupancySample(1, DATE, occupied, capacity, Coverage.COMPLETE);
        if (capacity == 0) assertThat(sample.occupancyPercent()).isEmpty();
        else assertThat(sample.occupancyPercent().orElseThrow()).isEqualTo(100.0 * occupied / capacity).isBetween(0.0, 100.0);
    }

    @ParameterizedTest
    @CsvSource(value = {"-1,100", "0,-1", "101,100", "1,0", "null,100", "1,null"}, nullValues = "null")
    void rejectsInvalidCountsWithoutEchoingThem(Integer occupied, Integer capacity) {
        assertThatThrownBy(() -> new DailyOccupancySample(1, DATE, occupied, capacity, Coverage.COMPLETE))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid occupancy counts");
    }

    @Test
    void missingIsDistinctFromAnObservedZero() {
        var missing = DailyOccupancySample.missing(1, DATE);
        assertThat(missing.occupiedRooms()).isNull();
        assertThat(missing.capacityRooms()).isNull();
        assertThat(missing.occupancyPercent()).isEmpty();
        var emptyHotel = new DailyOccupancySample(1, DATE, 0, 100, Coverage.COMPLETE);
        assertThat(emptyHotel.occupancyPercent()).hasValue(0.0);
        assertThatThrownBy(() -> new DailyOccupancySample(1, DATE, 0, null, Coverage.MISSING))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DailyOccupancySample(1, DATE, null, 100, Coverage.MISSING))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 32768})
    void rejectsUnrepresentableHotelAndRegionScopes(int id) {
        assertThatThrownBy(() -> DailyOccupancySample.missing(id, DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthorizedHotelScope(id, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthorizedHotelScope(1, id)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresDateCoverageAndProvenanceAndCopiesBatch() {
        assertThatThrownBy(() -> DailyOccupancySample.missing(1, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DailyOccupancySample(1, DATE, 0, 1, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OccupancyHistoryBatch(null, List.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OccupancyHistoryBatch(DataMode.SYNTHETIC, null)).isInstanceOf(NullPointerException.class);
        List<DailyOccupancySample> original = new ArrayList<>();
        original.add(DailyOccupancySample.missing(1, DATE));
        var batch = new OccupancyHistoryBatch(DataMode.SYNTHETIC, original);
        original.clear();
        assertThat(batch.samples()).hasSize(1);
        assertThatThrownBy(() -> batch.samples().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
