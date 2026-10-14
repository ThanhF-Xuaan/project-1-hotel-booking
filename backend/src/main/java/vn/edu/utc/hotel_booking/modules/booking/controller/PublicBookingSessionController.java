package vn.edu.utc.hotel_booking.modules.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancelSessionRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.InitiateDraftSessionRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.DraftSessionResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingSessionService;

@RestController
@RequestMapping("/api/v1/public/booking-sessions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public - Booking Sessions", description = "Public APIs quản lý vòng đời phiên giữ phòng (Draft Session 30m)")
public class PublicBookingSessionController {

    BookingSessionService bookingSessionService;

    @PostMapping("/draft")
    @Operation(summary = "Khởi tạo hoặc cập nhật phiên đặt phòng nháp (TTL 30 phút - tạm giữ phòng và lưu giỏ hàng)")
    public ApiResponse<DraftSessionResponse> initiateOrUpdateDraftSession(
            @Valid @RequestBody InitiateDraftSessionRequest request
    ) {
        DraftSessionResponse response = bookingSessionService.initiateOrUpdateDraftSession(request);
        return ApiResponse.<DraftSessionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Khởi tạo phiên đặt phòng thành công")
                .result(response)
                .build();
    }

    @GetMapping("/draft/{sessionId}")
    @Operation(summary = "Lấy thông tin và thời gian đếm ngược còn lại của phiên đặt phòng nháp")
    public ApiResponse<DraftSessionResponse> getDraftSession(@PathVariable String sessionId) {
        DraftSessionResponse response = bookingSessionService.getDraftSession(sessionId);
        return ApiResponse.<DraftSessionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin phiên đặt phòng thành công")
                .result(response)
                .build();
    }

    @PostMapping("/cancel")
    @Operation(summary = "Hủy phiên đặt phòng chủ động (Giải phóng kho phòng và xóa session Redis)")
    public ApiResponse<Void> cancelSession(
            @Valid @RequestBody CancelSessionRequest request
    ) {
        bookingSessionService.cancelSession(request);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Hủy phiên đặt phòng và giải phóng tồn kho thành công")
                .result(null)
                .build();
    }
}
