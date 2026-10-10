package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.DestinationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DestinationSuggestionResponse {

    @Schema(description = "ID của điểm đến (ID khu vực hoặc ID khách sạn)", example = "1")
    Integer id;

    @Schema(description = "Loại điểm đến: REGION hoặc HOTEL", example = "REGION")
    DestinationType type;

    @Schema(description = "Tiêu đề hiển thị chính", example = "Hà Nội")
    String title;

    @Schema(description = "Tiêu đề phụ / thông tin địa chỉ", example = "Khu vực / Tỉnh thành")
    String subTitle;

    @Schema(description = "Huy hiệu phân loại", example = "Vùng miền")
    String badge;
}
