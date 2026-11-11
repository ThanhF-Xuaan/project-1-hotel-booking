package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscountRuleUpdateRequest {

    @Schema(description = "ID chiến dịch khuyến mại (Bắt buộc nếu ruleType là SPECIAL_CAMPAIGN)", example = "1")
    Integer campaignId;

    @Schema(description = "Điều kiện áp dụng mở rộng (JSON chuỗi)", example = "{\"minNights\": 3}")
    String conditions;

    @NotBlank(message = "Loại chiết khấu không được để trống (PERCENT hoặc FIXED)")
    @Pattern(regexp = "^(PERCENT|FIXED)$", message = "Loại chiết khấu phải là PERCENT hoặc FIXED")
    @Schema(description = "Loại chiết khấu: PERCENT hoặc FIXED", example = "PERCENT")
    String discountType;

    @NotNull(message = "Giá trị chiết khấu không được để trống")
    @Positive(message = "Giá trị chiết khấu phải lớn hơn 0")
    @Schema(description = "Giá trị chiết khấu", example = "15.00")
    BigDecimal discountValue;

    @NotNull(message = "Ngày bắt đầu áp dụng không được để trống")
    @Schema(description = "Ngày bắt đầu áp dụng", example = "2026-06-01")
    LocalDate startDate;

    @NotNull(message = "Ngày kết thúc áp dụng không được để trống")
    @Schema(description = "Ngày kết thúc áp dụng", example = "2026-08-31")
    LocalDate endDate;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
