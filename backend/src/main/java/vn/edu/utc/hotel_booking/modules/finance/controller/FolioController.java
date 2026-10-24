package vn.edu.utc.hotel_booking.modules.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.FolioResponse;
import vn.edu.utc.hotel_booking.modules.finance.service.FolioService;

@RestController
@RequestMapping("/api/v1/folios")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Folio Controller", description = "Quản lý Sổ cái Folio chi phí lưu trú của khách thời gian thực (Guest Folio Running Ledger)")
public class FolioController {

    FolioService folioService;

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE', 'SALES_GROUP')")
    @Operation(summary = "Lấy Sổ cái Folio toàn bộ đơn đặt phòng theo Booking ID")
    public ApiResponse<FolioResponse> getFolioByBookingId(@PathVariable Long bookingId) {
        return ApiResponse.success("Lấy thông tin Folio thành công", folioService.getFolioByBookingId(bookingId));
    }

    @GetMapping("/booking-room/{bookingRoomId}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE', 'SALES_GROUP')")
    @Operation(summary = "Lấy Sổ cái Folio chi tiết của một phòng đặt theo BookingRoom ID")
    public ApiResponse<FolioResponse> getFolioByBookingRoomId(@PathVariable Long bookingRoomId) {
        return ApiResponse.success("Lấy thông tin Folio phòng thành công", folioService.getFolioByBookingRoomId(bookingRoomId));
    }

    @GetMapping("/number/{bookingNumber}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE', 'SALES_GROUP')")
    @Operation(summary = "Tra cứu Sổ cái Folio theo mã đơn đặt phòng (Booking Number)")
    public ApiResponse<FolioResponse> getFolioByBookingNumber(@PathVariable String bookingNumber) {
        return ApiResponse.success("Tra cứu Folio thành công", folioService.getFolioByBookingNumber(bookingNumber));
    }
}
