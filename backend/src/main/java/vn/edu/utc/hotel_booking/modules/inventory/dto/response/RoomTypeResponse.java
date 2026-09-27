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
public class RoomTypeResponse {

    @Schema(description = "ID loại phòng", example = "1")
    Short id;

    @Schema(description = "Mã loại phòng", example = "DELUXE")
    String code;

    @Schema(description = "Tên hiển thị loại phòng", example = "Phòng Deluxe Hướng Biển")
    String name;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
