package vn.edu.utc.hotel_booking.modules.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffResponse {

    @Schema(description = "ID nhân viên", example = "1")
    Integer id;

    @Schema(description = "Keycloak UUID", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID keycloakId;

    @Schema(description = "ID vai trò", example = "3")
    Short roleId;

    @Schema(description = "Tên vai trò", example = "Tổng Quản lý Khách sạn")
    String roleName;

    @Schema(description = "Mã vai trò", example = "PROPERTY_MANAGER")
    String roleCode;

    @Schema(description = "Phạm vi dữ liệu: CHAIN, REGION, PROPERTY", example = "PROPERTY")
    String scopeType;

    @Schema(description = "ID thực thể theo phạm vi (hotel_id / region_id)", example = "1")
    Integer scopeEntityId;

    @Schema(description = "ID phòng ban", example = "1")
    Short departmentId;

    @Schema(description = "Tên phòng ban", example = "Lễ tân Tiền sảnh")
    String departmentName;

    @Schema(description = "Tên đăng nhập", example = "receptionist_hn")
    String username;

    @Schema(description = "Email", example = "receptionist@utc.edu.vn")
    String email;

    @Schema(description = "Số điện thoại", example = "0988889999")
    String phone;

    @Schema(description = "Tên", example = "Văn A")
    String firstName;

    @Schema(description = "Họ và tên đệm", example = "Nguyễn")
    String lastName;

    @Schema(description = "Họ và tên đầy đủ", example = "Nguyễn Văn A")
    String fullName;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
