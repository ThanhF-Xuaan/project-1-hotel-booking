package vn.edu.utc.hotel_booking.modules.operation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.StayGuestResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.StayGuestService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stay-guests")
@RequiredArgsConstructor
@Tag(name = "Stay Guest API", description = "Quản lý thông tin khách lưu trú thực tế tại phòng")
public class StayGuestController {

    private final StayGuestService stayGuestService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lọc danh sách khách lưu trú")
    public ApiResponse<PageResponse<StayGuestResponse>> search(@RequestBody StayGuestSearchDto request) {
        return ApiResponse.<PageResponse<StayGuestResponse>>builder()
                .result(stayGuestService.search(request))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lấy chi tiết thông tin khách lưu trú", description = "Mã lỗi: 9061 (STAY_GUEST_NOT_FOUND)")
    public ApiResponse<StayGuestResponse> getById(@PathVariable Long id) {
        return ApiResponse.<StayGuestResponse>builder()
                .result(stayGuestService.getById(id))
                .build();
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Ghi nhận khách lưu trú (Check-in)", description = "Tự động phân loại hàng chờ khai báo BCA: PENDING nếu đủ trường, ERROR nếu thiếu trường")
    public ApiResponse<StayGuestResponse> create(@Valid @RequestBody StayGuestCreateRequest request) {
        return ApiResponse.<StayGuestResponse>builder()
                .result(stayGuestService.createOrCheckIn(request))
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Cập nhật thông tin khách lưu trú", description = "Mã lỗi: 9061 (STAY_GUEST_NOT_FOUND)")
    public ApiResponse<StayGuestResponse> update(@PathVariable Long id, @Valid @RequestBody StayGuestUpdateRequest request) {
        return ApiResponse.<StayGuestResponse>builder()
                .result(stayGuestService.update(id, request))
                .build();
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa mềm danh sách khách lưu trú và hủy hàng chờ", description = "Chuyển is_deleted = true và queue status = CANCELLED")
    public ApiResponse<Void> delete(@RequestBody List<Long> ids) {
        stayGuestService.delete(ids);
        return ApiResponse.<Void>builder().build();
    }
}
