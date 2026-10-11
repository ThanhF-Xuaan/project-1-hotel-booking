package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchReleaseInventoryRequest {

    @NotNull(message = "ID khách sạn không được để trống")
    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @Schema(description = "Ngày check-in", example = "2026-10-15")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Schema(description = "Ngày check-out", example = "2026-10-17")
    LocalDate checkOutDate;

    @NotEmpty(message = "Danh sách hạng phòng cần giải phóng không được để trống")
    @Valid
    @Builder.Default
    @Schema(description = "Danh sách các hạng phòng và số lượng phòng cần mở khóa")
    List<BatchHoldInventoryRequest.RoomTypeHoldItem> items = new ArrayList<>();
}
