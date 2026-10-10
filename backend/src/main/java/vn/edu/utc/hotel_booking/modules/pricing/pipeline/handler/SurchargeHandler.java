package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.AbstractPricingHandler;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.DailyRateContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.repository.SurchargeRuleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SurchargeHandler extends AbstractPricingHandler {

    private final SurchargeRuleRepository surchargeRuleRepository;

    @Override
    public void handle(PricingContext context) {
        if (context.getHotelRoomType() != null && context.getRequest() != null) {
            HotelRoomType roomType = context.getHotelRoomType();
            PriceCalculationRequest req = context.getRequest();

            List<SurchargeRule> surchargeRules = Collections.emptyList();
            if (surchargeRuleRepository != null && roomType.getId() != null && req.getCheckInDate() != null && req.getCheckOutDate() != null) {
                surchargeRules = surchargeRuleRepository.findActiveSurchargesForRoomType(
                        roomType.getId(), req.getCheckInDate(), req.getCheckOutDate());
            }

            for (DailyRateContext daily : context.getDailyRates()) {
                LocalDate date = daily.getDate();
                BigDecimal basePrice = daily.getBaseRate();
                BigDecimal dailySurcharge = BigDecimal.ZERO;

                // 1. Extra Adults
                if (req.getAdults() != null && req.getAdults() > roomType.getStandardAdults()) {
                    int extraAdultCount = req.getAdults() - roomType.getStandardAdults();
                    Optional<SurchargeRule> ruleOpt = surchargeRules.stream()
                            .filter(s -> "EXTRA_PERSON".equalsIgnoreCase(s.getRuleType()) && !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
                            .findFirst();

                    BigDecimal fee = ruleOpt.map(sr -> "PERCENT".equalsIgnoreCase(sr.getAdjustmentType())
                                    ? basePrice.multiply(sr.getAdjustmentValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                                    : sr.getAdjustmentValue())
                            .orElseGet(() -> new BigDecimal("200000.00")); // Fallback 200k / người

                    dailySurcharge = dailySurcharge.add(fee.multiply(BigDecimal.valueOf(extraAdultCount)));
                }

                // 2. Extra Beds
                if (req.getExtraBeds() != null && req.getExtraBeds() > 0) {
                    Optional<SurchargeRule> bedRuleOpt = surchargeRules.stream()
                            .filter(s -> "EXTRA_BED".equalsIgnoreCase(s.getRuleType()) && !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
                            .findFirst();

                    BigDecimal fee = bedRuleOpt.map(sr -> "PERCENT".equalsIgnoreCase(sr.getAdjustmentType())
                                    ? basePrice.multiply(sr.getAdjustmentValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                                    : sr.getAdjustmentValue())
                            .orElseGet(() -> basePrice.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP));

                    dailySurcharge = dailySurcharge.add(fee.multiply(BigDecimal.valueOf(req.getExtraBeds())));
                }

                BigDecimal rateAfterSurcharge = basePrice.add(dailySurcharge);
                daily.setSurchargeAmount(dailySurcharge);
                daily.setRateAfterSurcharge(rateAfterSurcharge);
                daily.setAdjustedRate(rateAfterSurcharge);
                daily.setNetRoomRate(rateAfterSurcharge);
            }
        }

        executeNext(context);
    }
}
