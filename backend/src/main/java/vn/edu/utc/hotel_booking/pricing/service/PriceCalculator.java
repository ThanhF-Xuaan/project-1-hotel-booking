package vn.edu.utc.hotel_booking.pricing.service;

import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PriceCalculator {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public DailyAmounts calculate(BigDecimal base, List<Adjustment> increases, Adjustment discount,
                                  BigDecimal surcharge, BigDecimal serviceFeePercent, BigDecimal vatPercent) {
        if (base.signum() < 0 || surcharge.signum() < 0) {
            throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
        }
        BigDecimal adjustment = increases.stream().map(rule -> amount(base, rule))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal beforeDiscount = base.add(adjustment);
        BigDecimal discountAmount = discount == null ? BigDecimal.ZERO
                : amount(beforeDiscount, discount).min(beforeDiscount);
        BigDecimal taxable = beforeDiscount.subtract(discountAmount).max(BigDecimal.ZERO).add(surcharge);
        BigDecimal serviceFee = percent(taxable, serviceFeePercent);
        BigDecimal vat = percent(taxable.add(serviceFee), vatPercent);
        return new DailyAmounts(base.setScale(2, RoundingMode.HALF_UP), adjustment,
                discountAmount, surcharge.setScale(2, RoundingMode.HALF_UP), serviceFee, vat,
                taxable.add(serviceFee).add(vat).setScale(2, RoundingMode.HALF_UP));
    }

    public BigDecimal amount(BigDecimal base, Adjustment rule) {
        if (rule.value().signum() < 0) {
            throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
        }
        return switch (rule.type()) {
            case "PERCENT" -> percent(base, rule.value());
            case "FIXED" -> rule.value().setScale(2, RoundingMode.HALF_UP);
            default -> throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
        };
    }

    private BigDecimal percent(BigDecimal base, BigDecimal rate) {
        if (rate.signum() < 0 || rate.compareTo(HUNDRED) > 0) {
            throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
        }
        return base.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public record Adjustment(String type, BigDecimal value) {}
    public record DailyAmounts(BigDecimal basePrice, BigDecimal adjustmentAmount,
                               BigDecimal discountAmount, BigDecimal surchargeAmount,
                               BigDecimal serviceFeeAmount, BigDecimal vatAmount, BigDecimal netPrice) {}
}
