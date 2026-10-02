package vn.edu.utc.hotel_booking.modules.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.GuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.GuestResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.GuestService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/guests")
@RequiredArgsConstructor
@Tag(name = "Identity - Guests", description = "APIs quản lý hồ sơ khách hàng (CRM Guest Profiles)")
public class GuestController {

    private final GuestService guestService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Tìm kiếm và phân trang hồ sơ khách hàng")
    public ApiResponse<PageResponse<GuestResponse>> filter(@RequestBody @Valid GuestSearchDto searchDto) {
        return ApiResponse.<PageResponse<GuestResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách khách hàng thành công")
                .result(guestService.filter(searchDto))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Lấy chi tiết hồ sơ khách hàng theo ID")
    public ApiResponse<GuestResponse> getById(@PathVariable Long id) {
        return ApiResponse.<GuestResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin khách hàng thành công")
                .result(guestService.getById(id))
                .build();
    }

    @GetMapping("/public/{publicId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Lấy chi tiết khách hàng theo UUID công khai")
    public ApiResponse<GuestResponse> getByPublicId(@PathVariable UUID publicId) {
        return ApiResponse.<GuestResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin khách hàng thành công")
                .result(guestService.getByPublicId(publicId))
                .build();
    }

    @GetMapping("/phone/{phone}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Tìm nhanh khách hàng theo số điện thoại (Hỗ trợ Lễ tân nhận khách)")
    public ApiResponse<GuestResponse> getByPhone(@PathVariable String phone) {
        return ApiResponse.<GuestResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Tìm thấy hồ sơ khách hàng")
                .result(guestService.getByPhone(phone))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Tạo mới hồ sơ khách hàng")
    public ApiResponse<GuestResponse> create(@RequestBody @Valid GuestCreateRequest request) {
        return ApiResponse.<GuestResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo mới hồ sơ khách hàng thành công")
                .result(guestService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Cập nhật thông tin hồ sơ khách hàng")
    public ApiResponse<GuestResponse> update(@PathVariable Long id,
                                             @RequestBody @Valid GuestUpdateRequest request) {
        return ApiResponse.<GuestResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật thông tin khách hàng thành công")
                .result(guestService.update(id, request))
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm một hoặc nhiều hồ sơ khách hàng (Batch Delete)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Long> ids) {
        guestService.deleteBatch(ids);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa danh sách hồ sơ khách hàng thành công")
                .build();
    }
}
