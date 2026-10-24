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
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomSwapResponse;
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
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Tạo mới đơn đặt phòng (Hỗ trợ cả nội bộ, sales đoàn và walk-in)")
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody BookingCreateRequest request) {
        return ApiResponse.success("Tạo đơn đặt phòng thành công", bookingService.createBooking(request));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Tìm kiếm và phân trang đơn đặt phòng (POST Search DTO)")
    public ApiResponse<PageResponse<BookingResponse>> filter(@RequestBody BookingSearchDto searchDto) {
        return ApiResponse.success(bookingService.filter(searchDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Lấy chi tiết đơn đặt phòng theo ID")
    public ApiResponse<BookingResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(bookingService.getById(id));
    }

    @GetMapping("/number/{bookingNumber}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP', 'FINANCE')")
    @Operation(summary = "Tra cứu đơn đặt phòng theo mã booking")
    public ApiResponse<BookingResponse> getByBookingNumber(@PathVariable String bookingNumber) {
        return ApiResponse.success(bookingService.getByBookingNumber(bookingNumber));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Cập nhật trạng thái đơn đặt phòng (Hủy / No-show)")
    public ApiResponse<BookingResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusUpdateRequest request
    ) {
        return ApiResponse.success("Cập nhật trạng thái đơn thành công", bookingService.updateStatus(id, request.getStatus()));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lễ tân đánh dấu khách vãng mặt (No-Show) kèm lý do, áp dụng phạt đêm đầu và tạo yêu cầu hoàn cọc dư")
    public ApiResponse<BookingResponse> markAsNoShow(
            @PathVariable Long id,
            @Valid @RequestBody BookingNoShowRequest request
    ) {
        return ApiResponse.success("Đã ghi nhận khách vãng mặt (No-Show) thành công",
                bookingService.markBookingAsNoShow(id, request));
    }

    @PostMapping("/rooms/{bookingRoomId}/no-show")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lễ tân đánh dấu một phòng riêng lẻ khách không đến (Room-Level No-Show) kèm lý do, áp dụng phạt đêm đầu và nhả tài nguyên phòng")
    public ApiResponse<BookingResponse> markRoomAsNoShow(
            @PathVariable Long bookingRoomId,
            @Valid @RequestBody BookingNoShowRequest request
    ) {
        return ApiResponse.success("Đã ghi nhận phòng No-Show thành công",
                bookingService.markBookingRoomAsNoShow(bookingRoomId, request));
    }

    @PostMapping("/rooms/{bookingRoomId}/assign")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
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
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'F_AND_B')")
    @Operation(summary = "Thêm phụ phí (Early check-in, late check-out, minibar, F&B, hỏng hóc)")
    public ApiResponse<BookingChargeResponse> addCharge(
            @PathVariable Long bookingRoomId,
            @Valid @RequestBody BookingChargeCreateRequest request
    ) {
        return ApiResponse.success("Thêm phụ phí thành công", bookingService.addCharge(bookingRoomId, request));
    }

    @PostMapping("/rooms/{bookingRoomId}/preview-surcharge")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Xem trước đề xuất phụ thu nhận phòng sớm / trả phòng muộn theo mốc giờ thực tế")
    public ApiResponse<vn.edu.utc.hotel_booking.modules.booking.dto.response.SurchargeProposalResponse> previewSurcharge(
            @PathVariable Long bookingRoomId,
            @Valid @RequestBody SurchargePreviewRequest request
    ) {
        return ApiResponse.success("Đề xuất phụ thu thành công", bookingService.previewSurcharge(bookingRoomId, request));
    }

    @DeleteMapping("/charges/{chargeId}")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Xóa phụ phí đã nhập nhầm khỏi phòng và trừ tiền đơn đặt")
    public ApiResponse<Void> deleteCharge(@PathVariable Long chargeId) {
        bookingService.deleteCharge(chargeId);
        return ApiResponse.success("Xóa phụ phí thành công", null);
    }

    @PostMapping("/rooms/{bookingRoomId}/check-in")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Thực hiện Check-in nhận phòng")
    public ApiResponse<BookingResponse> checkIn(@PathVariable Long bookingRoomId) {
        return ApiResponse.success("Check-in nhận phòng thành công", bookingService.checkIn(bookingRoomId));
    }

    @PostMapping("/rooms/{bookingRoomId}/swap-room")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Đổi phòng cho khách lưu trú (Room Swap) kèm chuyển trạng thái buồng phòng và lưu vết lịch sử")
    public ApiResponse<RoomSwapResponse> swapRoom(
            @PathVariable Long bookingRoomId,
            @Valid @RequestBody RoomSwapRequest request
    ) {
        return ApiResponse.success("Đổi phòng thành công", bookingService.swapRoom(bookingRoomId, request));
    }

    @GetMapping("/rooms/{bookingRoomId}/checkout-settlement")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Lấy bảng kê chi phí, lịch sử thanh toán và số dư nợ để chuẩn bị Check-out")
    public ApiResponse<vn.edu.utc.hotel_booking.modules.booking.dto.response.CheckoutSettlementResponse> getCheckoutSettlement(
            @PathVariable Long bookingRoomId
    ) {
        return ApiResponse.success(bookingService.getCheckoutSettlement(bookingRoomId));
    }

    @PostMapping("/rooms/{bookingRoomId}/check-out")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Thực hiện Check-out trả phòng (Kiểm tra hết nợ và đóng sổ)")
    public ApiResponse<BookingResponse> checkOut(@PathVariable Long bookingRoomId) {
        return ApiResponse.success("Check-out trả phòng và đóng sổ thành công", bookingService.checkOut(bookingRoomId));
    }

    @PostMapping("/walk-in/quote")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Tính giá tạm tính và phát hiện phụ thu tự động cho quy trình đón khách vãng lai (Walk-in)")
    public ApiResponse<vn.edu.utc.hotel_booking.modules.booking.dto.response.WalkInQuoteResponse> calculateWalkInQuote(
            @Valid @RequestBody vn.edu.utc.hotel_booking.modules.booking.dto.request.WalkInQuoteRequest request
    ) {
        return ApiResponse.success("Tính báo giá walk-in thành công", bookingService.calculateWalkInQuote(request));
    }

    @PostMapping("/walk-in")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Tạo đơn đặt phòng khách vãng lai (Walk-in), thanh toán tại quầy và Check-in chuyển ngay sang IN-HOUSE")
    public ApiResponse<BookingResponse> createWalkInBooking(
            @Valid @RequestBody vn.edu.utc.hotel_booking.modules.booking.dto.request.WalkInBookingRequest request
    ) {
        return ApiResponse.success("Nhận khách vãng lai và check-in thành công", bookingService.createWalkInBooking(request));
    }

    @GetMapping("/{id}/cancel-preview")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Xem trước chi phí phạt và tiền hoàn khi hủy các phòng còn lại của đơn đặt phòng")
    public ApiResponse<vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingCancelPreviewResponse> previewCancelBooking(
            @PathVariable Long id
    ) {
        return ApiResponse.success(bookingService.previewCancelBooking(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lễ tân hủy các phòng còn lại của đơn đặt phòng (FIT <= 5 phòng)")
    public ApiResponse<vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingCancelResponse> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingCancelRequest request
    ) {
        return ApiResponse.success("Hủy đơn đặt phòng thành công", bookingService.cancelBooking(id, request));
    }

    @GetMapping("/rooms/{bookingRoomId}/cancel-preview")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Xem trước chi phí phạt và tiền hoàn khi hủy 1 phòng đơn lẻ (Room-Level Cancel Preview)")
    public ApiResponse<vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingCancelPreviewResponse> previewCancelBookingRoom(
            @PathVariable Long bookingRoomId
    ) {
        return ApiResponse.success(bookingService.previewCancelBookingRoom(bookingRoomId));
    }

    @PostMapping("/rooms/{bookingRoomId}/cancel")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Lễ tân hủy 1 phòng đơn lẻ trong đơn đặt phòng (Room-Level Cancel)")
    public ApiResponse<vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingCancelResponse> cancelBookingRoom(
            @PathVariable Long bookingRoomId,
            @Valid @RequestBody BookingCancelRequest request
    ) {
        return ApiResponse.success("Hủy phòng thành công", bookingService.cancelBookingRoom(bookingRoomId, request));
    }
}

