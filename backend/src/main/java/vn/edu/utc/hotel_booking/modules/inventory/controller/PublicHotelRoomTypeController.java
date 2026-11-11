package vn.edu.utc.hotel_booking.modules.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeMediaService;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelImageResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/hotel-room-types")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public - Hotel Room Types", description = "APIs công khai tra cứu hạng phòng cho khách hàng")
public class PublicHotelRoomTypeController {

    HotelRoomTypeMediaService hotelRoomTypeMediaService;

    @GetMapping("/{id}/images")
    @Operation(summary = "Lấy danh sách hình ảnh của hạng phòng công khai cho khách xem chi tiết")
    public ApiResponse<List<HotelImageResponse>> getPublicImages(@PathVariable Integer id) {
        return ApiResponse.<List<HotelImageResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách ảnh hạng phòng thành công")
                .result(hotelRoomTypeMediaService.getImages(id))
                .build();
    }
}

