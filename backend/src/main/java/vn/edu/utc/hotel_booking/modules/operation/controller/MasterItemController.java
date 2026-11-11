package vn.edu.utc.hotel_booking.modules.operation.controller;

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
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MasterItemResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.MasterItemService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master-items")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "MasterItem Controller", description = "Quản lý danh mục gốc toàn chuỗi sản phẩm & dịch vụ")
public class MasterItemController {

    MasterItemService masterItemService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ROLE_CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới danh mục gốc chuỗi")
    public ApiResponse<MasterItemResponse> create(@Valid @RequestBody MasterItemCreateRequest request) {
        return ApiResponse.success("Tạo danh mục gốc chuỗi thành công", masterItemService.create(request));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ROLE_CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật danh mục gốc chuỗi")
    public ApiResponse<MasterItemResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody MasterItemUpdateRequest request
    ) {
        return ApiResponse.success("Cập nhật danh mục gốc chuỗi thành công", masterItemService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_CHAIN_EXECUTIVE', 'ROLE_REGION_MANAGER', 'ROLE_PROPERTY_MANAGER', 'ROLE_F_AND_B', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Xem chi tiết danh mục gốc chuỗi theo ID")
    public ApiResponse<MasterItemResponse> getById(@PathVariable Integer id) {
        return ApiResponse.success(masterItemService.getById(id));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_CHAIN_EXECUTIVE', 'ROLE_REGION_MANAGER', 'ROLE_PROPERTY_MANAGER', 'ROLE_F_AND_B', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Tìm kiếm và phân trang danh mục gốc chuỗi (Search DTO)")
    public ApiResponse<PageResponse<MasterItemResponse>> filter(@RequestBody MasterItemSearchDto searchDto) {
        return ApiResponse.success(masterItemService.filter(searchDto));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ROLE_CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm một hoặc nhiều danh mục gốc chuỗi (Unified Batch Delete)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Integer> ids) {
        masterItemService.deleteBatch(ids);
        return ApiResponse.success("Xóa danh mục gốc chuỗi đã chọn thành công", null);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_CHAIN_EXECUTIVE', 'ROLE_REGION_MANAGER', 'ROLE_PROPERTY_MANAGER', 'ROLE_F_AND_B', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Lấy toàn bộ danh mục gốc chuỗi đang hoạt động (cho dropdown)")
    public ApiResponse<List<MasterItemResponse>> getAllActive() {
        return ApiResponse.success(masterItemService.getAllActive());
    }
}
