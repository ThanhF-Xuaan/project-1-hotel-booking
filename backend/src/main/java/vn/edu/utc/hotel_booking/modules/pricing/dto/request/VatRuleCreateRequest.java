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
public class VatRuleCreateRequest {

    @NotNull(message = "ID nhóm thuế không được để trống")
    @Schema(description = "ID nhóm thuế liên kết", example = "1")
    Integer taxCategoryId;

    @NotBlank(message = "Mã quy tắc thuế VAT không được để trống")
    @Size(min = 3, max = 50, message = "Mã quy tắc VAT từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã quy tắc VAT chỉ gồm chữ hoa, số và dấu gạch dưới")
    @Schema(description = "Mã quy tắc thuế VAT", example = "VAT_ROOM_8")
    String vatCode;

    @NotBlank(message = "Tên quy tắc thuế VAT không được để trống")
    @Size(max = 150, message = "Tên quy tắc VAT tối đa 150 ký tự")
    @Schema(description = "Tên quy tắc thuế VAT", example = "Thuế GTGT dịch vụ phòng 8% (Nghị định 72/2024)")
    String vatName;

    @NotNull(message = "Tỷ lệ thuế VAT không được để trống")
    @DecimalMin(value = "0.00", message = "Thuế suất VAT tối thiểu là 0%")
    @DecimalMax(value = "100.00", message = "Thuế suất VAT tối đa là 100%")
    @Digits(integer = 3, fraction = 2, message = "Thuế suất có tối đa 2 chữ số thập phân")
    @Schema(description = "Tỷ lệ thuế VAT (%)", example = "8.00")
    BigDecimal vatPercent;

    @NotNull(message = "Ngày bắt đầu áp dụng không được để trống")
    @Schema(description = "Ngày bắt đầu áp dụng", example = "2026-01-01")
    LocalDate startDate;

    @Schema(description = "Ngày kết thúc áp dụng (để trống nếu không có thời hạn)", example = "2026-06-30")
    LocalDate endDate;
}
