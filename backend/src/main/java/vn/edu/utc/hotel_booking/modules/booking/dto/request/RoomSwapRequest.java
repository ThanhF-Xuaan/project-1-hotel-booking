package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSwapRequest {

    @NotNull(message = "ID phòng đích không được để trống")
    Integer targetRoomInstanceId;

    @NotBlank(message = "Lý do đổi phòng không được để trống")
    String reason;

    @Builder.Default
    Boolean isComplimentary = false;

    LocalDate effectiveDate;
}
