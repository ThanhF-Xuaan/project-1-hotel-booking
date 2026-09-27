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
public class CampaignSearchDto extends BaseSearchDto {

    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "Tên chiến dịch", example = "Hè")
    String name;

    @Schema(description = "Ngày đang diễn ra chiến dịch", example = "2026-07-01")
    LocalDate activeOnDate;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
