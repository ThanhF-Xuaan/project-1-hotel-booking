package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.AbstractPricingHandler;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.DailyRateContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PromotionHandler extends AbstractPricingHandler {

    private final DiscountRuleRepository discountRuleRepository;
    private final VoucherRepository voucherRepository;

    @Override
    public void handle(PricingContext context) {
        if (context.getRequest() == null || context.getHotelRoomType() == null) {
            executeNext(context);
            return;
        }

        // 1. Quét discount_rules tự động
        List<DiscountRule> activeDiscounts = Collections.emptyList();
        if (discountRuleRepository != null && context.getHotelRoomType().getId() != null
                && context.getRequest().getCheckInDate() != null && context.getRequest().getCheckOutDate() != null) {
            activeDiscounts = discountRuleRepository.findActiveDiscountsForRoomType(
                    context.getHotelRoomType().getId(),
                    context.getRequest().getCheckInDate(),
                    context.getRequest().getCheckOutDate()
            );
        }

        // 2. Tra cứu Voucher nếu khách có nhập/chọn mã
        String voucherCode = context.getRequest().getVoucherCode();
        Optional<Voucher> voucherOpt = Optional.empty();
        if (voucherRepository != null && voucherCode != null && !voucherCode.trim().isBlank()) {
            Short hotelId = context.getHotel() != null ? context.getHotel().getId() : null;
            voucherOpt = voucherRepository.findValidVoucher(voucherCode.trim(), hotelId, OffsetDateTime.now());
            voucherOpt.ifPresent(v -> {
                context.setAppliedVoucher(v);
                context.setAppliedPromotionName(v.getTitle());
            });
        }

        // 3. Tính toán cho từng đêm lưu trú
        for (DailyRateContext daily : context.getDailyRates()) {
            LocalDate date = daily.getDate();
            BigDecimal adjustedRate = daily.getAdjustedRate();

            // A. PHÂN GIẢI CHÍNH SÁCH TỰ ĐỘNG (Priority DESC -> Calculated Amount DESC)
            BigDecimal autoDiscountAmt = BigDecimal.ZERO;
            List<DiscountRule> matchingAutoRules = activeDiscounts.stream()
                    .filter(d -> !date.isBefore(d.getStartDate()) && !date.isAfter(d.getEndDate()))
                    .toList();

            if (!matchingAutoRules.isEmpty()) {
                Optional<DiscountRule> bestAutoRuleOpt = matchingAutoRules.stream().max(
                        Comparator.<DiscountRule, Integer>comparing(
                                d -> d.getRuleType() != null && d.getRuleType().getPriority() != null ? (int) d.getRuleType().getPriority() : 0
                        ).thenComparing(
                                d -> calculateDiscountAmount(d, adjustedRate)
                        )
                );

                if (bestAutoRuleOpt.isPresent()) {
                    DiscountRule bestAutoRule = bestAutoRuleOpt.get();
                    autoDiscountAmt = calculateDiscountAmount(bestAutoRule, adjustedRate);
                    daily.setAppliedAutoDiscountRuleCode(bestAutoRule.getRuleType() != null ? bestAutoRule.getRuleType().getCode() : "DISCOUNT");
                }
            }

            // B. TÍNH VOUCHER NGƯỜI DÙNG (CỘNG DỒN)
            BigDecimal voucherDiscountAmt = BigDecimal.ZERO;
            if (voucherOpt.isPresent()) {
                Voucher voucher = voucherOpt.get();
                daily.setAppliedVoucherCode(voucher.getVoucherCode());

                if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
                    voucherDiscountAmt = adjustedRate.multiply(voucher.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    if (voucher.getMaxDiscountAmount() != null && voucherDiscountAmt.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                        voucherDiscountAmt = voucher.getMaxDiscountAmount();
                    }
                } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
                    voucherDiscountAmt = voucher.getDiscountValue();
                }
            }

            // C. TỔNG HỢP GIẢM GIÁ
            BigDecimal totalDiscount = autoDiscountAmt.add(voucherDiscountAmt);
            if (totalDiscount.compareTo(adjustedRate) > 0) {
                totalDiscount = adjustedRate;
            }

            BigDecimal netRoomRate = adjustedRate.subtract(totalDiscount);

            daily.setAutoDiscountAmount(autoDiscountAmt);
            daily.setVoucherDiscountAmount(voucherDiscountAmt);
            daily.setTotalDiscountAmount(totalDiscount);
            daily.setDiscountAmount(totalDiscount);
            daily.setNetRoomRate(netRoomRate);
        }

        executeNext(context);
    }

    private BigDecimal calculateDiscountAmount(DiscountRule rule, BigDecimal base) {
        if ("PERCENT".equalsIgnoreCase(rule.getDiscountType())) {
            return base.multiply(rule.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return rule.getDiscountValue();
    }
}
