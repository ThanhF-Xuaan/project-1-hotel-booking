package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.*;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TaxAndFeeHandler extends AbstractPricingHandler {

    private final VatRuleRepository vatRuleRepository;

    @Override
    public void handle(PricingContext context) {
        BigDecimal feeRate = context.getServiceFeePercent() != null ? context.getServiceFeePercent() : new BigDecimal("5.00");

        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalSurcharge = BigDecimal.ZERO;
        BigDecimal totalRateAdjustment = BigDecimal.ZERO;
        BigDecimal totalSeasonal = BigDecimal.ZERO;
        BigDecimal totalHoliday = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalRoomNet = BigDecimal.ZERO;
        BigDecimal totalRoomServiceFee = BigDecimal.ZERO;
        BigDecimal totalRoomVat = BigDecimal.ZERO;
        BigDecimal totalRoomGross = BigDecimal.ZERO;

        // 1. Tiền phòng từng đêm
        if (context.getDailyRates() != null) {
            for (DailyRateContext daily : context.getDailyRates()) {
                totalBase = totalBase.add(daily.getBaseRate() != null ? daily.getBaseRate() : BigDecimal.ZERO);
                totalSurcharge = totalSurcharge.add(daily.getSurchargeAmount() != null ? daily.getSurchargeAmount() : BigDecimal.ZERO);
                BigDecimal dailyAdj = daily.getRateAdjustmentAmount() != null ? daily.getRateAdjustmentAmount() : BigDecimal.ZERO;
                totalRateAdjustment = totalRateAdjustment.add(dailyAdj);
                totalSeasonal = totalSeasonal.add(daily.getSeasonalAdjustment() != null ? daily.getSeasonalAdjustment() : BigDecimal.ZERO);
                totalHoliday = totalHoliday.add(daily.getHolidayAdjustment() != null ? daily.getHolidayAdjustment() : BigDecimal.ZERO);
                totalDiscount = totalDiscount.add(daily.getTotalDiscountAmount() != null ? daily.getTotalDiscountAmount() : BigDecimal.ZERO);
                totalRoomNet = totalRoomNet.add(daily.getNetRoomRate() != null ? daily.getNetRoomRate() : BigDecimal.ZERO);

                // Phí dịch vụ 5%
                BigDecimal serviceFeeAmt = daily.getNetRoomRate().multiply(feeRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal rateWithFee = daily.getNetRoomRate().add(serviceFeeAmt);

                // Thuế VAT theo ngày
                BigDecimal vatPercent = BigDecimal.valueOf(10.00);
                if (daily.getTaxCategoryId() != null && vatRuleRepository != null && daily.getDate() != null) {
                    vatPercent = vatRuleRepository.findActiveVatRule(daily.getTaxCategoryId(), daily.getDate())
                            .map(VatRule::getVatPercent)
                            .orElse(BigDecimal.valueOf(10.00));
                }

                BigDecimal vatAmt = rateWithFee.multiply(vatPercent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal grossTotal = rateWithFee.add(vatAmt);

                daily.setServiceFeeRate(feeRate);
                daily.setServiceFeeAmount(serviceFeeAmt);
                daily.setRoomRateWithServiceFee(rateWithFee);
                daily.setVatPercent(vatPercent);
                daily.setVatAmount(vatAmt);
                daily.setGrossDailyTotal(grossTotal);

                totalRoomServiceFee = totalRoomServiceFee.add(serviceFeeAmt);
                totalRoomVat = totalRoomVat.add(vatAmt);
                totalRoomGross = totalRoomGross.add(grossTotal);
            }
        }

        // 2. Dịch vụ bán kèm
        BigDecimal totalServiceSubtotal = BigDecimal.ZERO;
        BigDecimal totalServiceFee = BigDecimal.ZERO;
        BigDecimal totalServiceVat = BigDecimal.ZERO;
        BigDecimal totalServiceGross = BigDecimal.ZERO;

        LocalDate refDate = (context.getDailyRates() != null && !context.getDailyRates().isEmpty())
                ? context.getDailyRates().get(0).getDate()
                : LocalDate.now();

        if (context.getServiceItems() != null) {
            for (ServiceItemContext svc : context.getServiceItems()) {
                totalServiceSubtotal = totalServiceSubtotal.add(svc.getSubtotal());

                BigDecimal svcFeeAmt = svc.getSubtotal().multiply(feeRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal subtotalWithFee = svc.getSubtotal().add(svcFeeAmt);

                BigDecimal vatPercent = BigDecimal.valueOf(10.00);
                if (svc.getTaxCategoryId() != null && vatRuleRepository != null) {
                    vatPercent = vatRuleRepository.findActiveVatRule(svc.getTaxCategoryId(), refDate)
                            .map(VatRule::getVatPercent)
                            .orElse(BigDecimal.valueOf(10.00));
                }

                BigDecimal vatAmt = subtotalWithFee.multiply(vatPercent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal grossTotal = subtotalWithFee.add(vatAmt);

                svc.setServiceFeeRate(feeRate);
                svc.setServiceFeeAmount(svcFeeAmt);
                svc.setSubtotalWithServiceFee(subtotalWithFee);
                svc.setVatPercent(vatPercent);
                svc.setVatAmount(vatAmt);
                svc.setGrossServiceTotal(grossTotal);

                totalServiceFee = totalServiceFee.add(svcFeeAmt);
                totalServiceVat = totalServiceVat.add(vatAmt);
                totalServiceGross = totalServiceGross.add(grossTotal);
            }
        }

        // 3. Grand Total
        BigDecimal grandTotal = totalRoomGross.add(totalServiceGross);

        PricingSummary summary = PricingSummary.builder()
                .totalBasePrice(totalBase)
                .totalSurchargeAmount(totalSurcharge)
                .totalRateAdjustmentAmount(totalRateAdjustment)
                .totalSeasonalAdjustment(totalSeasonal)
                .totalHolidayAdjustment(totalHoliday)
                .totalDiscountAmount(totalDiscount)
                .totalRoomNetAmount(totalRoomNet)
                .totalRoomServiceFee(totalRoomServiceFee)
                .totalRoomVat(totalRoomVat)
                .totalRoomGrossAmount(totalRoomGross)
                .totalServiceSubtotal(totalServiceSubtotal)
                .totalServiceFee(totalServiceFee)
                .totalServiceVat(totalServiceVat)
                .totalServiceGrossAmount(totalServiceGross)
                .grandTotal(grandTotal)
                .build();

        context.setSummary(summary);

        executeNext(context);
    }
}
