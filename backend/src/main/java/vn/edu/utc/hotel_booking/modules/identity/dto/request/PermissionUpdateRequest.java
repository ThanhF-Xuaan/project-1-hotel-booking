package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionUpdateRequest {

    @Schema(description = "Tài nguyên hoặc module áp dụng quyền", example = "ROOM_INSTANCE")
    @NotBlank(message = "Tài nguyên (resource) không được để trống")
    @Size(max = 100, message = "Tên tài nguyên tối đa 100 ký tự")
    String resource;

    @Schema(description = "Hành động thực thi trên tài nguyên", example = "UPDATE")
    @NotBlank(message = "Hành động (action) không được để trống")
    @Size(max = 50, message = "Tên hành động tối đa 50 ký tự")
    String action;

    @Schema(description = "Trạng thái hoạt động", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}

