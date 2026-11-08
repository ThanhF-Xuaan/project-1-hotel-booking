package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelAgePolicyUpdateRequest {

    @NotNull(message = "Độ tuổi tối thiểu không được để trống")
    @Min(value = 0, message = "Độ tuổi tối thiểu từ 0")
    @Max(value = 120, message = "Độ tuổi tối thiểu không vượt quá 120")
    @Schema(description = "Độ tuổi tối thiểu", example = "6")
    Short minAge;

    @NotNull(message = "Độ tuổi tối đa không được để trống")
    @Min(value = 0, message = "Độ tuổi tối đa từ 0")
    @Max(value = 120, message = "Độ tuổi tối đa không vượt quá 120")
    @Schema(description = "Độ tuổi tối đa", example = "12")
    Short maxAge;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái chỉ nhận ACTIVE hoặc INACTIVE")
    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
