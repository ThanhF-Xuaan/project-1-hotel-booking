package vn.edu.utc.hotel_booking.modules.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.*;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingChargeResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingService;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Booking Controller", description = "Quản lý vòng đời đơn đặt phòng, xếp phòng và tính phụ phí")
public class BookingController {

    BookingService bookingService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST', 'ROLE_CUSTOMER')")
    @Operation(summary = "Tạo mới đơn đặt phòng (Hỗ trợ cả online booking và walk-in)")
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody BookingCreateRequest request) {
        return ApiResponse.success("Tạo đơn đặt phòng thành công", bookingService.createBooking(request));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_REGION_MANAGER', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Tìm kiếm và phân trang đơn đặt phòng (POST Search DTO)")
    public ApiResponse<PageResponse<BookingResponse>> filter(@RequestBody BookingSearchDto searchDto) {
        return ApiResponse.success(bookingService.filter(searchDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_REGION_MANAGER', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Lấy chi tiết đơn đặt phòng theo ID")
    public ApiResponse<BookingResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(bookingService.getById(id));
    }

    @GetMapping("/number/{bookingNumber}")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_REGION_MANAGER', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST', 'ROLE_CUSTOMER')")
    @Operation(summary = "Tra cứu đơn đặt phòng theo mã booking")
    public ApiResponse<BookingResponse> getByBookingNumber(@PathVariable String bookingNumber) {
        return ApiResponse.success(bookingService.getByBookingNumber(bookingNumber));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Cập nhật trạng thái đơn đặt phòng (Hủy / No-show)")
    public ApiResponse<BookingResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusUpdateRequest request
    ) {
        return ApiResponse.success("Cập nhật trạng thái đơn thành công", bookingService.updateStatus(id, request.getStatus()));
    }

    @PostMapping("/rooms/{bookingRoomId}/assign")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Xếp phòng vật lý cho phòng đặt (Physical Room Assignment)")
    public ApiResponse<BookingResponse> assignRoom(
            @PathVariable Long bookingRoomId,
            @Valid @RequestBody RoomAssignmentRequest request
    ) {
        return ApiResponse.success("Xếp phòng thành công",
                bookingService.assignRoom(bookingRoomId, request.getRoomInstanceId()));
    }

    @PostMapping("/rooms/{bookingRoomId}/charges")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Thêm phụ phí (Early check-in, late check-out, minibar, hỏng hóc)")
    public ApiResponse<BookingChargeResponse> addCharge(
            @PathVariable Long bookingRoomId,
            @Valid @RequestBody BookingChargeCreateRequest request
    ) {
        return ApiResponse.success("Thêm phụ phí thành công", bookingService.addCharge(bookingRoomId, request));
    }

    @PostMapping("/rooms/{bookingRoomId}/check-in")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Thực hiện Check-in nhận phòng")
    public ApiResponse<BookingResponse> checkIn(@PathVariable Long bookingRoomId) {
        return ApiResponse.success("Check-in nhận phòng thành công", bookingService.checkIn(bookingRoomId));
    }

    @PostMapping("/rooms/{bookingRoomId}/check-out")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Thực hiện Check-out trả phòng")
    public ApiResponse<BookingResponse> checkOut(@PathVariable Long bookingRoomId) {
        return ApiResponse.success("Check-out trả phòng thành công", bookingService.checkOut(bookingRoomId));
    }
}
