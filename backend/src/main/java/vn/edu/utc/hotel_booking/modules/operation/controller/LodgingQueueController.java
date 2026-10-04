package vn.edu.utc.hotel_booking.modules.operation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingQueueSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.LodgingQueueResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.LodgingQueueService;

@RestController
@RequestMapping("/api/v1/lodging-queues")
@RequiredArgsConstructor
@Tag(name = "Lodging Queue API", description = "Quản lý hàng chờ khai báo lưu trú BCA")
public class LodgingQueueController {

    private final LodgingQueueService lodgingQueueService;

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lọc danh sách hàng chờ khai báo lưu trú")
    public ApiResponse<PageResponse<LodgingQueueResponse>> search(@RequestBody LodgingQueueSearchDto request) {
        return ApiResponse.<PageResponse<LodgingQueueResponse>>builder()
                .result(lodgingQueueService.search(request))
                .build();
    }

    @PostMapping("/retry/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Kiểm tra lại tính hợp lệ của bản ghi trong hàng chờ", description = "Mã lỗi: 9071 (LODGING_QUEUE_NOT_FOUND)")
    public ApiResponse<LodgingQueueResponse> retryValidate(@PathVariable Long id) {
        return ApiResponse.<LodgingQueueResponse>builder()
                .result(lodgingQueueService.retryValidate(id))
                .build();
    }
}
