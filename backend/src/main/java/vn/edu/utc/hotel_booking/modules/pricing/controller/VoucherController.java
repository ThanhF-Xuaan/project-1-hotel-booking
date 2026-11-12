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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.VoucherUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherResponse;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.VoucherUsageResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.VoucherService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/vouchers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Vouchers Management", description = "APIs quản lý mã giảm giá, voucher khuyến mại cho chuỗi và cơ sở")
public class VoucherController {

    VoucherService voucherService;

    @Auditable(
            actionType = "VOUCHER_CREATE",
            entityName = "Voucher",
            description = "'Tạo mới voucher mã ' + #request.voucherCode",
            captureResponse = true
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tạo mới voucher khuyến mại")
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(
            @Valid @RequestBody VoucherCreateRequest request) {
        VoucherResponse response = voucherService.createVoucher(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "VOUCHER_UPDATE",
            entityName = "Voucher",
            entityId = "#id.toString()",
            description = "'Cập nhật voucher ID ' + #id"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật voucher")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable Integer id,
            @Valid @RequestBody VoucherUpdateRequest request) {
        VoucherResponse response = voucherService.updateVoucher(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Xem chi tiết voucher theo ID")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherById(@PathVariable Integer id) {
        VoucherResponse response = voucherService.getVoucherById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/usages")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Xem lịch sử các lượt sử dụng của một voucher")
    public ResponseEntity<ApiResponse<List<VoucherUsageResponse>>> getVoucherUsages(@PathVariable Integer id) {
        List<VoucherUsageResponse> response = voucherService.getVoucherUsages(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Tìm kiếm & lọc danh sách vouchers phân trang")
    public ResponseEntity<ApiResponse<PageResponse<VoucherResponse>>> filterVouchers(
            @RequestBody VoucherSearchDto searchDto) {
        PageResponse<VoucherResponse> response = voucherService.filterVouchers(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "VOUCHER_DELETE",
            entityName = "Voucher",
            entityId = "#ids.toString()",
            description = "'Xóa các voucher ID: ' + #ids.toString()"
    )
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt voucher (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteVouchers(@RequestBody List<Integer> ids) {
        voucherService.deleteVouchers(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

