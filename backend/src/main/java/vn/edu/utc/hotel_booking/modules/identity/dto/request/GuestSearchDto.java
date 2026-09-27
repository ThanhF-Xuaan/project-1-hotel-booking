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
public class GuestSearchDto extends BaseSearchDto {

    @Schema(description = "Từ khóa tìm kiếm (số điện thoại, email, số CCCD/hộ chiếu)", example = "0912345678")
    String keyword;

    @Schema(description = "Loại giấy tờ tùy thân (CCCD, PASSPORT...)", example = "CCCD")
    String identityType;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
