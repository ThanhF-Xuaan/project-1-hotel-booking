package vn.edu.utc.hotel_booking.modules.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.ReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilitySearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilityTriggerRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityTriggerResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/availability")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory - Availability & Locking", description = "APIs tra cứu và khóa giữ chỗ tồn phòng")
public class RoomAvailabilityController {

    RoomAvailabilityService roomAvailabilityService;
    RoomAvailabilityGenerationService roomAvailabilityGenerationService;

    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Tra cứu tồn phòng theo khoảng thời gian")
    public ResponseEntity<ApiResponse<List<RoomAvailabilityResponse>>> getAvailability(
            @Valid @RequestBody RoomAvailabilitySearchDto searchDto) {
        List<RoomAvailabilityResponse> response = roomAvailabilityService.getAvailability(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/hold")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Khóa tạm thời phòng để thanh toán (TTL-based lock)")
    public ResponseEntity<ApiResponse<Void>> holdInventory(
            @Valid @RequestBody HoldInventoryRequest request) {
        roomAvailabilityService.holdInventory(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/release")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Hủy khóa giữ phòng tạm thời")
    public ResponseEntity<ApiResponse<Void>> releaseInventory(
            @Valid @RequestBody ReleaseInventoryRequest request) {
        roomAvailabilityService.releaseInventory(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/trigger/room-type/{hotelRoomTypeId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Kích hoạt sinh sẵn tồn kho cho một hạng phòng cụ thể")
    public ResponseEntity<ApiResponse<RoomAvailabilityTriggerResponse>> triggerRoomType(
            @PathVariable Integer hotelRoomTypeId,
            @Valid @RequestBody(required = false) RoomAvailabilityTriggerRequest request
    ) {
        LocalDate start = (request != null && request.getStartDate() != null) ? request.getStartDate() : LocalDate.now();
        int days = (request != null && request.getHorizonDays() != null && request.getHorizonDays() > 0) ? request.getHorizonDays() : 365;

        int created = roomAvailabilityGenerationService.generateForRoomType(hotelRoomTypeId, start, days);

        RoomAvailabilityTriggerResponse response = RoomAvailabilityTriggerResponse.builder()
                .scope("ROOM_TYPE")
                .targetId(hotelRoomTypeId)
                .startDate(start)
                .horizonDays(days)
                .recordsGenerated(created)
                .status("SUCCESS")
                .message(String.format("Đã sinh sẵn thành công %d bản ghi tồn kho cho hạng phòng ID: %d", created, hotelRoomTypeId))
                .triggeredAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/trigger/hotel/{hotelId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Kích hoạt cuộn/sinh sẵn tồn kho cho tất cả hạng phòng của một khách sạn cơ sở")
    public ResponseEntity<ApiResponse<RoomAvailabilityTriggerResponse>> triggerHotel(
            @PathVariable Short hotelId,
            @Valid @RequestBody(required = false) RoomAvailabilityTriggerRequest request
    ) {
        int days = (request != null && request.getHorizonDays() != null && request.getHorizonDays() > 0) ? request.getHorizonDays() : 365;
        LocalDate start = (request != null && request.getStartDate() != null) ? request.getStartDate() : LocalDate.now();

        int created = roomAvailabilityGenerationService.rollHorizonForHotel(hotelId, days);

        RoomAvailabilityTriggerResponse response = RoomAvailabilityTriggerResponse.builder()
                .scope("HOTEL")
                .targetId(hotelId.intValue())
                .startDate(start)
                .horizonDays(days)
                .recordsGenerated(created)
                .status("SUCCESS")
                .message(String.format("Đã cuộn/sinh sẵn thành công %d bản ghi tồn kho cho khách sạn ID: %d", created, hotelId))
                .triggeredAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/trigger/chain")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN')")
    @Operation(summary = "Kích hoạt cuộn/sinh sẵn tồn kho toàn bộ chuỗi khách sạn")
    public ResponseEntity<ApiResponse<RoomAvailabilityTriggerResponse>> triggerChain(
            @Valid @RequestBody(required = false) RoomAvailabilityTriggerRequest request
    ) {
        int days = (request != null && request.getHorizonDays() != null && request.getHorizonDays() > 0) ? request.getHorizonDays() : 365;
        LocalDate start = (request != null && request.getStartDate() != null) ? request.getStartDate() : LocalDate.now();

        int created = roomAvailabilityGenerationService.rollHorizonForAllActiveRoomTypes(days);

        RoomAvailabilityTriggerResponse response = RoomAvailabilityTriggerResponse.builder()
                .scope("CHAIN")
                .targetId(null)
                .startDate(start)
                .horizonDays(days)
                .recordsGenerated(created)
                .status("SUCCESS")
                .message(String.format("Đã cuộn/sinh sẵn thành công %d bản ghi tồn kho cho toàn bộ chuỗi khách sạn", created))
                .triggeredAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
