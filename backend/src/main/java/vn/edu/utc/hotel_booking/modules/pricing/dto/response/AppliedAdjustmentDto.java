package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppliedAdjustmentDto {

    @Schema(description = "Mã quy tắc điều chỉnh giá", example = "HOLIDAY")
    String ruleCode;

    @Schema(description = "Tên hiển thị quy tắc", example = "Phụ thu ngày Lễ/Tết")
    String ruleName;

    @Schema(description = "Hình thức điều chỉnh (PERCENT hoặc FIXED)", example = "PERCENT")
    String adjustmentType;

    @Schema(description = "Giá trị điều chỉnh thiết lập", example = "20.00")
    BigDecimal adjustmentValue;

    @Schema(description = "Số tiền thực tế được điều chỉnh cộng vào giá phòng", example = "240000.00")
    BigDecimal appliedAmount;
}
