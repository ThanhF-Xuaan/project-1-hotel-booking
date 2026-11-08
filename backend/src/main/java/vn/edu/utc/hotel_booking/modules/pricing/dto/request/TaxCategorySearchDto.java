package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaxCategorySearchDto extends BaseSearchDto {

    @Schema(description = "Từ khóa tìm kiếm (Mã hoặc Tên nhóm thuế)", example = "ROOM")
    String keyword;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}

