package vn.edu.utc.hotel_booking.modules.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomFeatureResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomFeatureService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/room-features")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory - Room Feature", description = "APIs quản lý danh mục tiện ích phòng toàn hệ thống")
public class RoomFeatureController {

    RoomFeatureService roomFeatureService;

    @PostMapping
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới tiện ích phòng")
    public ResponseEntity<ApiResponse<RoomFeatureResponse>> create(
            @Valid @RequestBody RoomFeatureCreateRequest request) {
        RoomFeatureResponse response = roomFeatureService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật tiện ích phòng")
    public ResponseEntity<ApiResponse<RoomFeatureResponse>> update(
            @PathVariable Short id,
            @Valid @RequestBody RoomFeatureUpdateRequest request) {
        RoomFeatureResponse response = roomFeatureService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết tiện ích phòng theo ID")
    public ResponseEntity<ApiResponse<RoomFeatureResponse>> getById(@PathVariable Short id) {
        RoomFeatureResponse response = roomFeatureService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tìm kiếm & lọc danh mục tiện ích phòng")
    public ResponseEntity<ApiResponse<PageResponse<RoomFeatureResponse>>> filter(
            @RequestBody RoomFeatureSearchDto searchDto) {
        PageResponse<RoomFeatureResponse> response = roomFeatureService.filter(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt tiện ích phòng (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(@RequestBody List<Short> ids) {
        roomFeatureService.deleteBatch(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

