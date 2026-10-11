package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
public class BatchHoldInventoryRequest {

    @NotNull(message = "ID khách sạn không được để trống")
    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @Schema(description = "Ngày check-in", example = "2026-10-15")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Schema(description = "Ngày check-out", example = "2026-10-17")
    LocalDate checkOutDate;

    @Min(value = 1, message = "Thời gian giữ phòng tối thiểu là 1 phút")
    @Schema(description = "Số phút giữ phòng tạm thời (mặc định 15)", example = "15")
    @Builder.Default
    Integer holdMinutes = 15;

    @NotEmpty(message = "Danh sách hạng phòng cần giữ không được để trống")
    @Valid
    @Builder.Default
    @Schema(description = "Danh sách các hạng phòng và số lượng phòng cần giữ")
    List<RoomTypeHoldItem> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomTypeHoldItem {

        @NotNull(message = "ID cấu hình loại phòng không được để trống")
        @Schema(description = "ID cấu hình loại phòng", example = "1")
        Integer hotelRoomTypeId;

        @NotNull(message = "Số lượng phòng cần giữ không được để trống")
        @Min(value = 1, message = "Số lượng phòng giữ tối thiểu là 1")
        @Schema(description = "Số lượng phòng giữ", example = "1")
        @Builder.Default
        Integer roomsCount = 1;
    }
}
