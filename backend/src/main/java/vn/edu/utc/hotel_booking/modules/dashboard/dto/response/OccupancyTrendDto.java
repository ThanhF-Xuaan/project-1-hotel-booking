package vn.edu.utc.hotel_booking.modules.dashboard.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OccupancyTrendDto {

    LocalDate date;
    Integer totalRooms;
    Integer occupiedRooms;
    Double occupancyRate;
}
