package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffCreateRequest {

    @Schema(description = "Mật khẩu khởi tạo tài khoản (Tùy chọn, nếu để trống hệ thống sẽ sinh mật khẩu ngẫu nhiên và yêu cầu đổi mật khẩu lần đầu)", example = "Hotel@123456")
    @Size(min = 6, message = "Mật khẩu phải có tối thiểu 6 ký tự")
    String password;

    @Schema(description = "ID vai trò được gán", example = "3")
    @NotNull(message = "Vai trò không được để trống")
    Short roleId;

    @Schema(description = "Phạm vi quản lý dữ liệu: CHAIN, REGION hoặc PROPERTY", example = "PROPERTY")
    @NotBlank(message = "Phạm vi quản lý không được để trống")
    String scopeType;

    @Schema(description = "ID thực thể theo phạm vi (hotel_id nếu PROPERTY, region_id nếu REGION, null nếu CHAIN)", example = "1")
    Integer scopeEntityId;

    @Schema(description = "ID phòng ban trực thuộc (chỉ áp dụng cho cấp PROPERTY)", example = "1")
    Short departmentId;

    @Schema(description = "Tên đăng nhập hệ thống", example = "receptionist_hanoi")
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(max = 100, message = "Tên đăng nhập tối đa 100 ký tự")
    String username;

    @Schema(description = "Email liên hệ", example = "receptionist.hn@utc.edu.vn")
    @Email(message = "Định dạng email không hợp lệ")
    String email;

    @Schema(description = "Số điện thoại liên hệ", example = "0988889999")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    String phone;

    @Schema(description = "Tên (First Name)", example = "Văn A")
    @NotBlank(message = "Tên không được để trống")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    String firstName;

    @Schema(description = "Họ và tên đệm (Last Name)", example = "Nguyễn")
    @NotBlank(message = "Họ không được để trống")
    @Size(max = 100, message = "Họ tối đa 100 ký tự")
    String lastName;

    @Schema(description = "Trạng thái hoạt động", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
