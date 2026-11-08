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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicySearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.HotelAgePolicyResponse;
import vn.edu.utc.hotel_booking.modules.pricing.service.HotelAgePolicyService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing/age-policies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pricing - Age Policies", description = "APIs quản lý chính sách phân loại độ tuổi (Toàn chuỗi & Override Cơ sở)")
public class HotelAgePolicyController {

    HotelAgePolicyService hotelAgePolicyService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Tìm kiếm & lọc danh sách chính sách độ tuổi")
    public ResponseEntity<ApiResponse<PageResponse<HotelAgePolicyResponse>>> filterAgePolicies(
            @RequestBody HotelAgePolicySearchDto searchDto) {
        PageResponse<HotelAgePolicyResponse> response = hotelAgePolicyService.filter(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Xem chi tiết chính sách độ tuổi theo ID")
    public ResponseEntity<ApiResponse<HotelAgePolicyResponse>> getAgePolicyById(@PathVariable Short id) {
        HotelAgePolicyResponse response = hotelAgePolicyService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/effective")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lấy danh sách chính sách hiệu lực của một cơ sở (kèm Fallback chuỗi)")
    public ResponseEntity<ApiResponse<List<HotelAgePolicyResponse>>> getEffectivePolicies(
            @RequestParam(required = false) Short hotelId) {
        List<HotelAgePolicyResponse> response = hotelAgePolicyService.getEffectivePoliciesForHotel(hotelId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "AGE_POLICY_CREATE",
            entityName = "HotelAgePolicy",
            description = "'Tạo mới chính sách độ tuổi cho ' + #request.guestType + ' (tuổi: ' + #request.minAge + '-' + #request.maxAge + ')'",
            captureResponse = true
    )
    @PostMapping
    @PreAuthorize("hasRole('CHAIN_ADMIN') or (hasRole('PROPERTY_MANAGER') and #request.hotelId != null)")
    @Operation(summary = "Tạo mới chính sách độ tuổi (Chuỗi hoặc Cơ sở)")
    public ResponseEntity<ApiResponse<HotelAgePolicyResponse>> createAgePolicy(
            @Valid @RequestBody HotelAgePolicyCreateRequest request) {
        HotelAgePolicyResponse response = hotelAgePolicyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "AGE_POLICY_UPDATE",
            entityName = "HotelAgePolicy",
            entityId = "#id.toString()",
            description = "'Cập nhật chính sách độ tuổi ID ' + #id"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật chính sách độ tuổi")
    public ResponseEntity<ApiResponse<HotelAgePolicyResponse>> updateAgePolicy(
            @PathVariable Short id,
            @Valid @RequestBody HotelAgePolicyUpdateRequest request) {
        HotelAgePolicyResponse response = hotelAgePolicyService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Auditable(
            actionType = "AGE_POLICY_DELETE",
            entityName = "HotelAgePolicy",
            entityId = "#ids.toString()",
            description = "'Xóa các chính sách độ tuổi: ' + #ids.toString()"
    )
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc hàng loạt chính sách độ tuổi (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteAgePolicies(@RequestBody List<Short> ids) {
        hotelAgePolicyService.deleteBatch(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
