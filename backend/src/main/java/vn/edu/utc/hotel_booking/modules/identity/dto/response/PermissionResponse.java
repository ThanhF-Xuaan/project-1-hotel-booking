package vn.edu.utc.hotel_booking.modules.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
