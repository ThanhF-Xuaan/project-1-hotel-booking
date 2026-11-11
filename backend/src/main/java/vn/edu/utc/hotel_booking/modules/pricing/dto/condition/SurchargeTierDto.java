package vn.edu.utc.hotel_booking.modules.pricing.dto.condition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SurchargeTierDto {

    @NotBlank(message = "Giờ bắt đầu không được để trống")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Giờ bắt đầu phải có định dạng HH:mm")
    @JsonProperty("from_time")
    @Schema(description = "Giờ bắt đầu khung bậc (HH:mm)", example = "06:00")
    String fromTime;

    @NotBlank(message = "Giờ kết thúc không được để trống")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Giờ kết thúc phải có định dạng HH:mm")
    @JsonProperty("to_time")
    @Schema(description = "Giờ kết thúc khung bậc (HH:mm)", example = "10:00")
    String toTime;

    @NotBlank(message = "Kiểu điều chỉnh không được để trống")
    @Pattern(regexp = "PERCENT|FIXED", message = "Kiểu điều chỉnh phải là PERCENT hoặc FIXED")
    @JsonProperty("adjustment_type")
    @Schema(description = "Kiểu điều chỉnh: PERCENT hoặc FIXED", example = "PERCENT")
    String adjustmentType;

    @NotNull(message = "Giá trị phụ thu không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá trị phụ thu không được âm")
    @JsonProperty("adjustment_value")
    @Schema(description = "Giá trị phụ thu (% hoặc VNĐ)", example = "50.0")
    BigDecimal adjustmentValue;

    @JsonProperty("description")
    @Schema(description = "Mô tả bậc phụ thu", example = "Nhận phòng từ 06:00 đến 10:00 (50% giá phòng)")
    String description;
}
