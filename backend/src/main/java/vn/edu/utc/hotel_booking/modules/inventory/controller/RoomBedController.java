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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomBedResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomBedService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/room-beds")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory - Room Bed", description = "APIs quản lý danh mục tiêu chuẩn giường toàn hệ thống")
public class RoomBedController {

    RoomBedService roomBedService;

    @PostMapping
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Tạo mới loại giường tiêu chuẩn")
    public ResponseEntity<ApiResponse<RoomBedResponse>> create(
            @Valid @RequestBody RoomBedCreateRequest request) {
        RoomBedResponse response = roomBedService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Cập nhật loại giường tiêu chuẩn")
    public ResponseEntity<ApiResponse<RoomBedResponse>> update(
            @PathVariable Short id,
            @Valid @RequestBody RoomBedUpdateRequest request) {
        RoomBedResponse response = roomBedService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết loại giường theo ID")
    public ResponseEntity<ApiResponse<RoomBedResponse>> getById(@PathVariable Short id) {
        RoomBedResponse response = roomBedService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tìm kiếm & lọc danh mục loại giường")
    public ResponseEntity<ApiResponse<PageResponse<RoomBedResponse>>> filter(
            @RequestBody RoomBedSearchDto searchDto) {
        PageResponse<RoomBedResponse> response = roomBedService.filter(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('CHAIN_ADMIN')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt loại giường (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(@RequestBody List<Short> ids) {
        roomBedService.deleteBatch(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

