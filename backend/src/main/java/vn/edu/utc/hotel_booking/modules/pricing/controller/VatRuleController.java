package vn.edu.utc.hotel_booking.modules.pricing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.annotation.Auditable;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VatRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VatRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.VatRuleService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/vat-rules")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - VAT Rules", description = "APIs quản lý quy tắc thuế suất VAT theo thời gian hiệu lực")
public class VatRuleController {

    VatRuleService vatRuleService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'FINANCE', 'RECEPTIONIST')")
    @Operation(summary = "Tìm kiếm & lọc danh sách quy tắc thuế VAT")
    public ResponseEntity<ApiResponse<PageResponse<VatRuleResponse>>> filterVatRules(
            @RequestBody VatRuleSearchDto searchDto) {
        PageResponse<VatRuleResponse> response = vatRuleService.filter(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'FINANCE', 'RECEPTIONIST')")
    @Operation(summary = "Xem chi tiết quy tắc thuế VAT theo ID")
    public ResponseEntity<ApiResponse<VatRuleResponse>> getVatRuleById(@PathVariable Integer id) {
        VatRuleResponse response = vatRuleService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "VAT_RULE_CREATE",
            entityName = "VatRule",
            description = "'Tạo mới quy tắc VAT: ' + #request.vatCode + ' (' + #request.vatPercent + '%)'",
            captureResponse = true
    )
    @PostMapping
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới quy tắc thuế VAT (Chỉ Chain Admin)")
    public ResponseEntity<ApiResponse<VatRuleResponse>> createVatRule(
            @Valid @RequestBody VatRuleCreateRequest request) {
        VatRuleResponse response = vatRuleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "VAT_RULE_UPDATE",
            entityName = "VatRule",
            entityId = "#id.toString()",
            description = "'Cập nhật quy tắc VAT ID ' + #id + ': ' + #request.vatName"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật quy tắc thuế VAT (Chỉ Chain Admin)")
    public ResponseEntity<ApiResponse<VatRuleResponse>> updateVatRule(
            @PathVariable Integer id,
            @Valid @RequestBody VatRuleUpdateRequest request) {
        VatRuleResponse response = vatRuleService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "VAT_RULE_DELETE",
            entityName = "VatRule",
            entityId = "#ids.toString()",
            description = "'Xóa các quy tắc VAT: ' + #ids.toString()"
    )
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt quy tắc VAT (Soft Delete, Chỉ Chain Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteVatRules(@RequestBody List<Integer> ids) {
        vatRuleService.deleteBatch(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

