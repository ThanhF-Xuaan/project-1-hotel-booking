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
public class RoomBedUpdateRequest {

    @NotBlank(message = "Tên loại giường không được để trống")
    @Size(max = 100, message = "Tên loại giường tối đa 100 ký tự")
    @Schema(description = "Tên loại giường", example = "King Size Bed (Giường cỡ đại)")
    String name;

    @Size(max = 50, message = "Kích thước tối đa 50 ký tự")
    @Schema(description = "Kích thước giường", example = "1.8m x 2.0m")
    String size;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}

