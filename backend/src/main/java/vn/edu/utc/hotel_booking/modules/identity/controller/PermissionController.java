package vn.edu.utc.hotel_booking.modules.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionSearchDto;
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
}
