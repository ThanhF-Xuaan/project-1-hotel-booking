package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnifiedBookingSearchResponse {

    @Schema(description = "Loại kết quả tìm kiếm: HOTELS (danh sách khách sạn) hoặc ROOMS (chi tiết phòng)", example = "HOTELS")
    String searchType; // "HOTELS" hoặc "ROOMS"

    @Schema(description = "Danh sách khách sạn khả dụng (khi searchType = HOTELS)")
    PageResponse<AvailableHotelResponse> hotels;

    @Schema(description = "Danh sách các hạng phòng khả dụng (khi searchType = ROOMS)")
    List<AvailableRoomTypeResponse> rooms;

    @Schema(description = "Thông tin khách sạn được chọn (khi searchType = ROOMS)")
    AvailableHotelResponse selectedHotel;
}
