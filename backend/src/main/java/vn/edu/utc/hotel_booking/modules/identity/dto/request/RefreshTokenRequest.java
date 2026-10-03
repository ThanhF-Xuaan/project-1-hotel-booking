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
public class RefreshTokenRequest {

    @Schema(description = "Refresh Token hợp lệ được cấp từ phiên đăng nhập", example = "eyJhbGciOiJIUzI1NiIsIn...")
    @NotBlank(message = "Refresh Token không được để trống")
    String refreshToken;
}
