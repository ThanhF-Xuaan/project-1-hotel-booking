package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleCreateRequest {

    @Schema(description = "Tên hiển thị của vai trò", example = "Tổng Quản lý Khách sạn")
    @NotBlank(message = "Tên vai trò không được để trống")
    @Size(max = 100, message = "Tên vai trò tối đa 100 ký tự")
    String name;

    @Schema(description = "Mã vai trò duy nhất", example = "PROPERTY_MANAGER")
    @NotBlank(message = "Mã vai trò không được để trống")
    @Size(max = 50, message = "Mã vai trò tối đa 50 ký tự")
    String code;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";

    @Schema(description = "Danh sách ID các quyền hạn được gán cho vai trò này")
    Set<Short> permissionIds;
}
