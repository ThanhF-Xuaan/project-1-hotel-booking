package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MenuCreateRequest {

    Short hotelId;

    @NotNull(message = "Nhóm thuế không được để trống")
    Integer taxCategoryId;

    @NotNull(message = "Loại thực đơn không được để trống (PRODUCT hoặc SERVICE)")
    MenuType menuType;

    @NotBlank(message = "Tên món ăn / dịch vụ không được để trống")
    String name;

    String description;

    @NotNull(message = "Đơn giá cơ bản không được để trống")
    @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
    BigDecimal basePrice;

    // Dành cho PRODUCT
    @Builder.Default
    Integer stockQuantity = 0;

    // Dành cho SERVICE
    @Builder.Default
    ServicePricingType pricingType = ServicePricingType.PER_STAY;
}
