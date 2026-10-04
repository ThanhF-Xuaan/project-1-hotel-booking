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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomInstanceResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory - Physical Rooms", description = "APIs quản lý danh sách phòng vật lý tại cơ sở")
public class RoomInstanceController {

    RoomInstanceService roomInstanceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Tạo phòng vật lý mới")
    public ResponseEntity<ApiResponse<RoomInstanceResponse>> createRoomInstance(
            @Valid @RequestBody RoomInstanceCreateRequest request) {
        RoomInstanceResponse response = roomInstanceService.createRoomInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật thông tin phòng vật lý")
    public ResponseEntity<ApiResponse<RoomInstanceResponse>> updateRoomInstance(
            @PathVariable Integer id,
            @Valid @RequestBody RoomInstanceUpdateRequest request) {
        RoomInstanceResponse response = roomInstanceService.updateRoomInstance(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING', 'ENGINEERING')")
    @Operation(summary = "Cập nhật nhanh trạng thái phòng (READY, OCCUPIED, CLEANING, MAINTENANCE)")
    public ResponseEntity<ApiResponse<RoomInstanceResponse>> updateRoomStatus(
            @PathVariable Integer id,
            @RequestParam String status) {
        RoomInstanceResponse response = roomInstanceService.updateRoomStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết phòng theo ID")
    public ResponseEntity<ApiResponse<RoomInstanceResponse>> getRoomInstanceById(@PathVariable Integer id) {
        RoomInstanceResponse response = roomInstanceService.getRoomInstanceById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tìm kiếm & lọc danh sách phòng vật lý")
    public ResponseEntity<ApiResponse<PageResponse<RoomInstanceResponse>>> filterRoomInstances(
            @RequestBody RoomInstanceSearchDto searchDto) {
        PageResponse<RoomInstanceResponse> response = roomInstanceService.filterRoomInstances(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt phòng vật lý (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteRoomInstances(@RequestBody List<Integer> ids) {
        roomInstanceService.deleteRoomInstances(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
