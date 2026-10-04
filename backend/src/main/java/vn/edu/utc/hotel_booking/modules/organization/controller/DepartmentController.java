package vn.edu.utc.hotel_booking.modules.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.DepartmentResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.DepartmentService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Organization - Departments", description = "APIs quản lý phòng ban tiêu chuẩn")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tìm kiếm và phân trang phòng ban")
    public ApiResponse<PageResponse<DepartmentResponse>> filter(@RequestBody @Valid DepartmentSearchDto searchDto) {
        return ApiResponse.<PageResponse<DepartmentResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách phòng ban thành công")
                .result(departmentService.filter(searchDto))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Lấy chi tiết phòng ban theo ID")
    public ApiResponse<DepartmentResponse> getById(@PathVariable Short id) {
        return ApiResponse.<DepartmentResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin phòng ban thành công")
                .result(departmentService.getById(id))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới phòng ban (Chỉ Chain Admin)")
    public ApiResponse<DepartmentResponse> create(@RequestBody @Valid DepartmentCreateRequest request) {
        return ApiResponse.<DepartmentResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo mới phòng ban thành công")
                .result(departmentService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật thông tin phòng ban (Chỉ Chain Admin)")
    public ApiResponse<DepartmentResponse> update(@PathVariable Short id,
                                                  @RequestBody @Valid DepartmentUpdateRequest request) {
        return ApiResponse.<DepartmentResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật phòng ban thành công")
                .result(departmentService.update(id, request))
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm một hoặc nhiều phòng ban (Batch Delete)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Short> ids) {
        departmentService.deleteBatch(ids);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa danh sách phòng ban thành công")
                .build();
    }
}
