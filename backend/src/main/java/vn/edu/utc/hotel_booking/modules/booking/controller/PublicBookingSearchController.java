package vn.edu.utc.hotel_booking.modules.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingSearchRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.RoomPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.*;
import vn.edu.utc.hotel_booking.modules.booking.service.PublicBookingSearchService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/booking-search")
@RequiredArgsConstructor
@Tag(name = "Public - Booking Search", description = "APIs tìm kiếm khách sạn và hạng phòng trực tuyến dành cho khách hàng")
public class PublicBookingSearchController {

    private final PublicBookingSearchService publicBookingSearchService;

    @GetMapping("/destinations")
    @Operation(summary = "Gợi ý điểm đến (Khu vực / Khách sạn) cho ô nhập Dropdown tìm kiếm")
    public ApiResponse<List<DestinationSuggestionResponse>> getDestinations(
            @RequestParam(required = false) String keyword
    ) {
        List<DestinationSuggestionResponse> suggestions = publicBookingSearchService.getDestinationSuggestions(keyword);
        return ApiResponse.<List<DestinationSuggestionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách gợi ý điểm đến thành công")
                .result(suggestions)
                .build();
    }

    @PostMapping("/unified")
    @Operation(summary = "Tìm kiếm hợp nhất từ 1 ô Dropdown (Tự động trả về danh sách khách sạn nếu chọn Khu vực, hoặc danh sách phòng nếu chọn Khách sạn)")
    public ApiResponse<UnifiedBookingSearchResponse> searchUnified(
            @Valid @RequestBody BookingSearchRequest request
    ) {
        UnifiedBookingSearchResponse response = publicBookingSearchService.searchUnified(request);
        return ApiResponse.<UnifiedBookingSearchResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Tìm kiếm thành công")
                .result(response)
                .build();
    }

    @PostMapping("/hotels")
    @Operation(summary = "Tìm kiếm khách sạn khả dụng theo khu vực và dải ngày lưu trú")
    public ApiResponse<PageResponse<AvailableHotelResponse>> searchHotels(
            @Valid @RequestBody BookingSearchRequest request
    ) {
        PageResponse<AvailableHotelResponse> response = publicBookingSearchService.searchHotels(request);
        return ApiResponse.<PageResponse<AvailableHotelResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Tìm kiếm khách sạn khả dụng thành công")
                .result(response)
                .build();
    }

    @PostMapping("/rooms")
    @Operation(summary = "Tìm kiếm các hạng phòng khả dụng của khách sạn kèm bảng giá qua Pricing Pipeline, tiện ích, tặng kèm và chính sách phụ thu")
    public ApiResponse<List<AvailableRoomTypeResponse>> searchRooms(
            @Valid @RequestBody BookingSearchRequest request
    ) {
        List<AvailableRoomTypeResponse> response = publicBookingSearchService.searchRooms(request);
        return ApiResponse.<List<AvailableRoomTypeResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Tìm kiếm hạng phòng khả dụng thành công")
                .result(response)
                .build();
    }

    @PostMapping("/quote")
    @Operation(summary = "Tính toán lại giá phòng và các khoản phụ thu thời gian thực khi khách chọn số lượng Người lớn, Trẻ em, Trẻ sơ sinh và Giường phụ")
    public ApiResponse<RoomPriceQuoteResponse> calculateRoomQuote(
            @Valid @RequestBody RoomPriceQuoteRequest request
    ) {
        RoomPriceQuoteResponse response = publicBookingSearchService.calculateRoomQuote(request);
        return ApiResponse.<RoomPriceQuoteResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Tính toán báo giá phòng thành công")
                .result(response)
                .build();
    }
}

