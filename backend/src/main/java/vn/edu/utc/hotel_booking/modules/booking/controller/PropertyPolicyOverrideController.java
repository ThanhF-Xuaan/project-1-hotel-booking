package vn.edu.utc.hotel_booking.modules.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.PropertyPolicyOverrideCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.PropertyPolicyOverrideSearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.PropertyPolicyOverrideUpdateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.PropertyPolicyOverrideResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.PropertyPolicyOverrideService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/property-policy-overrides")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Property Policy Override Controller", description = "Quản lý ghi đè và gán chính sách theo Khách sạn cơ sở và Hạng phòng")
public class PropertyPolicyOverrideController {

    PropertyPolicyOverrideService propertyPolicyOverrideService;

    @PostMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tìm kiếm và phân trang cấu hình ghi đè chính sách (POST Search DTO)")
    public ApiResponse<PageResponse<PropertyPolicyOverrideResponse>> filter(@RequestBody PropertyPolicyOverrideSearchDto searchDto) {
        return ApiResponse.success(propertyPolicyOverrideService.filter(searchDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy chi tiết cấu hình ghi đè theo ID")
    public ApiResponse<PropertyPolicyOverrideResponse> getById(@PathVariable Integer id) {
        return ApiResponse.success(propertyPolicyOverrideService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tạo mới cấu hình ghi đè chính sách cho khách sạn/hạng phòng")
    public ApiResponse<PropertyPolicyOverrideResponse> create(@Valid @RequestBody PropertyPolicyOverrideCreateRequest request) {
        return ApiResponse.success("Tạo cấu hình ghi đè chính sách thành công", propertyPolicyOverrideService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật cấu hình ghi đè chính sách theo ID")
    public ApiResponse<PropertyPolicyOverrideResponse> update(@PathVariable Integer id, @Valid @RequestBody PropertyPolicyOverrideUpdateRequest request) {
        return ApiResponse.success("Cập nhật cấu hình ghi đè chính sách thành công", propertyPolicyOverrideService.update(id, request));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa mềm một hoặc nhiều cấu hình ghi đè chính sách")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Integer> ids) {
        propertyPolicyOverrideService.delete(ids);
        return ApiResponse.success("Xóa cấu hình ghi đè chính sách thành công", null);
    }
}
