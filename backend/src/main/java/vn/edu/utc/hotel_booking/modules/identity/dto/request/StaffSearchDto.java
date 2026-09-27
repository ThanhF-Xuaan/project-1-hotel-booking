package vn.edu.utc.hotel_booking.modules.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffSearchDto extends BaseSearchDto {

    @Schema(description = "Từ khóa tìm kiếm (username, họ tên, email, SĐT)", example = "Nguyễn")
    String keyword;

    @Schema(description = "Lọc theo ID vai trò", example = "3")
    Short roleId;

    @Schema(description = "Lọc theo ID phòng ban", example = "1")
    Short departmentId;

    @Schema(description = "Lọc theo phạm vi (CHAIN, REGION, PROPERTY)", example = "PROPERTY")
    String scopeType;

    @Schema(description = "Lọc theo ID của khách sạn hoặc vùng tương ứng phạm vi", example = "1")
    Integer scopeEntityId;

    @Schema(description = "Lọc theo trạng thái (ACTIVE, INACTIVE)", example = "ACTIVE")
    String status;
}
