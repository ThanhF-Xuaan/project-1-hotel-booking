package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomStatusUpdateRequest {

    @NotBlank(message = "Trạng thái mới không được để trống (READY, OCCUPIED, CLEANING, DIRTY, MAINTENANCE)")
    String status;
}
