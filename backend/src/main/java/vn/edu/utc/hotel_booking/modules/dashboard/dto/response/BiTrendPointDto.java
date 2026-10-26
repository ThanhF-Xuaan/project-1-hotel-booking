package vn.edu.utc.hotel_booking.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiTrendPointDto {

    private LocalDate date;
    private String periodLabel;
    private Double occupancyRate;
    private BigDecimal adr;
    private BigDecimal revPar;
    private BigDecimal roomRevenue;
    private BigDecimal fnbRevenue;
    private BigDecimal otherRevenue;
    private BigDecimal totalRevenue;
}
