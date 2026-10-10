package vn.edu.utc.hotel_booking.modules.pricing.pipeline;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DailyRateContext {
    LocalDate date;
    BigDecimal baseRate;
    BigDecimal surchargeAmount;
    BigDecimal rateAfterSurcharge;

    // Dynamic Rate Adjustment Resolution (Itemized Modifiers)
    @Builder.Default
    BigDecimal rateAdjustmentAmount = BigDecimal.ZERO;
    @Builder.Default
    List<AppliedAdjustmentContext> appliedAdjustments = new ArrayList<>();

    // Backward-compatibility fields
    String appliedSeasonalityRuleCode;
    @Builder.Default
    BigDecimal seasonalAdjustment = BigDecimal.ZERO;
    @Builder.Default
    BigDecimal holidayAdjustment = BigDecimal.ZERO;
    BigDecimal adjustedRate;

    // Auto Promotion Resolution Info
    String appliedAutoDiscountRuleCode;
    @Builder.Default
    BigDecimal autoDiscountAmount = BigDecimal.ZERO;

    // User Voucher Info
    String appliedVoucherCode;
    @Builder.Default
    BigDecimal voucherDiscountAmount = BigDecimal.ZERO;

    // Total Discount = Auto + Voucher
    @Builder.Default
    BigDecimal totalDiscountAmount = BigDecimal.ZERO;
    @Builder.Default
    BigDecimal discountAmount = BigDecimal.ZERO; // Tương thích DTO

    BigDecimal netRoomRate;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    BigDecimal roomRateWithServiceFee;
    Integer taxCategoryId;
    BigDecimal vatPercent;
    BigDecimal vatAmount;
    BigDecimal grossDailyTotal;

    public void addAppliedAdjustment(AppliedAdjustmentContext adjustment) {
        if (this.appliedAdjustments == null) {
            this.appliedAdjustments = new ArrayList<>();
        }
        this.appliedAdjustments.add(adjustment);
    }
}
