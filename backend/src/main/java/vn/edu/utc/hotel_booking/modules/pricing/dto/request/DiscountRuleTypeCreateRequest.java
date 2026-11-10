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
public class DiscountRuleTypeCreateRequest {

    @NotBlank(message = "Mã loại giảm giá không được để trống")
    @Size(max = 50, message = "Mã loại giảm giá tối đa 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã loại giảm giá chỉ gồm chữ in hoa, số và dấu gạch dưới (VD: SPECIAL_CAMPAIGN)")
    @Schema(description = "Mã loại giảm giá duy nhất", example = "SPECIAL_CAMPAIGN")
    String code;

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 150, message = "Tên hiển thị tối đa 150 ký tự")
    @Schema(description = "Tên hiển thị mô tả", example = "Chiến dịch đặc biệt (Flash Sale)")
    String displayName;

    @NotNull(message = "Độ ưu tiên không được để trống")
    @Schema(description = "Độ ưu tiên áp dụng (số càng lớn ưu tiên càng cao)", example = "90")
    Short priority;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
