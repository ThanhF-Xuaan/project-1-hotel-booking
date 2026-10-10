package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomInclusionDto {

    @Schema(description = "ID cấu hình tặng kèm", example = "1")
    Long id;

    @Schema(description = "Loại tham chiếu", example = "POS_PRODUCT")
    String referenceType;

    @Schema(description = "ID tham chiếu món/dịch vụ", example = "5")
    Integer referenceId;

    @Schema(description = "Tên quà/dịch vụ tặng kèm", example = "Buffet Sáng Tiêu Chuẩn")
    String name;

    @Schema(description = "Số lượng tặng kèm", example = "1")
    Integer quantity;

    @Schema(description = "Mô tả chi tiết quà tặng kèm", example = "Miễn phí buffet sáng hàng ngày cho khách lưu trú")
    String description;
}
