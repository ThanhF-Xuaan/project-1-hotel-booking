package vn.edu.utc.hotel_booking.modules.operation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;

@RestController
@RequestMapping("/api/v1/public/menus")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public - Menus", description = "Public APIs tra cứu thực đơn, dịch vụ F&B, giặt là, spa cho khách hàng")
public class PublicMenuController {

    MenuService menuService;

    @PostMapping("/filter")
    @Operation(summary = "Tìm kiếm và lọc thực đơn/dịch vụ theo khách sạn")
    public ApiResponse<PageResponse<MenuResponse>> filterMenus(@RequestBody(required = false) MenuSearchDto searchDto) {
        MenuSearchDto criteria = searchDto != null ? searchDto : new MenuSearchDto();
        return ApiResponse.success(menuService.filter(criteria));
    }

    @GetMapping("/hotel/{hotelId}")
    @Operation(summary = "Lấy toàn bộ menu của một khách sạn")
    public ApiResponse<PageResponse<MenuResponse>> getMenusByHotel(
            @PathVariable Short hotelId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer size) {
        MenuSearchDto searchDto = new MenuSearchDto();
        searchDto.setHotelId(hotelId);
        searchDto.setPage(page);
        searchDto.setPageSize(size);
        return ApiResponse.success(menuService.filter(searchDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết món ăn / dịch vụ theo ID")
    public ApiResponse<MenuResponse> getMenuById(@PathVariable Integer id) {
        return ApiResponse.success(menuService.getById(id));
    }
}
