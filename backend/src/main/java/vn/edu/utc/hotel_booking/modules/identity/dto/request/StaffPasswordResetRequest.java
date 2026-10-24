package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Yêu cầu đặt lại mật khẩu cho tài khoản nhân sự")
public class StaffPasswordResetRequest {

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu mới phải có độ dài từ 6 đến 50 ký tự")
    @Schema(description = "Mật khẩu mới được thiết lập", example = "Pass@123456")
    String newPassword;
}
