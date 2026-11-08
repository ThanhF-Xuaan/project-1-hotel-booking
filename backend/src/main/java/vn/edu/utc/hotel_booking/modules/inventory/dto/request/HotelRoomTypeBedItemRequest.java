package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomTypeBedItemRequest {

    @NotNull(message = "ID loại giường không được để trống")
    @Schema(description = "ID loại giường", example = "1")
    Short roomBedId;

    @NotNull(message = "Số lượng giường không được để trống")
    @Min(value = 1, message = "Số lượng giường tối thiểu là 1")
    @Schema(description = "Số lượng giường", example = "1")
    @Builder.Default
    Short baseQuantity = 1;
}

