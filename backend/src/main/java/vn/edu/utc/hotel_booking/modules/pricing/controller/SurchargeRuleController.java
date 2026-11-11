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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SurchargeRuleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.SurchargeRuleResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.SurchargeRuleService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/surcharge-rules")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Surcharge Rules", description = "APIs quản lý quy tắc phụ thu phòng (Thêm người, Kê thêm giường, Nhận sớm, Trả muộn)")
public class SurchargeRuleController {

    SurchargeRuleService surchargeRuleService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Tìm kiếm & lọc danh sách quy tắc phụ thu")
    public ResponseEntity<ApiResponse<PageResponse<SurchargeRuleResponse>>> filterSurchargeRules(
            @RequestBody SurchargeRuleSearchDto searchDto) {
        PageResponse<SurchargeRuleResponse> response = surchargeRuleService.filter(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'CHAIN_EXECUTIVE', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Xem chi tiết quy tắc phụ thu theo ID")
    public ResponseEntity<ApiResponse<SurchargeRuleResponse>> getSurchargeRuleById(@PathVariable Integer id) {
        SurchargeRuleResponse response = surchargeRuleService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "SURCHARGE_RULE_CREATE",
            entityName = "SurchargeRule",
            description = "'Tạo mới quy tắc phụ thu: ' + #request.ruleType + ' cho loại phòng ID: ' + #request.hotelRoomTypeId",
            captureResponse = true
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tạo mới quy tắc phụ thu")
    public ResponseEntity<ApiResponse<SurchargeRuleResponse>> createSurchargeRule(
            @Valid @RequestBody SurchargeRuleCreateRequest request) {
        SurchargeRuleResponse response = surchargeRuleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "SURCHARGE_RULE_UPDATE",
            entityName = "SurchargeRule",
            entityId = "#id.toString()",
            description = "'Cập nhật quy tắc phụ thu ID ' + #id"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật quy tắc phụ thu")
    public ResponseEntity<ApiResponse<SurchargeRuleResponse>> updateSurchargeRule(
            @PathVariable Integer id,
            @Valid @RequestBody SurchargeRuleUpdateRequest request) {
        SurchargeRuleResponse response = surchargeRuleService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "SURCHARGE_RULE_DELETE",
            entityName = "SurchargeRule",
            entityId = "#ids.toString()",
            description = "'Xóa các quy tắc phụ thu ID: ' + #ids.toString()"
    )
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc hàng loạt quy tắc phụ thu (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteSurchargeRules(@RequestBody List<Integer> ids) {
        surchargeRuleService.deleteBatch(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
