package vn.edu.utc.hotel_booking.modules.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogResponse {

    @Schema(description = "ID bản ghi audit log", example = "1")
    Long id;

    @Schema(description = "ID nhân viên thao tác", example = "1")
    Integer staffId;

    @Schema(description = "Tên đăng nhập nhân viên", example = "admin_hanoi")
    String staffUsername;

    @Schema(description = "Họ tên nhân viên", example = "Nguyễn Văn A")
    String staffFullName;

    @Schema(description = "Loại hành động", example = "CREATE")
    String actionType;

    @Schema(description = "Tên thực thể", example = "bookings")
    String entityName;

    @Schema(description = "ID thực thể", example = "101")
    String entityId;

    @Schema(description = "Dữ liệu trước thay đổi (JSON string)")
    String oldValues;

    @Schema(description = "Dữ liệu sau thay đổi (JSON string)")
    String newValues;

    @Schema(description = "Địa chỉ IP", example = "192.168.1.100")
    String ipAddress;

    @Schema(description = "Trình duyệt / Thiết bị")
    String userAgent;

    @Schema(description = "Thời gian ghi nhận")
    OffsetDateTime createdAt;
}
