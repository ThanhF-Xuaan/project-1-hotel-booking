package vn.edu.utc.hotel_booking.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutiveBiReportResponse {

    private String scope;
    private String scopeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isProvisional;
    private String provisionalNotice;
    private ExecutiveKpiSummaryDto summary;
    private DepartmentRevenueBreakdownDto departmentRevenue;
    private SegmentRevenueBreakdownDto segmentRevenue;
    private List<RegionPerformanceDto> regionBreakdowns;
    private List<HotelPerformanceDto> hotelBreakdowns;
    private List<BiTrendPointDto> trends;
}
