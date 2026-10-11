package vn.edu.utc.hotel_booking.modules.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.ReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilitySearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/availability")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public - Room Availability", description = "Public APIs tra cứu và tạm giữ phòng cho khách hàng trực tuyến")
public class PublicAvailabilityController {

    RoomAvailabilityService roomAvailabilityService;

    @PostMapping("/search")
    @Operation(summary = "Tra cứu tồn phòng theo ngày và loại phòng cho khách đặt phòng")
    public ResponseEntity<ApiResponse<List<RoomAvailabilityResponse>>> searchAvailability(
            @Valid @RequestBody RoomAvailabilitySearchDto searchDto) {
        List<RoomAvailabilityResponse> response = roomAvailabilityService.getAvailability(searchDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/hold")
    @Operation(summary = "Khóa tạm giữ phòng trong quá trình khách hàng thanh toán (TTL-based lock)")
    public ResponseEntity<ApiResponse<Void>> holdInventory(
            @Valid @RequestBody HoldInventoryRequest request) {
        roomAvailabilityService.holdInventory(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/release")
    @Operation(summary = "Hủy khóa tạm giữ phòng khi khách hủy giao dịch hoặc hết hạn")
    public ResponseEntity<ApiResponse<Void>> releaseInventory(
            @Valid @RequestBody ReleaseInventoryRequest request) {
        roomAvailabilityService.releaseInventory(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/batch-hold")
    @Operation(summary = "Khóa tạm giữ đồng thời nhiều hạng phòng trong giỏ hàng (Batch TTL-based lock)")
    public ResponseEntity<ApiResponse<Void>> batchHoldInventory(
            @Valid @RequestBody vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchHoldInventoryRequest request) {
        roomAvailabilityService.batchHoldInventory(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/batch-release")
    @Operation(summary = "Hủy khóa tạm giữ giỏ hàng nhiều hạng phòng khi khách hủy thanh toán")
    public ResponseEntity<ApiResponse<Void>> batchReleaseInventory(
            @Valid @RequestBody vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchReleaseInventoryRequest request) {
        roomAvailabilityService.batchReleaseInventory(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

