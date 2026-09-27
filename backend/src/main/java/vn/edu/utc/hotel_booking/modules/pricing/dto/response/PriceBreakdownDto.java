package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceBreakdownDto {

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "Tên loại phòng", example = "Phòng Deluxe Hướng Biển")
    String roomTypeName;

    @Schema(description = "Tổng số đêm lưu trú", example = "3")
    Integer totalNights;

    @Schema(description = "Chi tiết tính giá từng đêm")
    List<DailyPriceDto> dailyPrices;

    @Schema(description = "Tổng giá gốc các đêm", example = "3600000.00")
    BigDecimal totalBasePrice;

    @Schema(description = "Tổng số tiền khuyến mại giảm giá", example = "360000.00")
    BigDecimal totalDiscountAmount;

    @Schema(description = "Tổng số tiền phụ thu", example = "450000.00")
    BigDecimal totalSurchargeAmount;

    @Schema(description = "Tổng tiền trước thuế (Pre-tax)", example = "3690000.00")
    BigDecimal preTaxAmount;

    @Schema(description = "Tỷ lệ thuế VAT áp dụng (%)", example = "10.00")
    BigDecimal vatPercent;

    @Schema(description = "Tiền thuế VAT", example = "369000.00")
    BigDecimal vatAmount;

    @Schema(description = "Tổng số tiền thanh toán cuối cùng (đã gồm thuế & phí)", example = "4059000.00")
    BigDecimal finalTotalAmount;
}
