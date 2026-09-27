package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MenuUpdateRequest {

    Integer taxCategoryId;
    String name;
    String description;

    @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
    BigDecimal basePrice;

    String status;

    Integer stockQuantity;
    ServicePricingType pricingType;
}
