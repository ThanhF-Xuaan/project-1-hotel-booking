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
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.RoleResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Identity - Roles & Permissions", description = "APIs quản lý vai trò và phân quyền hệ thống")
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Tìm kiếm và phân trang vai trò")
    public ApiResponse<PageResponse<RoleResponse>> filter(@RequestBody @Valid RoleSearchDto searchDto) {
        return ApiResponse.<PageResponse<RoleResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách vai trò thành công")
                .result(roleService.filter(searchDto))
                .build();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Lấy toàn bộ danh mục quyền hạn (Permissions) trong hệ thống")
    public ApiResponse<List<PermissionResponse>> getAllPermissions() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách quyền hạn thành công")
                .result(roleService.getAllPermissions())
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Lấy chi tiết vai trò theo ID")
    public ApiResponse<RoleResponse> getById(@PathVariable Short id) {
        return ApiResponse.<RoleResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin vai trò thành công")
                .result(roleService.getById(id))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới vai trò và gán quyền (Chỉ Chain Admin)")
    public ApiResponse<RoleResponse> create(@RequestBody @Valid RoleCreateRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo mới vai trò thành công")
                .result(roleService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật thông tin vai trò (Chỉ Chain Admin)")
    public ApiResponse<RoleResponse> update(@PathVariable Short id,
                                           @RequestBody @Valid RoleUpdateRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật vai trò thành công")
                .result(roleService.update(id, request))
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm một hoặc nhiều vai trò (Batch Delete)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Short> ids) {
        roleService.deleteBatch(ids);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa danh sách vai trò thành công")
                .build();
    }
}
