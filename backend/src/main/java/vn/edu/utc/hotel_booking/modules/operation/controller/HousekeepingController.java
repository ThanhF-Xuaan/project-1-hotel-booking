package vn.edu.utc.hotel_booking.modules.operation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.RoomStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.RoomHousekeepingStatusResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.HousekeepingService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/housekeeping")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Housekeeping Controller", description = "Quản lý trạng thái buồng phòng, dọn dẹp và bảo trì")
public class HousekeepingController {

    HousekeepingService housekeepingService;

    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING')")
    @Operation(summary = "Lấy sơ đồ và danh sách trạng thái buồng phòng theo khách sạn")
    public ApiResponse<List<RoomHousekeepingStatusResponse>> getRoomsByHotel(
            @RequestParam Short hotelId,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(housekeepingService.getRoomsByHotel(hotelId, status));
    }

    @PutMapping("/rooms/{roomInstanceId}/status")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'HOUSEKEEPING')")
    @Operation(summary = "Cập nhật trạng thái buồng phòng (READY, CLEANING, DIRTY, MAINTENANCE)")
    public ApiResponse<RoomHousekeepingStatusResponse> updateRoomStatus(
            @PathVariable Integer roomInstanceId,
            @Valid @RequestBody RoomStatusUpdateRequest request
    ) {
        return ApiResponse.success("Cập nhật trạng thái phòng thành công", housekeepingService.updateRoomStatus(roomInstanceId, request));
    }
}
