package vn.edu.utc.hotel_booking.modules.ai.demo.data;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.Snapshot;

/** Data provider for AI demonstration and historical forecast replays. */
public interface DemoDataProvider extends AutoCloseable {
    record OccupancyData(int hotelId, int regionId, Instant reference, List<DailyOccupancySample> rows, OccupancyHistoryBatch.DataMode dataMode) {
        public OccupancyData {
            rows = List.copyOf(rows);
            dataMode = dataMode == null ? OccupancyHistoryBatch.DataMode.SYNTHETIC : dataMode;
        }
        public OccupancyData(int hotelId, int regionId, Instant reference, List<DailyOccupancySample> rows) {
            this(hotelId, regionId, reference, rows, OccupancyHistoryBatch.DataMode.SYNTHETIC);
        }
    }
    record GroupData(GroupRoomAssignmentPreviewRequest request, Snapshot snapshot) { }
    <T> T occupancy(String scenario, Function<OccupancyData, T> action);
    <T> T group(String sourceId, Function<GroupData, T> action);
    @Override default void close() { }
}
