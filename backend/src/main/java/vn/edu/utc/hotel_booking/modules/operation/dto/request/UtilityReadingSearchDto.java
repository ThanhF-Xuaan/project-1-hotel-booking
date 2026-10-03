package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UtilityReadingSearchDto extends BaseSearchDto {
    
    @Schema(description = "ID đồng hồ", example = "1")
    Integer meterId;
    
    @Schema(description = "Từ ngày", example = "2023-01-01")
    LocalDate fromDate;
    
    @Schema(description = "Đến ngày", example = "2023-12-31")
    LocalDate toDate;
}
