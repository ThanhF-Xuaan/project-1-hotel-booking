package vn.edu.utc.hotel_booking.modules.operation.controller;

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
import vn.edu.utc.hotel_booking.modules.operation.dto.request.NightAuditRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.NightAuditResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.NightAuditService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/night-audit")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Night Audit Controller", description = "Quy trình đóng ngày, chốt doanh thu và xử lý No-show tự động")
public class NightAuditController {

    NightAuditService nightAuditService;

    @PostMapping("/execute")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'FINANCE')")
    @Operation(summary = "Thực hiện quy trình đóng ngày Night Audit (Hủy No-show, chốt doanh thu ngày)")
    public ApiResponse<NightAuditResponse> executeNightAudit(@Valid @RequestBody NightAuditRequest request) {
        return ApiResponse.success("Đóng ngày hoàn tất thành công", nightAuditService.executeNightAudit(request));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'FINANCE')")
    @Operation(summary = "Xem tổng kết doanh thu và tình trạng phòng sơ bộ trước khi đóng ngày")
    public ApiResponse<NightAuditResponse> getAuditSummary(
            @RequestParam Short hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate auditDate
    ) {
        return ApiResponse.success(nightAuditService.getAuditSummary(hotelId, auditDate));
    }
}
