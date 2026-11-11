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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomInclusionDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeInclusionRequest;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeInclusionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/hotel-room-types/{hotelRoomTypeId}/inclusions")
@RequiredArgsConstructor
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory - Room Type Inclusions", description = "APIs quản lý quà tặng và dịch vụ kèm theo hạng phòng")
public class HotelRoomTypeInclusionController {

    HotelRoomTypeInclusionService inclusionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách quà tặng/dịch vụ kèm theo của hạng phòng")
    public ResponseEntity<ApiResponse<List<RoomInclusionDto>>> getInclusions(
            @PathVariable Integer hotelRoomTypeId) {
        List<RoomInclusionDto> inclusions = inclusionService.getInclusionsByHotelRoomTypeId(hotelRoomTypeId);
        return ResponseEntity.ok(ApiResponse.success(inclusions));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Thêm quà tặng/dịch vụ kèm theo cho hạng phòng")
    public ResponseEntity<ApiResponse<RoomInclusionDto>> createInclusion(
            @PathVariable Integer hotelRoomTypeId,
            @Valid @RequestBody HotelRoomTypeInclusionRequest request) {
        RoomInclusionDto inclusion = inclusionService.createInclusion(hotelRoomTypeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(inclusion));
    }

    @PutMapping("/{inclusionId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Cập nhật cấu hình quà tặng kèm theo của hạng phòng")
    public ResponseEntity<ApiResponse<RoomInclusionDto>> updateInclusion(
            @PathVariable Integer hotelRoomTypeId,
            @PathVariable Long inclusionId,
            @Valid @RequestBody HotelRoomTypeInclusionRequest request) {
        RoomInclusionDto inclusion = inclusionService.updateInclusion(inclusionId, request);
        return ResponseEntity.ok(ApiResponse.success(inclusion));
    }

    @DeleteMapping("/{inclusionId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Xóa quà tặng kèm theo của hạng phòng")
    public ResponseEntity<ApiResponse<Void>> deleteInclusion(
            @PathVariable Integer hotelRoomTypeId,
            @PathVariable Long inclusionId) {
        inclusionService.deleteInclusion(inclusionId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa quà tặng kèm phòng thành công")
                .build());
    }
}

