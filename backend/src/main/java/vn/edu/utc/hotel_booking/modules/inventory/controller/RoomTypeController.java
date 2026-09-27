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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomTypeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/room-types")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory - Room Type", description = "APIs quản lý danh mục loại phòng toàn chuỗi")
public class RoomTypeController {

    RoomTypeService roomTypeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Tạo mới loại phòng danh mục")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> createRoomType(@Valid @RequestBody RoomTypeCreateRequest request) {
        RoomTypeResponse response = roomTypeService.createRoomType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Cập nhật loại phòng danh mục")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> updateRoomType(
            @PathVariable Short id,
            @Valid @RequestBody RoomTypeUpdateRequest request) {
        RoomTypeResponse response = roomTypeService.updateRoomType(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết loại phòng theo ID")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> getRoomTypeById(@PathVariable Short id) {
        RoomTypeResponse response = roomTypeService.getRoomTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tìm kiếm & lọc danh mục loại phòng")
    public ResponseEntity<ApiResponse<PageResponse<RoomTypeResponse>>> filterRoomTypes(@RequestBody RoomTypeSearchDto searchDto) {
        PageResponse<RoomTypeResponse> response = roomTypeService.filterRoomTypes(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt loại phòng (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteRoomTypes(@RequestBody List<Short> ids) {
        roomTypeService.deleteRoomTypes(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
