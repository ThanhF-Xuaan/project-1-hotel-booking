package vn.edu.utc.hotel_booking.modules.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuestResponse {

    @Schema(description = "ID khách hàng", example = "1")
    Long id;

    @Schema(description = "Mã định danh công khai UUID", example = "d3b07384-d113-46fb-9a4f-5f56476104bc")
    UUID publicId;

    @Schema(description = "Ngày sinh", example = "1990-05-15")
    LocalDate birthDate;

    @Schema(description = "Loại giấy tờ tùy thân", example = "CCCD")
    String identityType;

    @Schema(description = "Số giấy tờ tùy thân", example = "001200001234")
    String identityNumber;

    @Schema(description = "Quốc tịch", example = "Việt Nam")
    String nationality;

    @Schema(description = "Email", example = "guest@gmail.com")
    String email;

    @Schema(description = "Số điện thoại", example = "0912345678")
    String phone;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời gian tạo hồ sơ")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
