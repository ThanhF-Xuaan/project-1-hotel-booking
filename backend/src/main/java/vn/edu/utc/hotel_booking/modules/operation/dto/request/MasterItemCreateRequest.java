package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class MasterItemCreateRequest {

    @NotBlank(message = "Mã danh mục gốc không được để trống")
    @Size(max = 100, message = "Mã danh mục tối đa 100 ký tự")
    String code;

    @NotBlank(message = "Tên danh mục gốc không được để trống")
    @Size(max = 255, message = "Tên danh mục tối đa 255 ký tự")
    String name;

    String description;

    @NotNull(message = "Loại danh mục không được để trống (PRODUCT hoặc SERVICE)")
    MenuType itemType;

    @NotNull(message = "Đơn giá cơ bản đề xuất không được để trống")
    @DecimalMin(value = "0.0", message = "Đơn giá đề xuất không được âm")
    BigDecimal defaultBasePrice;

    @NotNull(message = "Nhóm thuế không được để trống")
    Integer taxCategoryId;

    ServicePricingType pricingType;

    @Size(max = 50, message = "Đơn vị tính tối đa 50 ký tự")
    String unit;

    @Builder.Default
    String status = "ACTIVE";
}
