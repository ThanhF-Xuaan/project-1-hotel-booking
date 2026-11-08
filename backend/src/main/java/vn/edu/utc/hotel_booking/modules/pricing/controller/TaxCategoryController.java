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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategoryCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategorySearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategoryUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.TaxCategoryResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.TaxCategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/tax-categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Tax Categories", description = "APIs quản lý danh mục nhóm thuế (Lưu trú, F&B, Dịch vụ)")
public class TaxCategoryController {

    TaxCategoryService taxCategoryService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'FINANCE', 'RECEPTIONIST')")
    @Operation(summary = "Tìm kiếm & lọc danh sách nhóm thuế")
    public ResponseEntity<ApiResponse<PageResponse<TaxCategoryResponse>>> filterTaxCategories(
            @RequestBody TaxCategorySearchDto searchDto) {
        PageResponse<TaxCategoryResponse> response = taxCategoryService.filter(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'FINANCE', 'RECEPTIONIST')")
    @Operation(summary = "Xem chi tiết nhóm thuế theo ID")
    public ResponseEntity<ApiResponse<TaxCategoryResponse>> getTaxCategoryById(@PathVariable Integer id) {
        TaxCategoryResponse response = taxCategoryService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "TAX_CATEGORY_CREATE",
            entityName = "TaxCategory",
            description = "'Tạo mới nhóm thuế: ' + #request.categoryCode + ' - ' + #request.categoryName",
            captureResponse = true
    )
    @PostMapping
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới nhóm thuế (Chỉ Chain Admin)")
    public ResponseEntity<ApiResponse<TaxCategoryResponse>> createTaxCategory(
            @Valid @RequestBody TaxCategoryCreateRequest request) {
        TaxCategoryResponse response = taxCategoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "TAX_CATEGORY_UPDATE",
            entityName = "TaxCategory",
            entityId = "#id.toString()",
            description = "'Cập nhật nhóm thuế ID ' + #id + ': ' + #request.categoryName"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật nhóm thuế (Chỉ Chain Admin)")
    public ResponseEntity<ApiResponse<TaxCategoryResponse>> updateTaxCategory(
            @PathVariable Integer id,
            @Valid @RequestBody TaxCategoryUpdateRequest request) {
        TaxCategoryResponse response = taxCategoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "TAX_CATEGORY_DELETE",
            entityName = "TaxCategory",
            entityId = "#ids.toString()",
            description = "'Xóa các nhóm thuế: ' + #ids.toString()"
    )
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt nhóm thuế (Soft Delete, Chỉ Chain Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteTaxCategories(@RequestBody List<Integer> ids) {
        taxCategoryService.deleteBatch(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

