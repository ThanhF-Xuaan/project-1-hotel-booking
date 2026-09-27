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
public class RoomAvailabilityResponse {

    @Schema(description = "ID bản ghi tồn phòng", example = "1")
    Long id;

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "Ngày", example = "2026-10-01")
    LocalDate date;

    @Schema(description = "Tổng số phòng", example = "10")
    Integer totalRooms;

    @Schema(description = "Số phòng đã đặt", example = "3")
    Integer bookedRooms;

    @Schema(description = "Số phòng đang giữ tạm thời", example = "1")
    Integer lockedRooms;

    @Schema(description = "Số phòng bảo trì / out-of-order", example = "0")
    Integer oooRooms;

    @Schema(description = "Số phòng còn trống thực tế", example = "6")
    Integer availableCount;

    @Schema(description = "Thời hạn khóa phòng tạm")
    OffsetDateTime lockedUntil;

    @Schema(description = "Version kiểm soát Optimistic Lock", example = "1")
    Long version;
}
