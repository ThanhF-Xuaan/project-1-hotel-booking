package vn.edu.utc.hotel_booking.modules.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomFeatureResponse {

    @Schema(description = "ID tiện ích", example = "1")
    Short id;

    @Schema(description = "Mã tiện ích", example = "BALCONY")
    String code;

    @Schema(description = "Tên tiện ích", example = "Ban công ngắm biển")
    String name;

    @Schema(description = "Mô tả")
    String description;
}
