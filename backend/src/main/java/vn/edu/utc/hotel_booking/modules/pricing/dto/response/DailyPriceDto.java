package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DailyPriceDto {

    @Schema(description = "Ngày lưu trú", example = "2026-12-31")
    LocalDate date;

    @Schema(description = "Giá cơ bản của loại phòng", example = "1000000.00")
    BigDecimal baseRate;

    @Schema(description = "Số tiền phụ thu (người thêm, giường phụ)", example = "200000.00")
    BigDecimal surchargeAmount;

    @Schema(description = "Giá cơ sở sau phụ thu", example = "1200000.00")
    BigDecimal rateAfterSurcharge;

    @Schema(description = "Tổng số tiền điều chỉnh tăng giá (mùa vụ, cuối tuần, lễ tết)", example = "240000.00")
    BigDecimal rateAdjustmentAmount;

    @Schema(description = "Danh sách chi tiết các quy tắc điều chỉnh giá áp dụng trong đêm")
    List<AppliedAdjustmentDto> appliedAdjustments;

    @Schema(description = "Điều chỉnh theo mùa vụ (backward compatible)", example = "0.00")
    BigDecimal seasonalAdjustment;

    @Schema(description = "Điều chỉnh theo ngày lễ/tết (backward compatible)", example = "240000.00")
    BigDecimal holidayAdjustment;

    @Schema(description = "Giá phòng sau điều chỉnh mùa vụ/lễ", example = "1440000.00")
    BigDecimal adjustedRate;

    @Schema(description = "Số tiền giảm giá từ chiến dịch tự động", example = "0.00")
    BigDecimal autoDiscountAmount;

    @Schema(description = "Số tiền giảm giá từ voucher người dùng", example = "144000.00")
    BigDecimal voucherDiscountAmount;

    @Schema(description = "Tổng số tiền giảm giá khuyến mại", example = "144000.00")
    BigDecimal discountAmount;

    @Schema(description = "Giá ròng tiền phòng trước phí & thuế của đêm này", example = "1296000.00")
    BigDecimal netAmount;

    @Schema(description = "Phí dịch vụ 5% cho đêm này", example = "64800.00")
    BigDecimal serviceFeeAmount;

    @Schema(description = "Giá phòng đã gồm phí dịch vụ", example = "1360800.00")
    BigDecimal roomRateWithServiceFee;

    @Schema(description = "Thuế suất VAT (%) cho đêm này", example = "8.00")
    BigDecimal vatPercent;

    @Schema(description = "Tiền thuế VAT cho đêm này", example = "108864.00")
    BigDecimal vatAmount;

    @Schema(description = "Tổng thành tiền sau thuế và phí của đêm này", example = "1469664.00")
    BigDecimal grossDailyTotal;
}
