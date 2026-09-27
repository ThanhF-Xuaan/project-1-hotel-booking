package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DailyPriceDto {

    @Schema(description = "Ngày lưu trú", example = "2026-10-01")
    LocalDate date;

    @Schema(description = "Giá cơ bản của loại phòng", example = "1200000.00")
    BigDecimal baseRate;

    @Schema(description = "Điều chỉnh theo mùa vụ", example = "100000.00")
    BigDecimal seasonalAdjustment;

    @Schema(description = "Điều chỉnh theo ngày lễ/tết", example = "0.00")
    BigDecimal holidayAdjustment;

    @Schema(description = "Giá phòng sau điều chỉnh quy tắc", example = "1300000.00")
    BigDecimal adjustedRate;

    @Schema(description = "Số tiền giảm giá khuyến mại", example = "130000.00")
    BigDecimal discountAmount;

    @Schema(description = "Số tiền phụ thu (người thêm, giường phụ)", example = "150000.00")
    BigDecimal surchargeAmount;

    @Schema(description = "Giá ròng trước thuế của đêm này", example = "1320000.00")
    BigDecimal netAmount;
}
