package vn.edu.utc.hotel_booking.modules.dashboard.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardKpiResponse {

    Short hotelId;
    String hotelName;
    LocalDate targetDate;

    // Core Hotel Industry Metrics
    Double occupancyRate;         // OCC % = (Occupied Rooms / Total Active Rooms) * 100
    BigDecimal averageDailyRate;  // ADR = Total Room Revenue / Occupied Rooms
    BigDecimal revPar;            // RevPAR = Total Room Revenue / Total Available Rooms

    // Inventory & Volume Metrics
    Integer totalActiveRooms;
    Integer occupiedRooms;
    Integer availableRooms;
    Integer outOfServiceRooms;     // Cleaning + Maintenance

    // Revenue Stream
    BigDecimal totalRoomRevenue;
    BigDecimal totalServiceRevenue;
    BigDecimal totalRevenueToday;

    // Operations Quick Feed
    Integer arrivalsTodayCount;
    Integer departuresTodayCount;
    Integer inHouseGuestsCount;
}
