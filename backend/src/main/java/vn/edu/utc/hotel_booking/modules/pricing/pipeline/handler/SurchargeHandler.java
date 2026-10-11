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

                BigDecimal extraAdultFee = BigDecimal.ZERO;
                // 1. Extra Adults
                if (req.getAdults() != null && roomType.getStandardAdults() != null && req.getAdults() > roomType.getStandardAdults()) {
                    int extraAdultCount = req.getAdults() - roomType.getStandardAdults();
                    Optional<SurchargeRule> adultRuleOpt = surchargeRules.stream()
                            .filter(s -> "EXTRA_PERSON".equalsIgnoreCase(s.getRuleType())
                                    && (s.getAgePolicy() == null || "ADULT".equalsIgnoreCase(s.getAgePolicy().getGuestType()))
                                    && !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
                            .findFirst();

                    if (adultRuleOpt.isPresent()) {
                        SurchargeRule sr = adultRuleOpt.get();
                        BigDecimal fee = "PERCENT".equalsIgnoreCase(sr.getAdjustmentType())
                                ? basePrice.multiply(sr.getAdjustmentValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                                : sr.getAdjustmentValue();
                        extraAdultFee = fee.multiply(BigDecimal.valueOf(extraAdultCount));
                        dailySurcharge = dailySurcharge.add(extraAdultFee);
                    }
                }

                BigDecimal extraChildFee = BigDecimal.ZERO;
                // 2. Extra Children
                short standardChildren = roomType.getStandardChildren() != null ? roomType.getStandardChildren() : (short) 0;
                if (req.getChildren() != null && req.getChildren() > standardChildren) {
                    int extraChildCount = req.getChildren() - standardChildren;
                    Optional<SurchargeRule> childRuleOpt = surchargeRules.stream()
                            .filter(s -> "EXTRA_PERSON".equalsIgnoreCase(s.getRuleType())
                                    && s.getAgePolicy() != null && "CHILD".equalsIgnoreCase(s.getAgePolicy().getGuestType())
                                    && !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
                            .findFirst();

                    if (childRuleOpt.isPresent()) {
                        SurchargeRule sr = childRuleOpt.get();
                        BigDecimal fee = "PERCENT".equalsIgnoreCase(sr.getAdjustmentType())
                                ? basePrice.multiply(sr.getAdjustmentValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                                : sr.getAdjustmentValue();
                        extraChildFee = fee.multiply(BigDecimal.valueOf(extraChildCount));
                        dailySurcharge = dailySurcharge.add(extraChildFee);
                    }
                }

                BigDecimal extraBedFee = BigDecimal.ZERO;
                // 3. Extra Beds
                if (req.getExtraBeds() != null && req.getExtraBeds() > 0) {
                    Optional<SurchargeRule> bedRuleOpt = surchargeRules.stream()
                            .filter(s -> "EXTRA_BED".equalsIgnoreCase(s.getRuleType()) && !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
                            .findFirst();

                    if (bedRuleOpt.isPresent()) {
                        SurchargeRule sr = bedRuleOpt.get();
                        BigDecimal fee = "PERCENT".equalsIgnoreCase(sr.getAdjustmentType())
                                ? basePrice.multiply(sr.getAdjustmentValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                                : sr.getAdjustmentValue();
                        extraBedFee = fee.multiply(BigDecimal.valueOf(req.getExtraBeds()));
                        dailySurcharge = dailySurcharge.add(extraBedFee);
                    }
                }

                BigDecimal rateAfterSurcharge = basePrice.add(dailySurcharge);
                daily.setExtraAdultsSurcharge(extraAdultFee);
                daily.setExtraChildrenSurcharge(extraChildFee);
                daily.setExtraBedsSurcharge(extraBedFee);
                daily.setSurchargeAmount(dailySurcharge);
                daily.setRateAfterSurcharge(rateAfterSurcharge);
                daily.setAdjustedRate(rateAfterSurcharge);
                daily.setNetRoomRate(rateAfterSurcharge);
            }
        }

        executeNext(context);
    }
}
