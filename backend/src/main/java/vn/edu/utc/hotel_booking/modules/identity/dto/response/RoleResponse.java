package vn.edu.utc.hotel_booking.modules.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {

    @Schema(description = "ID vai trò", example = "1")
    Short id;

    @Schema(description = "Tên vai trò", example = "Quản trị viên Toàn Chuỗi")
    String name;

    @Schema(description = "Mã vai trò (Keycloak Role Code)", example = "CHAIN_ADMIN")
    String code;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Danh sách quyền hạn của vai trò")
    Set<PermissionResponse> permissions;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
