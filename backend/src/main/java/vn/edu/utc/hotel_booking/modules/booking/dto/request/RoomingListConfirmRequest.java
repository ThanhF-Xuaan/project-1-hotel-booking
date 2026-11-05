package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListRowDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Yêu cầu xác nhận import danh sách phòng sau khi đã preview")
public class RoomingListConfirmRequest {

    @NotEmpty(message = "Danh sách khách xác nhận import không được để trống")
    @Schema(description = "Danh sách các dòng khách hợp lệ đã được kiểm tra tại bước preview")
    List<RoomingListRowDto> guests;
}
