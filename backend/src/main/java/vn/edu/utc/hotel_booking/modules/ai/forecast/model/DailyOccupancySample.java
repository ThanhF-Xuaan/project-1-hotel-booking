package vn.edu.utc.hotel_booking.modules.ai.forecast.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.OptionalDouble;

/** One completed local room-night observation; missing counts are never zero-filled. */
public record DailyOccupancySample(
        int hotelId,
        LocalDate date,
        Integer occupiedRooms,
        Integer capacityRooms,
        Coverage coverage
) {
    public enum Coverage { COMPLETE, MISSING }

    public DailyOccupancySample {
        if (hotelId < 1 || hotelId > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid occupancy hotel");
        }
        Objects.requireNonNull(date, "Occupancy date required");
        Objects.requireNonNull(coverage, "Occupancy coverage required");
        if (coverage == Coverage.MISSING) {
            if (occupiedRooms != null || capacityRooms != null) {
                throw new IllegalArgumentException("Missing occupancy must not contain counts");
            }
        } else if (occupiedRooms == null || capacityRooms == null
                || occupiedRooms < 0 || capacityRooms < 0 || occupiedRooms > capacityRooms) {
            throw new IllegalArgumentException("Invalid occupancy counts");
        }
    }

    public static DailyOccupancySample missing(int hotelId, LocalDate date) {
        return new DailyOccupancySample(hotelId, date, null, null, Coverage.MISSING);
    }

    public OptionalDouble occupancyPercent() {
        return coverage == Coverage.COMPLETE && capacityRooms > 0
                ? OptionalDouble.of(100.0 * occupiedRooms / capacityRooms)
                : OptionalDouble.empty();
    }
}
