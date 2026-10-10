package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.ServicePricingType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SelectedServiceRequest {

    @NotNull(message = "ID dịch vụ không được để trống")
    @Schema(description = "ID dịch vụ / món trong menu", example = "101")
    Integer serviceId;

    @Schema(description = "Tên dịch vụ (optional, backend tự động lấy từ Menu nếu để trống)", example = "Trang trí Honeymoon")
    String serviceName;

    @Schema(description = "Loại tính giá dịch vụ (optional, backend tự động lấy từ Service catalog nếu để trống)", example = "PER_STAY")
    ServicePricingType pricingType;

    @Schema(description = "Đơn giá dịch vụ (optional, backend tự động lấy theo Menu nếu để trống)", example = "500000.00")
    BigDecimal unitPrice;

    @Schema(description = "Số lượng yêu cầu", example = "1")
    @Builder.Default
    Integer quantity = 1;
}
