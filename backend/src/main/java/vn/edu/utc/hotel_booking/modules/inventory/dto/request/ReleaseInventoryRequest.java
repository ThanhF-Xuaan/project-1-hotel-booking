package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReleaseInventoryRequest {

    @NotNull(message = "ID cấu hình loại phòng không được để trống")
    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @Schema(description = "Ngày check-in", example = "2026-10-01")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Schema(description = "Ngày check-out", example = "2026-10-03")
    LocalDate checkOutDate;

    @NotNull(message = "Số lượng phòng cần giải phóng không được để trống")
    @Min(value = 1, message = "Số lượng phòng tối thiểu là 1")
    @Schema(description = "Số lượng phòng cần mở khóa", example = "1")
    @Builder.Default
    Integer roomsCount = 1;
}
