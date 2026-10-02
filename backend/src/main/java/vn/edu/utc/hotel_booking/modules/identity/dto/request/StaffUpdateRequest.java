package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffUpdateRequest {

    @Schema(description = "ID vai trò mới", example = "3")
    Short roleId;

    @Schema(description = "Phạm vi quản lý dữ liệu mới: CHAIN, REGION hoặc PROPERTY", example = "PROPERTY")
    String scopeType;

    @Schema(description = "ID thực thể theo phạm vi mới", example = "1")
    Integer scopeEntityId;

    @Schema(description = "ID phòng ban mới", example = "1")
    Short departmentId;

    @Schema(description = "Email mới", example = "receptionist.new@utc.edu.vn")
    @Email(message = "Định dạng email không hợp lệ")
    String email;

    @Schema(description = "Số điện thoại mới", example = "0988889999")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    String phone;

    @Schema(description = "Tên", example = "Văn B")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    String firstName;

    @Schema(description = "Họ và tên đệm", example = "Trần")
    @Size(max = 100, message = "Họ tối đa 100 ký tự")
    String lastName;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Mật khẩu mới (Nếu muốn đổi mật khẩu trên Keycloak)", example = "NewSecret@123")
    @Size(min = 6, message = "Mật khẩu phải có tối thiểu 6 ký tự")
    String password;
}
