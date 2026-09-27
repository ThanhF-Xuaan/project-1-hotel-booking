package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomAssignmentRequest {

    @NotNull(message = "ID phòng vật lý không được để trống")
    Integer roomInstanceId;
}
