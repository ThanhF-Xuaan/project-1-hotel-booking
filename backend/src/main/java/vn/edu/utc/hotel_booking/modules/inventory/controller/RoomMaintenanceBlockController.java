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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomMaintenanceBlockResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomMaintenanceBlockService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/maintenance-blocks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory - Room Maintenance Blocks", description = "APIs quản lý khóa phòng bảo trì (OOO/OOS)")
public class RoomMaintenanceBlockController {

    RoomMaintenanceBlockService service;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'HOUSEKEEPING', 'ENGINEERING')")
    @Operation(summary = "Tạo lệnh khóa phòng bảo trì (OOO hoặc OOS)")
    public ResponseEntity<ApiResponse<RoomMaintenanceBlockResponse>> create(
            @Valid @RequestBody RoomMaintenanceBlockCreateRequest request) {
        RoomMaintenanceBlockResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'HOUSEKEEPING', 'ENGINEERING')")
    @Operation(summary = "Cập nhật lệnh khóa phòng đang ACTIVE")
    public ResponseEntity<ApiResponse<RoomMaintenanceBlockResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RoomMaintenanceBlockUpdateRequest request) {
        RoomMaintenanceBlockResponse response = service.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'HOUSEKEEPING', 'ENGINEERING')")
    @Operation(summary = "Kết thúc lệnh khóa phòng (chuyển sang COMPLETED và giải phóng phòng)")
    public ResponseEntity<ApiResponse<RoomMaintenanceBlockResponse>> complete(
            @PathVariable Long id) {
        RoomMaintenanceBlockResponse response = service.complete(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'HOUSEKEEPING', 'ENGINEERING')")
    @Operation(summary = "Hủy lệnh khóa phòng (chuyển sang CANCELLED và giải phóng phòng)")
    public ResponseEntity<ApiResponse<RoomMaintenanceBlockResponse>> cancel(
            @PathVariable Long id) {
        RoomMaintenanceBlockResponse response = service.cancel(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'HOUSEKEEPING', 'ENGINEERING', 'RECEPTIONIST')")
    @Operation(summary = "Tìm kiếm và phân trang danh sách khóa phòng")
    public ResponseEntity<ApiResponse<PageResponse<RoomMaintenanceBlockResponse>>> filter(
            @RequestBody RoomMaintenanceBlockSearchDto searchDto) {
        PageResponse<RoomMaintenanceBlockResponse> response = service.filter(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'HOUSEKEEPING', 'ENGINEERING', 'RECEPTIONIST')")
    @Operation(summary = "Lấy chi tiết lệnh khóa phòng theo ID")
    public ResponseEntity<ApiResponse<RoomMaintenanceBlockResponse>> getById(
            @PathVariable Long id) {
        RoomMaintenanceBlockResponse response = service.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/room/{roomInstanceId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'HOUSEKEEPING', 'ENGINEERING', 'RECEPTIONIST')")
    @Operation(summary = "Lấy lịch sử các lệnh khóa phòng theo phòng vật lý")
    public ResponseEntity<ApiResponse<List<RoomMaintenanceBlockResponse>>> getByRoomInstanceId(
            @PathVariable Integer roomInstanceId) {
        List<RoomMaintenanceBlockResponse> response = service.getByRoomInstanceId(roomInstanceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
