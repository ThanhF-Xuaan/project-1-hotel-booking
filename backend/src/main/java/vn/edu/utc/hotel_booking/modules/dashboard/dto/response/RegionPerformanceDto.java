package vn.edu.utc.hotel_booking.modules.dashboard.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegionPerformanceDto {

    Short regionId;
    String regionCode;
    String regionName;
    Integer hotelCount;
    Double occupancyRate;
    BigDecimal averageDailyRate;
    BigDecimal revPar;
    BigDecimal roomRevenue;
    BigDecimal fnbRevenue;
    BigDecimal totalRevenue;
    Double contributePercentage;
}
