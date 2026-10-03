package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.MeterType;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UtilityMeterSearchDto extends BaseSearchDto {
    
    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;
    
    @Schema(description = "Loại đồng hồ", example = "ELECTRICITY")
    MeterType meterType;
    
    @Schema(description = "Mã đồng hồ", example = "EL-001")
    String keyword;
}
