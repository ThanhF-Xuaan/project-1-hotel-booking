package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VatRuleUpdateRequest {

    @NotBlank(message = "Tên quy tắc thuế VAT không được để trống")
    @Size(max = 150, message = "Tên quy tắc VAT tối đa 150 ký tự")
    @Schema(description = "Tên quy tắc thuế VAT", example = "Thuế GTGT dịch vụ phòng 10%")
    String vatName;

    @NotNull(message = "Tỷ lệ thuế VAT không được để trống")
    @DecimalMin(value = "0.00", message = "Thuế suất VAT tối thiểu là 0%")
    @DecimalMax(value = "100.00", message = "Thuế suất VAT tối đa là 100%")
    @Digits(integer = 3, fraction = 2, message = "Thuế suất có tối đa 2 chữ số thập phân")
    @Schema(description = "Tỷ lệ thuế VAT (%)", example = "10.00")
    BigDecimal vatPercent;

    @NotNull(message = "Ngày bắt đầu áp dụng không được để trống")
    @Schema(description = "Ngày bắt đầu áp dụng", example = "2026-07-01")
    LocalDate startDate;

    @Schema(description = "Ngày kết thúc áp dụng")
    LocalDate endDate;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái chỉ nhận ACTIVE hoặc INACTIVE")
    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
