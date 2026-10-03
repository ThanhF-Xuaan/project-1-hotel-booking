package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {

    @Schema(description = "Tên đăng nhập", example = "admin")
    @NotBlank(message = "Tên đăng nhập không được để trống")
    String username;

    @Schema(description = "Mật khẩu người dùng", example = "123456")
    @NotBlank(message = "Mật khẩu không được để trống")
    String password;
}
