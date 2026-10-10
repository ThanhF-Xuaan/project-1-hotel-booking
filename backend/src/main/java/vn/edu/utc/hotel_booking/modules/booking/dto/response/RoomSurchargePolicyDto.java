package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSurchargePolicyDto {

    @Schema(description = "Loại phụ thu (EXTRA_PERSON hoặc EXTRA_BED)", example = "EXTRA_PERSON")
    String ruleType;

    @Schema(description = "Cách tính phí (PER_NIGHT hoặc PER_STAY)", example = "PER_NIGHT")
    String pricingType;

    @Schema(description = "Loại điều chỉnh (FIXED hoặc PERCENT)", example = "FIXED")
    String adjustmentType;

    @Schema(description = "Giá trị phụ thu", example = "200000.00")
    BigDecimal adjustmentValue;

    @Schema(description = "Mô tả thân thiện cho khách", example = "Phụ thu thêm người: 200,000 VND / đêm")
    String formattedDescription;

    @Schema(description = "Điều kiện chi tiết nếu có", example = "{}")
    String conditions;
}
