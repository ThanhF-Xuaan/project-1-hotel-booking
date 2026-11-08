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
import vn.edu.utc.hotel_booking.modules.pricing.service.HotelAgePolicyService;

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
    private final HotelAgePolicyService hotelAgePolicyService;

    @Override
    public void handle(PricingContext context) {
        if (context.getHotelRoomType() != null && context.getRequest() != null) {
            HotelRoomType roomType = context.getHotelRoomType();
            PriceCalculationRequest req = context.getRequest();

            Short hotelId = null;
            if (context.getHotel() != null && context.getHotel().getId() != null) {
                hotelId = context.getHotel().getId();
            } else if (roomType.getHotel() != null && roomType.getHotel().getId() != null) {
                hotelId = roomType.getHotel().getId();
            }

            // Phân giải số lượng khách thực tế dựa trên độ tuổi (Children Ages Resolution)
            int effectiveAdults = req.getAdults() != null ? req.getAdults() : 0;
            int effectiveChildren = req.getChildren() != null ? req.getChildren() : 0;
            int effectiveInfants = req.getInfants() != null ? req.getInfants() : 0;

            if (req.getChildrenAges() != null && !req.getChildrenAges().isEmpty() && hotelAgePolicyService != null) {
                int resolvedInfants = 0;
                int resolvedChildren = 0;
                int extraAdultsFromAge = 0;

                for (Integer age : req.getChildrenAges()) {
                    if (age != null) {
                        String guestType = hotelAgePolicyService.resolveGuestTypeByAge(hotelId, age);
                        if ("INFANT".equalsIgnoreCase(guestType)) {
                            resolvedInfants++;
                        } else if ("CHILD".equalsIgnoreCase(guestType)) {
                            resolvedChildren++;
                        } else if ("ADULT".equalsIgnoreCase(guestType)) {
                            extraAdultsFromAge++;
                        }
                    }
                }
                effectiveInfants = resolvedInfants;
                effectiveChildren = resolvedChildren;
                effectiveAdults += extraAdultsFromAge;
            }

            List<SurchargeRule> surchargeRules = Collections.emptyList();
            if (surchargeRuleRepository != null && roomType.getId() != null
                    && req.getCheckInDate() != null && req.getCheckOutDate() != null) {
                surchargeRules = surchargeRuleRepository.findActiveSurchargesForRoomType(
                        roomType.getId(), req.getCheckInDate(), req.getCheckOutDate());
            }

            for (DailyRateContext daily : context.getDailyRates()) {
                LocalDate date = daily.getDate();
                BigDecimal basePrice = daily.getBaseRate();
                BigDecimal dailySurcharge = BigDecimal.ZERO;

                BigDecimal extraAdultFee = BigDecimal.ZERO;
                // 1. Phụ thu người lớn thêm (Extra Adults)
                short standardAdults = roomType.getStandardAdults() != null ? roomType.getStandardAdults() : (short) 2;
                if (effectiveAdults > standardAdults) {
                    int extraAdultCount = effectiveAdults - standardAdults;
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
                // 2. Phụ thu trẻ em thêm (Extra Children)
                short standardChildren = roomType.getStandardChildren() != null ? roomType.getStandardChildren() : (short) 0;
                if (effectiveChildren > standardChildren) {
                    int extraChildCount = effectiveChildren - standardChildren;
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

                BigDecimal extraInfantFee = BigDecimal.ZERO;
                // 3. Phụ thu em bé sơ sinh (Extra Infants - nếu cơ sở có rule quy định riêng)
                if (effectiveInfants > 0) {
                    Optional<SurchargeRule> infantRuleOpt = surchargeRules.stream()
                            .filter(s -> "EXTRA_PERSON".equalsIgnoreCase(s.getRuleType())
                                    && s.getAgePolicy() != null && "INFANT".equalsIgnoreCase(s.getAgePolicy().getGuestType())
                                    && !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
                            .findFirst();

                    if (infantRuleOpt.isPresent()) {
                        SurchargeRule sr = infantRuleOpt.get();
                        BigDecimal fee = "PERCENT".equalsIgnoreCase(sr.getAdjustmentType())
                                ? basePrice.multiply(sr.getAdjustmentValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                                : sr.getAdjustmentValue();
                        extraInfantFee = fee.multiply(BigDecimal.valueOf(effectiveInfants));
                        dailySurcharge = dailySurcharge.add(extraInfantFee);
                    }
                }

                BigDecimal extraBedFee = BigDecimal.ZERO;
                // 4. Phụ thu giường phụ (Extra Beds)
                if (req.getExtraBeds() != null && req.getExtraBeds() > 0) {
                    Optional<SurchargeRule> bedRuleOpt = surchargeRules.stream()
                            .filter(s -> "EXTRA_BED".equalsIgnoreCase(s.getRuleType())
                                    && !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
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
                daily.setExtraInfantsSurcharge(extraInfantFee);
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
