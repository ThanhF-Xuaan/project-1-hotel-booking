package vn.edu.utc.hotel_booking.modules.booking.dto.policy;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancellationTierDto {

    @NotNull(message = "minDays không được để trống")
    @Min(value = 0, message = "minDays phải >= 0")
    Integer minDays;

    Integer maxDays;

    @NotNull(message = "penaltyPercent không được để trống")
    @DecimalMin(value = "0.00", message = "penaltyPercent phải >= 0.00")
    @DecimalMax(value = "100.00", message = "penaltyPercent phải <= 100.00")
    BigDecimal penaltyPercent;

    String description;
}
