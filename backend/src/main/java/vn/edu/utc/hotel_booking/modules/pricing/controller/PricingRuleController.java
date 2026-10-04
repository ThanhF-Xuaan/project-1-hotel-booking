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
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PricingRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.PricingRuleService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/rules")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Rules Configuration", description = "APIs quản lý quy tắc điều chỉnh giá theo mùa, lễ tết, cuối tuần")
public class PricingRuleController {

    PricingRuleService pricingRuleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tạo mới quy tắc giá")
    public ResponseEntity<ApiResponse<PricingRuleResponse>> createPricingRule(
            @Valid @RequestBody PricingRuleCreateRequest request) {
        PricingRuleResponse response = pricingRuleService.createPricingRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật quy tắc giá")
    public ResponseEntity<ApiResponse<PricingRuleResponse>> updatePricingRule(
            @PathVariable Integer id,
            @Valid @RequestBody PricingRuleUpdateRequest request) {
        PricingRuleResponse response = pricingRuleService.updatePricingRule(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Xem chi tiết quy tắc giá theo ID")
    public ResponseEntity<ApiResponse<PricingRuleResponse>> getPricingRuleById(@PathVariable Integer id) {
        PricingRuleResponse response = pricingRuleService.getPricingRuleById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Tìm kiếm & lọc danh sách quy tắc giá")
    public ResponseEntity<ApiResponse<PageResponse<PricingRuleResponse>>> filterPricingRules(
            @RequestBody PricingRuleSearchDto searchDto) {
        PageResponse<PricingRuleResponse> response = pricingRuleService.filterPricingRules(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt quy tắc giá (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deletePricingRules(@RequestBody List<Integer> ids) {
        pricingRuleService.deletePricingRules(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
