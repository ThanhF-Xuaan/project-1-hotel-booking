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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleTypeResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.DiscountRuleTypeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/discount-rule-types")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Discount Rule Types", description = "APIs quản lý danh mục loại quy tắc giảm giá / khuyến mại")
public class DiscountRuleTypeController {

    DiscountRuleTypeService discountRuleTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Lấy tất cả danh mục loại quy tắc giảm giá chưa xóa")
    public ResponseEntity<ApiResponse<List<DiscountRuleTypeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(discountRuleTypeService.getAll()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Lấy danh sách loại quy tắc giảm giá đang kích hoạt (ACTIVE)")
    public ResponseEntity<ApiResponse<List<DiscountRuleTypeResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(discountRuleTypeService.getActive()));
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xem chi tiết loại quy tắc giảm giá theo code")
    public ResponseEntity<ApiResponse<DiscountRuleTypeResponse>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(discountRuleTypeService.getByCode(code)));
    }

    @Auditable(
            actionType = "DISCOUNT_RULE_TYPE_CREATE",
            entityName = "DiscountRuleType",
            description = "'Tạo mới loại quy tắc giảm giá: ' + #request.code",
            captureResponse = true
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Tạo mới loại quy tắc giảm giá")
    public ResponseEntity<ApiResponse<DiscountRuleTypeResponse>> create(
            @Valid @RequestBody DiscountRuleTypeCreateRequest request) {
        DiscountRuleTypeResponse response = discountRuleTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "DISCOUNT_RULE_TYPE_UPDATE",
            entityName = "DiscountRuleType",
            entityId = "#code",
            description = "'Cập nhật loại quy tắc giảm giá: ' + #code"
    )
    @PutMapping("/{code}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Cập nhật loại quy tắc giảm giá")
    public ResponseEntity<ApiResponse<DiscountRuleTypeResponse>> update(
            @PathVariable String code,
            @Valid @RequestBody DiscountRuleTypeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(discountRuleTypeService.update(code, request)));
    }

    @Auditable(
            actionType = "DISCOUNT_RULE_TYPE_DELETE",
            entityName = "DiscountRuleType",
            entityId = "#code",
            description = "'Xóa loại quy tắc giảm giá: ' + #code"
    )
    @DeleteMapping("/{code}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm loại quy tắc giảm giá")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String code) {
        discountRuleTypeService.delete(code);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
