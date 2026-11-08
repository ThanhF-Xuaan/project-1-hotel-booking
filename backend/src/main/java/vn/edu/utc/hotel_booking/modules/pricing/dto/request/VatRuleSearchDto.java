package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VatRuleSearchDto extends BaseSearchDto {

    @Schema(description = "ID nhóm thuế", example = "1")
    Integer taxCategoryId;

    @Schema(description = "Từ khóa tìm kiếm (Mã hoặc Tên quy tắc VAT)", example = "ROOM")
    String keyword;

    @Schema(description = "Ngày đang có hiệu lực", example = "2026-03-15")
    LocalDate activeOnDate;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
