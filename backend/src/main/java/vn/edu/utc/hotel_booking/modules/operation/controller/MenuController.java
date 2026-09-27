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
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Menu Controller", description = "Quản lý thực đơn F&B, minibar, giặt là, spa")
public class MenuController {

    MenuService menuService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER')")
    @Operation(summary = "Tạo mới món ăn hoặc dịch vụ trong menu")
    public ApiResponse<MenuResponse> create(@Valid @RequestBody MenuCreateRequest request) {
        return ApiResponse.success("Tạo món/dịch vụ thành công", menuService.create(request));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật món ăn hoặc dịch vụ")
    public ApiResponse<MenuResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody MenuUpdateRequest request
    ) {
        return ApiResponse.success("Cập nhật món/dịch vụ thành công", menuService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_REGION_MANAGER', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Xem chi tiết món/dịch vụ theo ID")
    public ApiResponse<MenuResponse> getById(@PathVariable Integer id) {
        return ApiResponse.success(menuService.getById(id));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_REGION_MANAGER', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST', 'ROLE_CUSTOMER')")
    @Operation(summary = "Tìm kiếm và phân trang món/dịch vụ (POST Search DTO)")
    public ApiResponse<PageResponse<MenuResponse>> filter(@RequestBody MenuSearchDto searchDto) {
        return ApiResponse.success(menuService.filter(searchDto));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER')")
    @Operation(summary = "Xóa mềm một hoặc nhiều món/dịch vụ (Unified Batch Delete)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Integer> ids) {
        menuService.deleteBatch(ids);
        return ApiResponse.success("Xóa các món/dịch vụ đã chọn thành công", null);
    }
}
