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
import vn.edu.utc.hotel_booking.modules.booking.dto.request.NoShowPolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.NoShowPolicySearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.NoShowPolicyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.NoShowPolicyResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.NoShowPolicyService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/no-show-policies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "No-Show Policy Controller", description = "Quản lý chính sách phạt vắng mặt (No-Show) cấp chuỗi")
public class NoShowPolicyController {

    NoShowPolicyService noShowPolicyService;

    @PostMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tìm kiếm và phân trang chính sách No-Show (POST Search DTO)")
    public ApiResponse<PageResponse<NoShowPolicyResponse>> filter(@RequestBody NoShowPolicySearchDto searchDto) {
        return ApiResponse.success(noShowPolicyService.filter(searchDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy chi tiết chính sách No-Show theo ID")
    public ApiResponse<NoShowPolicyResponse> getById(@PathVariable Integer id) {
        return ApiResponse.success(noShowPolicyService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới chính sách No-Show cấp chuỗi (Chỉ Chain Admin)")
    public ApiResponse<NoShowPolicyResponse> create(@Valid @RequestBody NoShowPolicyCreateRequest request) {
        return ApiResponse.success("Tạo chính sách No-Show thành công", noShowPolicyService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật chính sách No-Show cấp chuỗi (Chỉ Chain Admin)")
    public ApiResponse<NoShowPolicyResponse> update(@PathVariable Integer id, @Valid @RequestBody NoShowPolicyUpdateRequest request) {
        return ApiResponse.success("Cập nhật chính sách No-Show thành công", noShowPolicyService.update(id, request));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm một hoặc nhiều chính sách No-Show (Chỉ Chain Admin, chặn nếu đã có đơn sử dụng)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Integer> ids) {
        noShowPolicyService.delete(ids);
        return ApiResponse.success("Xóa chính sách No-Show thành công", null);
    }
}
