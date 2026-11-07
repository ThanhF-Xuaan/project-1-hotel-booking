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
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.PermissionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Identity - Permissions", description = "APIs danh mục quyền hạn hệ thống")
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Tìm kiếm và phân trang danh mục quyền hạn")
    public ApiResponse<PageResponse<PermissionResponse>> filter(@RequestBody(required = false) PermissionSearchDto searchDto) {
        PermissionSearchDto criteria = searchDto != null ? searchDto : new PermissionSearchDto();
        return ApiResponse.<PageResponse<PermissionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách quyền hạn thành công")
                .result(permissionService.filter(criteria))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Lấy toàn bộ danh mục quyền hạn (Permissions) trong hệ thống")
    public ApiResponse<List<PermissionResponse>> getAllPermissions() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách quyền hạn thành công")
                .result(permissionService.getAllPermissions())
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới một quyền hạn trong hệ thống")
    public ApiResponse<PermissionResponse> create(@RequestBody @Valid PermissionCreateRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo mới quyền hạn thành công")
                .result(permissionService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật thông tin quyền hạn")
    public ApiResponse<PermissionResponse> update(@PathVariable Short id, @RequestBody @Valid PermissionUpdateRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật quyền hạn thành công")
                .result(permissionService.update(id, request))
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm danh sách quyền hạn")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Short> ids) {
        permissionService.deleteBatch(ids);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa quyền hạn thành công")
                .build();
    }

    @GetMapping("/resources")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Lấy danh sách các phân hệ tài nguyên hiện có")
    public ApiResponse<List<String>> getResources() {
        return ApiResponse.<List<String>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách tài nguyên thành công")
                .result(permissionService.getAllResources())
                .build();
    }

    @GetMapping("/actions")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Lấy danh sách các hành động hiện có")
    public ApiResponse<List<String>> getActions() {
        return ApiResponse.<List<String>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách hành động thành công")
                .result(permissionService.getAllActions())
                .build();
    }
}
