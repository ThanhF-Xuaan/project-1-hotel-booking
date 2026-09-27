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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.HotelRoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/hotel-room-types")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory - Hotel Room Type", description = "APIs cấu hình loại phòng cho từng khách sạn")
public class HotelRoomTypeController {

    HotelRoomTypeService hotelRoomTypeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Thêm cấu hình loại phòng cho khách sạn")
    public ResponseEntity<ApiResponse<HotelRoomTypeResponse>> createHotelRoomType(
            @Valid @RequestBody HotelRoomTypeCreateRequest request) {
        HotelRoomTypeResponse response = hotelRoomTypeService.createHotelRoomType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật cấu hình loại phòng của khách sạn")
    public ResponseEntity<ApiResponse<HotelRoomTypeResponse>> updateHotelRoomType(
            @PathVariable Integer id,
            @Valid @RequestBody HotelRoomTypeUpdateRequest request) {
        HotelRoomTypeResponse response = hotelRoomTypeService.updateHotelRoomType(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết cấu hình loại phòng theo ID")
    public ResponseEntity<ApiResponse<HotelRoomTypeResponse>> getHotelRoomTypeById(@PathVariable Integer id) {
        HotelRoomTypeResponse response = hotelRoomTypeService.getHotelRoomTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tìm kiếm & lọc cấu hình loại phòng khách sạn")
    public ResponseEntity<ApiResponse<PageResponse<HotelRoomTypeResponse>>> filterHotelRoomTypes(
            @RequestBody HotelRoomTypeSearchDto searchDto) {
        PageResponse<HotelRoomTypeResponse> response = hotelRoomTypeService.filterHotelRoomTypes(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa đơn lẻ hoặc xóa hàng loạt cấu hình loại phòng (Soft Delete)")
    public ResponseEntity<ApiResponse<Void>> deleteHotelRoomTypes(@RequestBody List<Integer> ids) {
        hotelRoomTypeService.deleteHotelRoomTypes(ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
