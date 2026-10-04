package vn.edu.utc.hotel_booking.modules.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.ErrorRowDto;
import vn.edu.utc.hotel_booking.modules.finance.service.PayrollSummaryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@Tag(name = "Payroll", description = "API cho quản lý lương nhân viên")
public class PayrollSummaryController {

    private final PayrollSummaryService payrollSummaryService;

    @Operation(summary = "Import Payroll Summary từ file CSV", description = "Chấp nhận file CSV. Trả về 200 OK kèm mảng lỗi (nếu có dòng lỗi).")
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<List<ErrorRowDto>>> importCsv(
            @RequestParam("file") MultipartFile file) {
        
        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);
        
        ApiResponse<List<ErrorRowDto>> response = ApiResponse.<List<ErrorRowDto>>builder()
                .code(200)
                .message("Import process completed")
                .result(errors)
                .build();
                
        return ResponseEntity.ok(response);
    }
}
