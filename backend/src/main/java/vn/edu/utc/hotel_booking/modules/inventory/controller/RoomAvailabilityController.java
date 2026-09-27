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
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/availability")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory - Availability & Locking", description = "APIs tra cứu và khóa giữ chỗ tồn phòng")
public class RoomAvailabilityController {

    RoomAvailabilityService roomAvailabilityService;

    @PostMapping("/query")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tra cứu tồn phòng theo khoảng thời gian")
    public ResponseEntity<ApiResponse<List<RoomAvailabilityResponse>>> getAvailability(
            @Valid @RequestBody RoomAvailabilitySearchDto searchDto) {
        List<RoomAvailabilityResponse> response = roomAvailabilityService.getAvailability(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/hold")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Khóa tạm thời phòng để thanh toán (TTL-based lock)")
    public ResponseEntity<ApiResponse<Void>> holdInventory(
            @Valid @RequestBody HoldInventoryRequest request) {
        roomAvailabilityService.holdInventory(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/release")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hủy khóa giữ phòng tạm thời")
    public ResponseEntity<ApiResponse<Void>> releaseInventory(
            @Valid @RequestBody ReleaseInventoryRequest request) {
        roomAvailabilityService.releaseInventory(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
