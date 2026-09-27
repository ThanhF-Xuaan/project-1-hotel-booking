package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricingRuleUpdateRequest {

    @Schema(description = "ID ngày lễ tết (nếu áp dụng theo ngày lễ)", example = "1")
    Integer holidayCalendarId;

    @NotBlank(message = "Loại điều chỉnh không được để trống (PERCENT, FIXED)")
    @Schema(description = "Loại điều chỉnh: PERCENT hoặc FIXED", example = "PERCENT")
    String adjustmentType;

    @NotNull(message = "Giá trị điều chỉnh không được để trống")
    @Schema(description = "Giá trị điều chỉnh (+/-)", example = "20.00")
    BigDecimal adjustmentValue;

    @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
    @Schema(description = "Ngày bắt đầu áp dụng", example = "2026-06-01")
    LocalDate startDate;

    @NotNull(message = "Ngày kết thúc hiệu lực không được để trống")
    @Schema(description = "Ngày kết thúc áp dụng", example = "2026-08-31")
    LocalDate endDate;

    @Schema(description = "Trạng thái quy tắc", example = "ACTIVE")
    String status;
}
