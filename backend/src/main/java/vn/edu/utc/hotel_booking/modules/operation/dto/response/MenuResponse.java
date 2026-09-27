package vn.edu.utc.hotel_booking.modules.operation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MenuResponse {

    Integer id;
    Short hotelId;
    String hotelName;
    Integer taxCategoryId;
    String taxCategoryName;
    MenuType menuType;
    String name;
    String description;
    BigDecimal basePrice;
    String status;

    // Riêng cho PRODUCT
    Integer stockQuantity;

    // Riêng cho SERVICE
    ServicePricingType pricingType;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
