package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeUpdateRequest {

    @NotBlank(message = "Tên loại phòng không được để trống")
    @Size(max = 150, message = "Tên loại phòng tối đa 150 ký tự")
    @Schema(description = "Tên loại phòng", example = "Phòng Deluxe Hướng Biển VIP")
    String name;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
