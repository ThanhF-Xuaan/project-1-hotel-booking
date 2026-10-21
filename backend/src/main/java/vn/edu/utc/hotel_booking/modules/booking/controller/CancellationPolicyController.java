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
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancellationPolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancellationPolicySearchDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancellationPolicyUpdateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CancellationPolicyResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.CancellationPolicyService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cancellation-policies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Cancellation Policy Controller", description = "Quản lý chính sách hủy phòng cấp chuỗi và thuộc tính")
public class CancellationPolicyController {

    CancellationPolicyService cancellationPolicyService;

    @PostMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tìm kiếm và phân trang chính sách hủy phòng (POST Search DTO)")
    public ApiResponse<PageResponse<CancellationPolicyResponse>> filter(@RequestBody CancellationPolicySearchDto searchDto) {
        return ApiResponse.success(cancellationPolicyService.filter(searchDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy chi tiết chính sách hủy phòng theo ID")
    public ApiResponse<CancellationPolicyResponse> getById(@PathVariable Integer id) {
        return ApiResponse.success(cancellationPolicyService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới chính sách hủy phòng cấp chuỗi (Chỉ Chain Admin)")
    public ApiResponse<CancellationPolicyResponse> create(@Valid @RequestBody CancellationPolicyCreateRequest request) {
        return ApiResponse.success("Tạo chính sách hủy thành công", cancellationPolicyService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật chính sách hủy phòng cấp chuỗi (Chỉ Chain Admin)")
    public ApiResponse<CancellationPolicyResponse> update(@PathVariable Integer id, @Valid @RequestBody CancellationPolicyUpdateRequest request) {
        return ApiResponse.success("Cập nhật chính sách hủy thành công", cancellationPolicyService.update(id, request));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa mềm một hoặc nhiều chính sách hủy (Chỉ Chain Admin, chặn nếu đã có đơn sử dụng)")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Integer> ids) {
        cancellationPolicyService.delete(ids);
        return ApiResponse.success("Xóa chính sách hủy thành công", null);
    }
}
