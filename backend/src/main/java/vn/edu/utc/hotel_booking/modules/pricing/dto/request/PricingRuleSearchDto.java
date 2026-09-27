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
public class PricingRuleSearchDto extends BaseSearchDto {

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "Loại quy tắc", example = "SEASONAL")
    String ruleTypeCode;

    @Schema(description = "Tra cứu ngày trong khoảng", example = "2026-07-01")
    LocalDate activeOnDate;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
