package vn.edu.utc.hotel_booking.modules.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSlotResponse {

    @Schema(description = "ID slot phòng", example = "1")
    Long id;

    @Schema(description = "ID phòng vật lý", example = "1")
    Integer roomInstanceId;

    @Schema(description = "Số phòng", example = "P301")
    String roomNumber;

    @Schema(description = "Ngày của slot", example = "2026-10-01")
    LocalDate slotDate;

    @Schema(description = "ID đơn đặt phòng chi tiết", example = "105")
    Long bookingRoomId;

    @Schema(description = "Trạng thái slot", example = "OCCUPIED")
    String status;

    @Schema(description = "Thời điểm khóa")
    OffsetDateTime lockedAt;

    @Schema(description = "Thời điểm đặt")
    OffsetDateTime reservedAt;
}
