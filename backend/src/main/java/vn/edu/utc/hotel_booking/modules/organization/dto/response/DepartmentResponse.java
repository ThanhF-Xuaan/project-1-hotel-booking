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
public class DepartmentResponse {

    @Schema(description = "ID phòng ban", example = "1")
    Short id;

    @Schema(description = "Mã phòng ban", example = "FRONT_OFFICE")
    String code;

    @Schema(description = "Tên phòng ban", example = "Lễ tân Tiền sảnh")
    String name;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    OffsetDateTime updatedAt;
}
