package vn.edu.utc.hotel_booking.modules.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenResponse {

    @Schema(description = "Access Token JWT dùng để xác thực các API", example = "eyJhbGciOiJSUzI1NiIsIn...")
    @JsonProperty("access_token")
    String accessToken;

    @Schema(description = "Thời gian hết hạn của Access Token (giây)", example = "3600")
    @JsonProperty("expires_in")
    Long expiresIn;

    @Schema(description = "Thời gian hết hạn của Refresh Token (giây)", example = "86400")
    @JsonProperty("refresh_expires_in")
    Long refreshExpiresIn;

    @Schema(description = "Refresh Token dùng để lấy Access Token mới", example = "eyJhbGciOiJIUzI1NiIsIn...")
    @JsonProperty("refresh_token")
    String refreshToken;

    @Schema(description = "Loại Token", example = "Bearer")
    @JsonProperty("token_type")
    String tokenType;

    @Schema(description = "Phạm vi cấp quyền của Token", example = "openid profile email roles")
    @JsonProperty("scope")
    String scope;
}
