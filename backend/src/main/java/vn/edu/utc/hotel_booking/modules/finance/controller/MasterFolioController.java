package vn.edu.utc.hotel_booking.modules.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.BillingRoutingRuleRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioAdjustmentRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.MasterFolioStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.BillingRoutingRuleResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.MasterFolioResponse;
import vn.edu.utc.hotel_booking.modules.finance.service.MasterFolioService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master-folios")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Master Folio Controller", description = "Quản lý Sổ cái Master Folio đoàn & Định tuyến chi phí tự động (Group Master Folio & Cost Routing Engine)")
public class MasterFolioController {

    MasterFolioService masterFolioService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE', 'SALES_GROUP')")
    @Operation(summary = "Lấy Sổ cái Master Folio tổng hợp theo ID")
    public ApiResponse<MasterFolioResponse> getMasterFolioById(@PathVariable Long id) {
        return ApiResponse.success("Lấy thông tin Master Folio thành công", masterFolioService.getMasterFolioById(id));
    }

    @GetMapping("/contract/{contractId}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE', 'SALES_GROUP')")
    @Operation(summary = "Lấy Sổ cái Master Folio theo ID hợp đồng đoàn")
    public ApiResponse<MasterFolioResponse> getMasterFolioByContractId(@PathVariable Long contractId) {
        return ApiResponse.success("Lấy thông tin Master Folio theo hợp đồng thành công", masterFolioService.getMasterFolioByContractId(contractId));
    }

    @GetMapping("/number/{folioNumber}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE', 'SALES_GROUP')")
    @Operation(summary = "Tra cứu Sổ cái Master Folio theo mã Folio Number")
    public ApiResponse<MasterFolioResponse> getMasterFolioByNumber(@PathVariable String folioNumber) {
        return ApiResponse.success("Tra cứu Master Folio thành công", masterFolioService.getMasterFolioByNumber(folioNumber));
    }

    @GetMapping("/{id}/routing-rules")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE', 'SALES_GROUP')")
    @Operation(summary = "Lấy danh sách quy tắc định tuyến chi phí của Master Folio")
    public ApiResponse<List<BillingRoutingRuleResponse>> getRoutingRules(@PathVariable Long id) {
        return ApiResponse.success("Lấy danh sách quy tắc định tuyến thành công", masterFolioService.getRoutingRules(id));
    }

    @PostMapping("/{id}/routing-rules")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'FINANCE', 'SALES_GROUP')")
    @Operation(summary = "Thêm mới hoặc cập nhật quy tắc định tuyến chi phí cho Master Folio")
    public ApiResponse<BillingRoutingRuleResponse> addRoutingRule(
            @PathVariable Long id,
            @Valid @RequestBody BillingRoutingRuleRequest request
    ) {
        return ApiResponse.success("Thêm quy tắc định tuyến thành công", masterFolioService.addRoutingRule(id, request));
    }

    @PutMapping("/{id}/routing-rules/{ruleId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'FINANCE', 'SALES_GROUP')")
    @Operation(summary = "Cập nhật quy tắc định tuyến chi phí")
    public ApiResponse<BillingRoutingRuleResponse> updateRoutingRule(
            @PathVariable Long id,
            @PathVariable Long ruleId,
            @Valid @RequestBody BillingRoutingRuleRequest request
    ) {
        return ApiResponse.success("Cập nhật quy tắc định tuyến thành công", masterFolioService.updateRoutingRule(id, ruleId, request));
    }

    @DeleteMapping("/{id}/routing-rules/{ruleId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'FINANCE', 'SALES_GROUP')")
    @Operation(summary = "Vô hiệu hóa quy tắc định tuyến chi phí")
    public ApiResponse<Void> deleteRoutingRule(
            @PathVariable Long id,
            @PathVariable Long ruleId
    ) {
        masterFolioService.deleteRoutingRule(id, ruleId);
        return ApiResponse.success("Vô hiệu hóa quy tắc định tuyến thành công", null);
    }

    @PostMapping("/{id}/adjust-routing")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Điều chuyển thủ công chi phí giữa Sub-Folio và Master Folio (Ngoại lệ 2a SRS UC-FIN-04)")
    public ApiResponse<MasterFolioResponse> adjustRouting(
            @PathVariable Long id,
            @Valid @RequestBody MasterFolioAdjustmentRequest request
    ) {
        String staffUsername = getCurrentUsername();
        return ApiResponse.success("Điều chuyển chi phí thành công", masterFolioService.adjustItemRouting(id, request, staffUsername));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'FINANCE')")
    @Operation(summary = "Cập nhật trạng thái Master Folio (Khóa quyết toán, đóng sổ...)")
    public ApiResponse<MasterFolioResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody MasterFolioStatusUpdateRequest request
    ) {
        return ApiResponse.success("Cập nhật trạng thái Master Folio thành công", masterFolioService.updateMasterFolioStatus(id, request));
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "STAFF";
    }
}
