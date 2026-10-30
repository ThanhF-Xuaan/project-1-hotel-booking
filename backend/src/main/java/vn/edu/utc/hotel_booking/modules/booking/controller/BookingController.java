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
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.RoomingListConfirmRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListImportResultResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListPreviewResponse;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingService;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomInstanceResponse;

import java.util.List;

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

    @PostMapping("/rooms/{bookingRoomId}/check-in")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Thực hiện Check-in nhận phòng")
    public ApiResponse<BookingResponse> checkIn(@PathVariable Long bookingRoomId) {
        return ApiResponse.success("Check-in nhận phòng thành công", bookingService.checkIn(bookingRoomId));
    }

    @PostMapping("/rooms/{bookingRoomId}/check-out")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Thực hiện Check-out trả phòng")
    public ApiResponse<BookingResponse> checkOut(@PathVariable Long bookingRoomId) {
        return ApiResponse.success("Check-out trả phòng thành công", bookingService.checkOut(bookingRoomId));
    }

    @PostMapping(value = "/{id}/rooming-list/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Xem trước danh sách phòng từ file Excel Rooming List (Preview)")
    public ApiResponse<RoomingListPreviewResponse> previewRoomingList(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Đọc và kiểm tra file Excel thành công", bookingService.previewRoomingList(id, file));
    }

    @PostMapping(value = "/{id}/rooming-list/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Import danh sách phòng từ file Excel (Hỗ trợ dryRun/confirm)")
    public ApiResponse<RoomingListImportResultResponse> importRoomingList(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirm", defaultValue = "false") boolean confirm) {
        RoomingListImportResultResponse result = bookingService.importRoomingList(id, file, confirm);
        String msg = confirm
                ? "Import danh sách phòng thành công (" + result.getImportedCount() + " khách)"
                : "Phân tích file Excel thành công (chế độ xem trước)";
        return ApiResponse.success(msg, result);
    }

    @PostMapping("/{id}/rooming-list/confirm")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Xác nhận lưu danh sách khách đã preview vào đơn đặt phòng")
    public ApiResponse<RoomingListImportResultResponse> confirmRoomingList(
            @PathVariable Long id,
            @Valid @RequestBody RoomingListConfirmRequest request) {
        RoomingListImportResultResponse result = bookingService.confirmRoomingList(id, request);
        return ApiResponse.success("Xác nhận import danh sách phòng thành công (" + result.getImportedCount() + " khách)", result);
    }

    @GetMapping("/vacant-rooms")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'SALES_GROUP')")
    @Operation(summary = "Xem danh sách các phòng vật lý đang còn trống (READY) kèm sức chứa")
    public ApiResponse<List<RoomInstanceResponse>> getVacantRooms(
            @RequestParam(value = "hotelId", required = false) Short hotelId) {
        return ApiResponse.success("Lấy danh sách phòng trống thành công", bookingService.getVacantRooms(hotelId));
    }
}
