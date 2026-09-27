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
public class RoomInstanceSearchDto extends BaseSearchDto {

    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "Số phòng cần tìm", example = "301")
    String roomNumber;

    @Schema(description = "Trạng thái phòng", example = "READY")
    String currentStatus;
}
