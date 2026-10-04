package vn.edu.utc.hotel_booking.modules.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

@RestController
@RequestMapping("/api/v1/public/hotels")
@RequiredArgsConstructor
@Tag(name = "Public - Hotels", description = "Public APIs tra cứu thông tin khách sạn dành cho khách hàng")
public class PublicHotelController {

    private final HotelService hotelService;

    @PostMapping("/filter")
    @Operation(summary = "Tìm kiếm và phân trang danh sách khách sạn công khai")
    public ApiResponse<PageResponse<HotelResponse>> filterHotels(@RequestBody(required = false) HotelSearchDto searchDto) {
        HotelSearchDto criteria = searchDto != null ? searchDto : new HotelSearchDto();
        return ApiResponse.<PageResponse<HotelResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách khách sạn thành công")
                .result(hotelService.filter(criteria))
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách khách sạn cơ bản")
    public ApiResponse<PageResponse<HotelResponse>> getAllHotels(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        HotelSearchDto searchDto = new HotelSearchDto();
        searchDto.setPage(page);
        searchDto.setPageSize(size);
        return ApiResponse.<PageResponse<HotelResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách khách sạn thành công")
                .result(hotelService.filter(searchDto))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết thông tin khách sạn")
    public ApiResponse<HotelResponse> getHotelById(@PathVariable Short id) {
        return ApiResponse.<HotelResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin khách sạn thành công")
                .result(hotelService.getById(id))
                .build();
    }
}
