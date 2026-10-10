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

    @Schema(description = "Tổng số đêm lưu trú", example = "2")
    Integer totalNights;

    @Schema(description = "Mã voucher đã áp dụng (nếu có)", example = "NEWYEAR10")
    String appliedVoucherCode;

    @Schema(description = "Tên chương trình khuyến mãi/Voucher áp dụng", example = "Ưu đãi Tết Dương Lịch 10%")
    String appliedPromotionName;

    @Schema(description = "Chi tiết tính giá từng đêm phòng")
    List<DailyPriceDto> dailyPrices;

    @Schema(description = "Chi tiết các dịch vụ bán kèm")
    List<ServiceItemDto> serviceItems;

    @Schema(description = "Tổng giá gốc các đêm phòng", example = "2000000.00")
    BigDecimal totalBasePrice;

    @Schema(description = "Tổng số tiền phụ thu tiền phòng", example = "400000.00")
    BigDecimal totalSurchargeAmount;

    @Schema(description = "Tổng điều chỉnh tăng giá (mùa vụ, cuối tuần, lễ tết)", example = "480000.00")
    BigDecimal totalRateAdjustmentAmount;

    @Schema(description = "Tổng điều chỉnh mùa vụ/lễ (backward compatible)", example = "480000.00")
    BigDecimal totalSeasonalAdjustment;

    @Schema(description = "Tổng số tiền khuyến mại giảm giá tiền phòng", example = "288000.00")
    BigDecimal totalDiscountAmount;

    @Schema(description = "Tổng tiền phòng net trước thuế & phí", example = "2592000.00")
    BigDecimal totalRoomNetAmount;

    @Schema(description = "Tổng phí dịch vụ tiền phòng (5%)", example = "129600.00")
    BigDecimal totalRoomServiceFee;

    @Schema(description = "Tổng thuế VAT tiền phòng", example = "244944.00")
    BigDecimal totalRoomVat;

    @Schema(description = "Tổng tiền phòng sau thuế & phí", example = "2966544.00")
    BigDecimal totalRoomGrossAmount;

    @Schema(description = "Tổng tiền dịch vụ trước thuế & phí", example = "2300000.00")
    BigDecimal totalServiceSubtotal;

    @Schema(description = "Tổng phí dịch vụ cho dịch vụ (5%)", example = "115000.00")
    BigDecimal totalServiceFee;

    @Schema(description = "Tổng thuế VAT cho dịch vụ (10%)", example = "241500.00")
    BigDecimal totalServiceVat;

    @Schema(description = "Tổng tiền dịch vụ sau thuế & phí", example = "2656500.00")
    BigDecimal totalServiceGrossAmount;

    @Schema(description = "Tiền phòng trước thuế & phí (backward compatible)", example = "2592000.00")
    BigDecimal preTaxAmount;

    @Schema(description = "Thuế suất VAT (%) (backward compatible)", example = "10.00")
    BigDecimal vatPercent;

    @Schema(description = "Tổng thuế VAT (backward compatible)", example = "244944.00")
    BigDecimal vatAmount;

    @Schema(description = "Tổng tiền thanh toán cuối cùng (Grand Total)", example = "5623044.00")
    BigDecimal finalTotalAmount;
}
