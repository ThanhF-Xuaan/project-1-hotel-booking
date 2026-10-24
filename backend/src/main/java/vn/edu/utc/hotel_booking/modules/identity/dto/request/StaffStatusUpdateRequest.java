package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.identity.entity.StaffStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Yêu cầu thay đổi trạng thái tài khoản nhân sự (Vòng đời tài khoản)")
public class StaffStatusUpdateRequest {

    @NotNull(message = "Trạng thái nhân sự không được để trống")
    @Schema(description = "Trạng thái mới của tài khoản nhân viên", example = "SUSPENDED")
    StaffStatus status;

    @Schema(description = "Lý do thay đổi trạng thái (Tùy chọn phục vụ kiểm toán)", example = "Nhân viên tạm nghỉ thai sản")
    String reason;
}
