package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MasterItemUpdateRequest {

    @Size(max = 255, message = "Tên danh mục tối đa 255 ký tự")
    String name;

    String description;

    @DecimalMin(value = "0.0", message = "Đơn giá đề xuất không được âm")
    BigDecimal defaultBasePrice;

    Integer taxCategoryId;

    ServicePricingType pricingType;

    @Size(max = 50, message = "Đơn vị tính tối đa 50 ký tự")
    String unit;

    String status;
}

