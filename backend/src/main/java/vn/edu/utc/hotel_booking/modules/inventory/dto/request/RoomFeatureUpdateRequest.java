package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomFeatureUpdateRequest {

    @NotBlank(message = "Tên tiện ích không được để trống")
    @Size(max = 150, message = "Tên tiện ích tối đa 150 ký tự")
    @Schema(description = "Tên tiện ích", example = "Ban công ngắm biển")
    String name;

    @Size(max = 50, message = "Danh mục tối đa 50 ký tự")
    @Schema(description = "Danh mục tiện ích (VIEW, BATHROOM, BEDROOM, MEDIA, ENTERTAINMENT, AMENITY, COMFORT, INTERNET, OTHER)", example = "VIEW")
    String category;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}

