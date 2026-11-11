package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricingRuleTypeCreateRequest {

    @NotBlank(message = "Mã loại quy tắc không được để trống")
    @Size(max = 50, message = "Mã loại quy tắc tối đa 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã loại quy tắc chỉ gồm chữ in hoa, số và dấu gạch dưới (VD: PEAK_SEASON)")
    @Schema(description = "Mã loại quy tắc duy nhất", example = "PEAK_SEASON")
    String code;

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 150, message = "Tên hiển thị tối đa 150 ký tự")
    @Schema(description = "Tên hiển thị mô tả", example = "Mùa cao điểm hè")
    String displayName;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    @Schema(description = "Mô tả chi tiết loại quy tắc giá", example = "Áp dụng phụ thu cho mùa cao điểm du lịch")
    String description;

    @NotNull(message = "Độ ưu tiên không được để trống")
    @Schema(description = "Độ ưu tiên áp dụng (số càng lớn ưu tiên càng cao)", example = "80")
    Short priority;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
