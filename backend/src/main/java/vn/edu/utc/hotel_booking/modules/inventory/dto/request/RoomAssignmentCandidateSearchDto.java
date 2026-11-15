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
public class RoomAssignmentCandidateSearchDto {

    @NotNull(message = "ID khách sạn không được để trống")
    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @Schema(description = "Ngày nhận phòng", example = "2026-11-16")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Schema(description = "Ngày trả phòng", example = "2026-11-18")
    LocalDate checkOutDate;

    @Schema(description = "ID hạng phòng mong muốn của lượt đặt", example = "1")
    Integer targetHotelRoomTypeId;

    @Schema(description = "ID BookingRoom hiện tại cần loại trừ khi đổi phòng / gán lại", example = "10")
    Long excludeBookingRoomId;
}

