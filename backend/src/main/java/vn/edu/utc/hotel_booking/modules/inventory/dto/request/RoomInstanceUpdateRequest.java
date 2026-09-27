package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomInstanceUpdateRequest {

    @NotNull(message = "ID loại phòng khách sạn không được để trống")
    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @NotBlank(message = "Số phòng không được để trống")
    @Size(max = 20, message = "Số phòng tối đa 20 ký tự")
    @Schema(description = "Số phòng / Tên phòng vật lý", example = "P301")
    String roomNumber;

    @NotBlank(message = "Trạng thái hiện tại không được để trống")
    @Schema(description = "Trạng thái hiện tại", example = "READY")
    String currentStatus;
}
