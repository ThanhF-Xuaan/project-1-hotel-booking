package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeSearchDto extends BaseSearchDto {

    @Schema(description = "Từ khóa tìm kiếm (mã hoặc tên)", example = "DELUXE")
    String keyword;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
