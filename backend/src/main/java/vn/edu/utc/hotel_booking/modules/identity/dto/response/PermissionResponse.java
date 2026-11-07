package vn.edu.utc.hotel_booking.modules.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionResponse {

    @Schema(description = "ID quyền hạn", example = "1")
    Short id;

    @Schema(description = "Hành động (VIEW, CREATE, UPDATE, DELETE...)", example = "VIEW")
    String action;

    @Schema(description = "Tài nguyên bảo vệ (HOTEL, STAFF, BOOKING...)", example = "PROPERTY")
    String resource;

    @Schema(description = "Mô tả chi tiết quyền hạn", example = "Xem danh sách và chi tiết đơn đặt phòng")
    String description;

    @Schema(description = "Danh sách mã vai trò sở hữu quyền")
    List<String> roleCodes;

    @Schema(description = "Danh sách tên vai trò sở hữu quyền")
    List<String> roleNames;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
