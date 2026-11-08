package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomFeatureCreateRequest {

    @NotBlank(message = "Mã tiện ích không được để trống")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã tiện ích chỉ được chứa chữ hoa không dấu, số và gạch dưới (VD: BALCONY, MINIBAR)")
    @Size(max = 50, message = "Mã tiện ích tối đa 50 ký tự")
    @Schema(description = "Mã tiện ích", example = "BALCONY")
    String code;

    @NotBlank(message = "Tên tiện ích không được để trống")
    @Size(max = 150, message = "Tên tiện ích tối đa 150 ký tự")
    @Schema(description = "Tên tiện ích", example = "Ban công ngắm biển")
    String name;

    @Size(max = 50, message = "Danh mục tối đa 50 ký tự")
    @Schema(description = "Danh mục tiện ích (BATHROOM, BEDROOM, MEDIA, ENTERTAINMENT, AMENITY, COMFORT, INTERNET, OTHER)", example = "BATHROOM")
    String category;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}

