package vn.edu.utc.hotel_booking.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelPerformanceDto {

    private Integer hotelId;
    private String hotelName;
    private Short regionId;
    private String regionName;
    private Double occupancyRate;
    private BigDecimal averageDailyRate;
    private BigDecimal revPar;
    private BigDecimal roomRevenue;
    private BigDecimal fnbRevenue;
    private BigDecimal otherRevenue;
    private BigDecimal totalRevenue;
    private Integer rank;
}
