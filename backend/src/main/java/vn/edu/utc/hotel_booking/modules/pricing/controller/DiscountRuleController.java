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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.DiscountRuleService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/discount-rules")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Discount Rules Configuration", description = "APIs quản lý quy tắc giảm giá, khuyến mại cho hạng phòng và chiến dịch")
public class DiscountRuleController {

    DiscountRuleService discountRuleService;

    @Auditable(
            actionType = "DISCOUNT_RULE_CREATE",
            entityName = "DiscountRule",
            description = "'Tạo mới quy tắc giảm giá cho hạng phòng ' + #request.hotelRoomTypeId",
            captureResponse = true
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tạo mới quy tắc giảm giá")
    public ResponseEntity<ApiResponse<DiscountRuleResponse>> createDiscountRule(
            @Valid @RequestBody DiscountRuleCreateRequest request) {
        DiscountRuleResponse response = discountRuleService.createDiscountRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "DISCOUNT_RULE_UPDATE",
            entityName = "DiscountRule",
            entityId = "#id.toString()",
            description = "'Cập nhật quy tắc giảm giá ID ' + #id"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật quy tắc giảm giá")
    public ResponseEntity<ApiResponse<DiscountRuleResponse>> updateDiscountRule(
            @PathVariable Integer id,
            @Valid @RequestBody DiscountRuleUpdateRequest request) {
        DiscountRuleResponse response = discountRuleService.updateDiscountRule(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Xem chi tiết quy tắc giảm giá theo ID")
    public ResponseEntity<ApiResponse<DiscountRuleResponse>> getDiscountRuleById(@PathVariable Integer id) {
        DiscountRuleResponse response = discountRuleService.getDiscountRuleById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Tìm kiếm & lọc danh sách quy tắc giảm giá")
    public ResponseEntity<ApiResponse<PageResponse<DiscountRuleResponse>>> filterDiscountRules(
            @RequestBody DiscountRuleSearchDto searchDto) {
        PageResponse<DiscountRuleResponse> response = discountRuleService.filterDiscountRules(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "DISCOUNT_RULE_DELETE",
            entityName = "DiscountRule",
            entityId = "#ids.toString()",
            description = "'Xóa các quy tắc giảm giá: ' + #ids.toString()"
    )
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt quy tắc giảm giá (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteDiscountRules(@RequestBody List<Integer> ids) {
        discountRuleService.deleteDiscountRules(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
