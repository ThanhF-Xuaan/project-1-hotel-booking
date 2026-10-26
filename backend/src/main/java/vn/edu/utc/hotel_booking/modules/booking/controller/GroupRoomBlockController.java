package vn.edu.utc.hotel_booking.modules.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupRoomBlockResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupRoomBlockService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group-bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Group Room Block & Cut-off Management", description = "Quản lý Quỹ phòng đoàn, Kiểm soát tồn phòng và Giải phóng Cut-off")
public class GroupRoomBlockController {

    GroupRoomBlockService groupRoomBlockService;

    @GetMapping("/contracts/{contractId}/room-blocks")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST', 'ROLE_SALE_STAFF')")
    @Operation(summary = "Xem danh sách Quỹ phòng đoàn (Room Blocks) và chi tiết phân bổ theo ngày của hợp đồng")
    public ApiResponse<List<GroupRoomBlockResponse>> getBlocksByContractId(@PathVariable Long contractId) {
        List<GroupRoomBlockResponse> responses = groupRoomBlockService.getBlocksByContractId(contractId);
        return ApiResponse.<List<GroupRoomBlockResponse>>builder()
                .result(responses)
                .build();
    }

    @PostMapping("/contracts/{contractId}/room-blocks/{blockId}/release-cutoff")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Giải phóng thủ công các phòng chưa gán khách (unpicked-up) cho một Room Block về Public Inventory")
    public ApiResponse<GroupRoomBlockResponse> releaseUnpickedUpRoomsManually(
            @PathVariable Long contractId,
            @PathVariable Long blockId
    ) {
        GroupRoomBlockResponse response = groupRoomBlockService.releaseUnpickedUpRoomsManually(contractId, blockId);
        return ApiResponse.<GroupRoomBlockResponse>builder()
                .message("Giải phóng phòng đoàn dôi dư thành công")
                .result(response)
                .build();
    }

    @PostMapping("/scheduler/trigger-cutoff")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER')")
    @Operation(summary = "Kích hoạt thủ công tiến trình quét và giải phóng phòng quá hạn Cut-off Date")
    public ApiResponse<Integer> triggerCutoffAutoRelease() {
        int releasedCount = groupRoomBlockService.processCutoffAutoRelease();
        return ApiResponse.<Integer>builder()
                .message("Đã quét và giải phóng thành công " + releasedCount + " phòng dôi dư về Public Inventory")
                .result(releasedCount)
                .build();
    }
}
