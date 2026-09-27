package vn.edu.utc.hotel_booking.modules.organization.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegionResponse {

    @Schema(description = "ID khu vực", example = "1")
    Short id;

    @Schema(description = "Mã khu vực", example = "NORTH")
    String code;

    @Schema(description = "Tên khu vực", example = "Miền Bắc")
    String name;

    @Schema(description = "Mô tả chi tiết", example = "Khu vực các tỉnh phía Bắc")
    String description;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    OffsetDateTime updatedAt;
}
