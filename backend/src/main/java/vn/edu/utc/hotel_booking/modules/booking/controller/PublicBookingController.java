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
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingService;

@RestController
@RequestMapping("/api/v1/public/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public - Bookings", description = "Public APIs đặt phòng trực tuyến và tra cứu đơn đặt phòng cho khách hàng")
public class PublicBookingController {

    BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo đơn đặt phòng trực tuyến dành cho khách hàng không đăng nhập")
    public ApiResponse<BookingResponse> createPublicBooking(@Valid @RequestBody BookingCreateRequest request) {
        return ApiResponse.success("Tạo đơn đặt phòng thành công", bookingService.createBooking(request));
    }

    @GetMapping("/{bookingNumber}")
    @Operation(summary = "Tra cứu thông tin đơn đặt phòng bằng mã booking")
    public ApiResponse<BookingResponse> getBookingByNumber(@PathVariable String bookingNumber) {
        return ApiResponse.success(bookingService.getByBookingNumber(bookingNumber));
    }
}
