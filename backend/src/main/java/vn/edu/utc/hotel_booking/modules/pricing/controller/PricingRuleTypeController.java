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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PricingRuleTypeResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.PricingRuleTypeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/rule-types")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Rule Types", description = "APIs quản lý danh mục loại quy tắc điều chỉnh giá")
public class PricingRuleTypeController {

    PricingRuleTypeService pricingRuleTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Lấy tất cả danh mục loại quy tắc giá chưa xóa")
    public ResponseEntity<ApiResponse<List<PricingRuleTypeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(pricingRuleTypeService.getAll()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Lấy danh sách loại quy tắc giá đang kích hoạt (ACTIVE)")
    public ResponseEntity<ApiResponse<List<PricingRuleTypeResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(pricingRuleTypeService.getActive()));
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xem chi tiết loại quy tắc giá theo code")
    public ResponseEntity<ApiResponse<PricingRuleTypeResponse>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(pricingRuleTypeService.getByCode(code)));
    }

    @Auditable(
            actionType = "PRICING_RULE_TYPE_CREATE",
            entityName = "PricingRuleType",
            description = "'Tạo mới loại quy tắc giá: ' + #request.code",
            captureResponse = true
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Tạo mới loại quy tắc giá")
    public ResponseEntity<ApiResponse<PricingRuleTypeResponse>> create(
            @Valid @RequestBody PricingRuleTypeCreateRequest request) {
        PricingRuleTypeResponse response = pricingRuleTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "PRICING_RULE_TYPE_UPDATE",
            entityName = "PricingRuleType",
            entityId = "#code",
            description = "'Cập nhật loại quy tắc giá: ' + #code"
    )
    @PutMapping("/{code}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Cập nhật loại quy tắc giá")
    public ResponseEntity<ApiResponse<PricingRuleTypeResponse>> update(
            @PathVariable String code,
            @Valid @RequestBody PricingRuleTypeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(pricingRuleTypeService.update(code, request)));
    }

    @Auditable(
            actionType = "PRICING_RULE_TYPE_DELETE",
            entityName = "PricingRuleType",
            entityId = "#code",
            description = "'Xóa loại quy tắc giá: ' + #code"
    )
    @DeleteMapping("/{code}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm loại quy tắc giá")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String code) {
        pricingRuleTypeService.delete(code);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
