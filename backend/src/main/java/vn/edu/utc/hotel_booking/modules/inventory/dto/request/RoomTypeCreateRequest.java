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
public class RoomTypeCreateRequest {

    @NotBlank(message = "Mã loại phòng không được để trống")
    @Size(max = 50, message = "Mã loại phòng tối đa 50 ký tự")
    @Schema(description = "Mã loại phòng", example = "DELUXE")
    String code;

    @NotBlank(message = "Tên loại phòng không được để trống")
    @Size(max = 150, message = "Tên loại phòng tối đa 150 ký tự")
    @Schema(description = "Tên loại phòng", example = "Phòng Deluxe Hướng Biển")
    String name;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
