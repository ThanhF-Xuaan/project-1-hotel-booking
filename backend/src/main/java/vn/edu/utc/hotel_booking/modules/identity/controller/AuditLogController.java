package vn.edu.utc.hotel_booking.modules.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.AuditLogSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.AuditLogResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Identity - Audit Logs", description = "APIs truy vấn nhật ký kiểm toán hệ thống")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Tìm kiếm và phân trang nhật ký kiểm toán")
    public ApiResponse<PageResponse<AuditLogResponse>> filter(@RequestBody @Valid AuditLogSearchDto searchDto) {
        return ApiResponse.<PageResponse<AuditLogResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách nhật ký kiểm toán thành công")
                .result(auditLogService.filter(searchDto))
                .build();
    }
}
