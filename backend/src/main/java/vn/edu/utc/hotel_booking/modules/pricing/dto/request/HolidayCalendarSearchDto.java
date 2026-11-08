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
public class HolidayCalendarSearchDto extends BaseSearchDto {

    @Schema(description = "Từ khóa tìm kiếm theo tên hoặc mô tả", example = "Tết")
    String keyword;

    @Schema(description = "Năm diễn ra", example = "2026")
    Integer year;

    @Schema(description = "Tháng diễn ra", example = "1")
    Integer month;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}

