package vn.edu.utc.hotel_booking.modules.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.request.ExecutiveReportFilterDto;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.ExecutiveBiReportResponse;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.ExecutiveKpiSummaryDto;
import vn.edu.utc.hotel_booking.modules.dashboard.service.ExecutiveReportService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/analytics/reports/executive")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Executive BI Report Controller", description = "API Báo cáo Quản trị BI Cấp cao (U21) - RevPAR, ADR, OCC %, Doanh thu phòng vs F&B phân tách theo Vùng/Chuỗi/Cơ sở")
public class ExecutiveReportController {

    ExecutiveReportService executiveReportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'FINANCE')")
    @Operation(summary = "Lấy báo cáo BI quản trị cấp cao đa chiều (Scope: CHAIN / REGION / HOTEL)")
    public ApiResponse<ExecutiveBiReportResponse> getExecutiveReport(
            @Valid @RequestBody(required = false) ExecutiveReportFilterDto filter
    ) {
        return ApiResponse.success(executiveReportService.generateExecutiveReport(filter));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'FINANCE')")
    @Operation(summary = "Truy vấn nhanh tóm tắt KPI cấp cao (OCC %, ADR, RevPAR, Doanh thu tổng hợp)")
    public ApiResponse<ExecutiveKpiSummaryDto> getQuickSummary(
            @RequestParam(required = false, defaultValue = "CHAIN") String scope,
            @RequestParam(required = false) Short regionId,
            @RequestParam(required = false) Short hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        return ApiResponse.success(executiveReportService.getQuickSummary(scope, regionId, hotelId, targetDate));
    }
}
