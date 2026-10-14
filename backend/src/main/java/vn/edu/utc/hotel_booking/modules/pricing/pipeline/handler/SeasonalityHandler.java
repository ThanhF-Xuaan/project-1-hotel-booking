package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRule;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.AbstractPricingHandler;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.AppliedAdjustmentContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.DailyRateContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SeasonalityHandler extends AbstractPricingHandler {

    private final PricingRuleRepository pricingRuleRepository;

    @Override
    public void handle(PricingContext context) {
        if (context.getHotelRoomType() != null && context.getRequest() != null) {
            List<PricingRule> activeRules = Collections.emptyList();
            if (pricingRuleRepository != null && context.getHotelRoomType().getId() != null
                    && context.getRequest().getCheckInDate() != null && context.getRequest().getCheckOutDate() != null) {
                activeRules = pricingRuleRepository.findActiveRulesForRoomType(
                        context.getHotelRoomType().getId(),
                        context.getRequest().getCheckInDate(),
                        context.getRequest().getCheckOutDate()
                );
            }

            for (DailyRateContext daily : context.getDailyRates()) {
                LocalDate date = daily.getDate();
                BigDecimal baseForSeason = daily.getRateAfterSurcharge() != null ? daily.getRateAfterSurcharge() : daily.getBaseRate();

                List<PricingRule> matchingRules = activeRules.stream()
                        .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
                        .toList();

                if (!matchingRules.isEmpty()) {
                    Optional<PricingRule> bestRuleOpt = matchingRules.stream().max(
                            Comparator.<PricingRule, Integer>comparing(
                                    r -> r.getRuleType() != null && r.getRuleType().getPriority() != null ? (int) r.getRuleType().getPriority() : 0
                            ).thenComparing(
                                    r -> calculateAdjustmentAmount(r, baseForSeason)
                            )
                    );

                    if (bestRuleOpt.isPresent()) {
                        PricingRule bestRule = bestRuleOpt.get();
                        BigDecimal adj = calculateAdjustmentAmount(bestRule, baseForSeason);
                        String ruleCode = bestRule.getRuleType() != null ? bestRule.getRuleType().getCode() : "SEASONAL";
                        String ruleName = bestRule.getRuleType() != null ? bestRule.getRuleType().getDisplayName() : "Điều chỉnh giá";
                        if (bestRule.getHolidayCalendar() != null && bestRule.getHolidayCalendar().getName() != null) {
                            ruleName = bestRule.getHolidayCalendar().getName();
                        }

                        daily.setAppliedSeasonalityRuleCode(ruleCode);
                        daily.setRateAdjustmentAmount(adj);

                        // Thêm vào danh sách chi tiết động (Itemized Modifiers)
                        AppliedAdjustmentContext adjustmentContext = AppliedAdjustmentContext.builder()
                                .ruleCode(ruleCode)
                                .ruleName(ruleName)
                                .adjustmentType(bestRule.getAdjustmentType())
                                .adjustmentValue(bestRule.getAdjustmentValue())
                                .appliedAmount(adj)
                                .build();
                        daily.addAppliedAdjustment(adjustmentContext);

                        // Backward-compatibility: Phân loại seasonal / holiday
                        if (bestRule.getHolidayCalendar() != null || "HOLIDAY".equalsIgnoreCase(ruleCode)) {
                            daily.setHolidayAdjustment(adj);
                            daily.setSeasonalAdjustment(BigDecimal.ZERO);
                        } else {
                            daily.setSeasonalAdjustment(adj);
                            daily.setHolidayAdjustment(BigDecimal.ZERO);
                        }
                    }
                }

                BigDecimal totalAdj = daily.getRateAdjustmentAmount() != null && daily.getRateAdjustmentAmount().compareTo(BigDecimal.ZERO) > 0
                        ? daily.getRateAdjustmentAmount()
                        : (daily.getSeasonalAdjustment() != null ? daily.getSeasonalAdjustment() : BigDecimal.ZERO)
                                .add(daily.getHolidayAdjustment() != null ? daily.getHolidayAdjustment() : BigDecimal.ZERO);

                BigDecimal adjustedRate = baseForSeason.add(totalAdj);

                daily.setRateAdjustmentAmount(totalAdj);
                daily.setAdjustedRate(adjustedRate);
                daily.setNetRoomRate(adjustedRate);
            }
        }

        executeNext(context);
    }

    private BigDecimal calculateAdjustmentAmount(PricingRule rule, BigDecimal base) {
        if ("PERCENT".equalsIgnoreCase(rule.getAdjustmentType())) {
            return base.multiply(rule.getAdjustmentValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return rule.getAdjustmentValue();
    }
}
