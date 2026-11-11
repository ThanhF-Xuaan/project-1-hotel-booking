package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscountRuleTypeUpdateRequest {

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 150, message = "Tên hiển thị tối đa 150 ký tự")
    @Schema(description = "Tên hiển thị mô tả", example = "Chiến dịch đặc biệt (Flash Sale)")
    String displayName;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    @Schema(description = "Mô tả chi tiết loại quy tắc giảm giá", example = "Áp dụng giảm giá theo chiến dịch khuyến mãi đặc biệt")
    String description;

    @NotNull(message = "Độ ưu tiên không được để trống")
    @Schema(description = "Độ ưu tiên áp dụng (số càng lớn ưu tiên càng cao)", example = "90")
    Short priority;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
