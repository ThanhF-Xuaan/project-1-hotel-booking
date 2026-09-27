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
public class CompanySearchDto extends BaseSearchDto {

    @Schema(description = "Từ khóa tìm kiếm (tên công ty, MST, người liên hệ, SĐT)", example = "Viettel")
    String keyword;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
