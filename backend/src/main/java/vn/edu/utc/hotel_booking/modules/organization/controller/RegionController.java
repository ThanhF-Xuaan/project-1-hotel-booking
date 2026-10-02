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
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.RegionResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.RegionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
@Tag(name = "Organization - Regions", description = "APIs quản lý khu vực / vùng của chuỗi khách sạn")
public class RegionController {

    private final RegionService regionService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Tìm kiếm và phân trang khu vực")
    public ApiResponse<PageResponse<RegionResponse>> filter(@RequestBody @Valid RegionSearchDto searchDto) {
        return ApiResponse.<PageResponse<RegionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách khu vực thành công")
                .result(regionService.filter(searchDto))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Lấy chi tiết một khu vực theo ID")
    public ApiResponse<RegionResponse> getById(@PathVariable Short id) {
        return ApiResponse.<RegionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin khu vực thành công")
                .result(regionService.getById(id))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới khu vực (Chỉ Chain Admin)")
    public ApiResponse<RegionResponse> create(@RequestBody @Valid RegionCreateRequest request) {
        return ApiResponse.<RegionResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo mới khu vực thành công")
                .result(regionService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật thông tin khu vực (Chỉ Chain Admin)")
    public ApiResponse<RegionResponse> update(@PathVariable Short id,
                                              @RequestBody @Valid RegionUpdateRequest request) {
        return ApiResponse.<RegionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật khu vực thành công")
                .result(regionService.update(id, request))
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm một hoặc nhiều khu vực (Batch Delete)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Short> ids) {
        regionService.deleteBatch(ids);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa danh sách khu vực thành công")
                .build();
    }
}
