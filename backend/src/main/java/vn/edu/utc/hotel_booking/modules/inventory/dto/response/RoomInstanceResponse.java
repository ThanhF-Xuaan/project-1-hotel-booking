package vn.edu.utc.hotel_booking.modules.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomInstanceResponse {

    @Schema(description = "ID phòng vật lý", example = "1")
    Integer id;

    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "Tên khách sạn", example = "Grand Hotel Da Nang")
    String hotelName;

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "Mã loại phòng", example = "DELUXE")
    String roomTypeCode;

    @Schema(description = "Tên loại phòng", example = "Phòng Deluxe Hướng Biển")
    String roomTypeName;

    @Schema(description = "Số phòng / Tên phòng", example = "P301")
    String roomNumber;

    @Schema(description = "Trạng thái hiện tại", example = "READY")
    String currentStatus;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
