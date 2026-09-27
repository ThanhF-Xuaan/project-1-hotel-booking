package vn.edu.utc.hotel_booking.modules.organization.dto.request;

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
public class RegionUpdateRequest {

    @Schema(description = "Tên hiển thị của vùng", example = "Miền Bắc")
    @NotBlank(message = "Tên vùng không được để trống")
    @Size(max = 150, message = "Tên vùng tối đa 150 ký tự")
    String name;

    @Schema(description = "Mô tả chi tiết về vùng/khu vực", example = "Khu vực các tỉnh phía Bắc")
    String description;

    @Schema(description = "Trạng thái hoạt động", example = "ACTIVE")
    String status;
}
