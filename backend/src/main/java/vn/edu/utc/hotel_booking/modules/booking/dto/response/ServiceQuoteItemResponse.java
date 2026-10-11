package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceQuoteItemResponse {

    @Schema(description = "ID menu dịch vụ", example = "10")
    Integer menuId;

    @Schema(description = "Tên dịch vụ", example = "Dịch vụ đón tiễn sân bay")
    String name;

    @Schema(description = "Đơn giá dịch vụ", example = "350000.00")
    BigDecimal unitPrice;

    @Schema(description = "Số lượng", example = "1")
    Integer quantity;

    @Schema(description = "Thành tiền trước thuế phí", example = "350000.00")
    BigDecimal subtotal;

    @Schema(description = "Phí phục vụ (5%)", example = "17500.00")
    BigDecimal serviceFee;

    @Schema(description = "Thuế VAT", example = "36750.00")
    BigDecimal vatAmount;

    @Schema(description = "Tổng tiền dịch vụ đã gồm thuế & phí", example = "404250.00")
    BigDecimal totalAmount;
}
