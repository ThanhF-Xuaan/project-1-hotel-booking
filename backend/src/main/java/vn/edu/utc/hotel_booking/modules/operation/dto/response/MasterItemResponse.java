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
public class MasterItemResponse {

    Integer id;
    String code;
    String name;
    String description;
    MenuType itemType;
    BigDecimal defaultBasePrice;
    Integer taxCategoryId;
    String taxCategoryName;
    ServicePricingType pricingType;
    String unit;
    String status;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}

