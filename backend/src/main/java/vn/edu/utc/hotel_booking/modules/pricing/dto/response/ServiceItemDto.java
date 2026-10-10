package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.ServicePricingType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceItemDto {

    @Schema(description = "ID dịch vụ", example = "101")
    Integer serviceId;

    @Schema(description = "Tên dịch vụ", example = "Trang trí Honeymoon")
    String serviceName;

    @Schema(description = "Hình thức tính giá", example = "PER_STAY")
    ServicePricingType pricingType;

    @Schema(description = "Đơn giá dịch vụ", example = "500000.00")
    BigDecimal unitPrice;

    @Schema(description = "Số lượng", example = "1")
    Integer quantity;

    @Schema(description = "Tiền dịch vụ trước thuế & phí", example = "500000.00")
    BigDecimal subtotal;

    @Schema(description = "Phí dịch vụ 5%", example = "25000.00")
    BigDecimal serviceFeeAmount;

    @Schema(description = "Thuế suất VAT (%)", example = "10.00")
    BigDecimal vatPercent;

    @Schema(description = "Tiền thuế VAT", example = "52500.00")
    BigDecimal vatAmount;

    @Schema(description = "Tổng thành tiền dịch vụ sau thuế & phí", example = "577500.00")
    BigDecimal grossTotal;
}
