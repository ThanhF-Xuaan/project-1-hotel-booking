package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSlotSearchDto {

    @NotNull(message = "ID phòng không được để trống")
    @Schema(description = "ID phòng vật lý", example = "1")
    Integer roomInstanceId;

    @NotNull(message = "Từ ngày không được để trống")
    @Schema(description = "Từ ngày", example = "2026-10-01")
    LocalDate fromDate;

    @NotNull(message = "Đến ngày không được để trống")
    @Schema(description = "Đến ngày", example = "2026-10-07")
    LocalDate toDate;
}
