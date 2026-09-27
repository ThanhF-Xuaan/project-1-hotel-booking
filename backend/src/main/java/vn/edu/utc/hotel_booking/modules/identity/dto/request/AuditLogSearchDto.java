package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

import java.time.OffsetDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogSearchDto extends BaseSearchDto {

    @Schema(description = "ID nhân viên thực hiện thao tác", example = "1")
    Integer staffId;

    @Schema(description = "Loại hành động: CREATE, UPDATE, DELETE, LOGIN, APPROVE, REFUND", example = "CREATE")
    String actionType;

    @Schema(description = "Tên bảng hoặc thực thể bị tác động", example = "bookings")
    String entityName;

    @Schema(description = "ID bản ghi bị tác động", example = "101")
    String entityId;

    @Schema(description = "Thời gian bắt đầu lọc")
    OffsetDateTime fromDate;

    @Schema(description = "Thời gian kết thúc lọc")
    OffsetDateTime toDate;
}
