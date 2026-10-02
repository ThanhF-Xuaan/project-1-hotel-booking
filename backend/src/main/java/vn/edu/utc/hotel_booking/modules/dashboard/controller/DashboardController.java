package vn.edu.utc.hotel_booking.modules.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.DashboardKpiResponse;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.OccupancyTrendDto;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.RevenueTrendDto;
import vn.edu.utc.hotel_booking.modules.dashboard.service.DashboardService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Dashboard Controller", description = "Báo cáo quản trị, chỉ số KPI ngành khách sạn (OCC, ADR, RevPAR) và biểu đồ xu hướng")
public class DashboardController {

    DashboardService dashboardService;

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Lấy các chỉ số KPI điều hành (OCC %, ADR, RevPAR, Doanh thu & Luồng khách hôm nay)")
    public ApiResponse<DashboardKpiResponse> getExecutiveKpis(
            @RequestParam Short hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        return ApiResponse.success(dashboardService.getExecutiveKpis(hotelId, targetDate));
    }

    @GetMapping("/revenue-trends")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'FINANCE')")
    @Operation(summary = "Lấy dữ liệu xu hướng doanh thu theo chu kỳ ngày")
    public ApiResponse<List<RevenueTrendDto>> getRevenueTrends(
            @RequestParam Short hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.success(dashboardService.getRevenueTrends(hotelId, startDate, endDate));
    }

    @GetMapping("/occupancy-trends")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'FINANCE')")
    @Operation(summary = "Lấy dữ liệu xu hướng tỷ lệ lấp đầy phòng (Occupancy Rate %)")
    public ApiResponse<List<OccupancyTrendDto>> getOccupancyTrends(
            @RequestParam Short hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.success(dashboardService.getOccupancyTrends(hotelId, startDate, endDate));
    }
}
