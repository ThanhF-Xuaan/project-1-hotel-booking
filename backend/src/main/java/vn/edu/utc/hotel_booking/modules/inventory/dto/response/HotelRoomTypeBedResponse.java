package vn.edu.utc.hotel_booking.modules.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomTypeBedResponse {

    @Schema(description = "ID loại giường", example = "1")
    Short roomBedId;

    @Schema(description = "Tên loại giường", example = "King Size Bed (Giường cỡ đại)")
    String roomBedName;

    @Schema(description = "Kích thước giường", example = "1.8m x 2.0m")
    String roomBedSize;

    @Schema(description = "Số lượng giường", example = "1")
    Short baseQuantity;
}

