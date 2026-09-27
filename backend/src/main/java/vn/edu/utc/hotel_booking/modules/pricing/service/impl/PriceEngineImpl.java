package vn.edu.utc.hotel_booking.modules.pricing.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DailyPriceDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;
import vn.edu.utc.hotel_booking.modules.pricing.repository.*;
import vn.edu.utc.hotel_booking.modules.pricing.service.PriceEngine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class PriceEngineImpl implements PriceEngine {

    HotelRoomTypeRepository hotelRoomTypeRepository;
    PricingRuleRepository pricingRuleRepository;
    HolidayCalendarRepository holidayCalendarRepository;
    DiscountRuleRepository discountRuleRepository;
    SurchargeRuleRepository surchargeRuleRepository;
    VatRuleRepository vatRuleRepository;

    @Override
    public PriceBreakdownDto calculatePrice(PriceCalculationRequest request) {
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày check-out phải sau ngày check-in");
        }

        HotelRoomType hotelRoomType = hotelRoomTypeRepository.findByIdWithDetails(request.getHotelRoomTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND,
                        "Không tìm thấy cấu hình loại phòng id: " + request.getHotelRoomTypeId()));

        // Check capacity
        int totalGuests = request.getAdults() + request.getChildren();
        if (totalGuests > hotelRoomType.getMaxTotalGuests()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Tổng số khách (" + totalGuests + ") vượt quá sức chứa tối đa (" + hotelRoomType.getMaxTotalGuests() + ")");
        }
        if (request.getExtraBeds() > hotelRoomType.getExtraBeds()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA,
                    "Số giường phụ (" + request.getExtraBeds() + ") vượt quá số lượng cho phép (" + hotelRoomType.getExtraBeds() + ")");
        }

        // Fetch active rules for the entire booking range
        List<PricingRule> pricingRules = pricingRuleRepository.findActiveRulesForRoomType(
                hotelRoomType.getId(), request.getCheckInDate(), request.getCheckOutDate());

        List<DiscountRule> discountRules = discountRuleRepository.findActiveDiscountsForRoomType(
                hotelRoomType.getId(), request.getCheckInDate(), request.getCheckOutDate());

        List<SurchargeRule> surchargeRules = surchargeRuleRepository.findActiveSurchargesForRoomType(
                hotelRoomType.getId(), request.getCheckInDate(), request.getCheckOutDate());

        BigDecimal basePrice = hotelRoomType.getBasePrice();
        List<DailyPriceDto> dailyPrices = new ArrayList<>();
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalSurcharge = BigDecimal.ZERO;
        BigDecimal totalPreTax = BigDecimal.ZERO;

        LocalDate cur = request.getCheckInDate();
        while (cur.isBefore(request.getCheckOutDate())) {
            final LocalDate date = cur;

            // 1. Base rate
            totalBase = totalBase.add(basePrice);

            // 2. Pricing Rule adjustment (Seasonal or Holiday)
            BigDecimal seasonalAdj = BigDecimal.ZERO;
            BigDecimal holidayAdj = BigDecimal.ZERO;

            Optional<PricingRule> activeRuleOpt = pricingRules.stream()
                    .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
                    .findFirst();

            if (activeRuleOpt.isPresent()) {
                PricingRule rule = activeRuleOpt.get();
                BigDecimal adj = "PERCENT".equalsIgnoreCase(rule.getAdjustmentType())
                        ? basePrice.multiply(rule.getAdjustmentValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        : rule.getAdjustmentValue();

                if (rule.getHolidayCalendar() != null || "HOLIDAY".equalsIgnoreCase(rule.getRuleType().getCode())) {
                    holidayAdj = adj;
                } else {
                    seasonalAdj = adj;
                }
            }

            BigDecimal adjustedRate = basePrice.add(seasonalAdj).add(holidayAdj);

            // 3. Discount Rules
            BigDecimal dailyDiscount = BigDecimal.ZERO;
            Optional<DiscountRule> activeDiscountOpt = discountRules.stream()
                    .filter(d -> !date.isBefore(d.getStartDate()) && !date.isAfter(d.getEndDate()))
                    .findFirst();

            if (activeDiscountOpt.isPresent()) {
                DiscountRule d = activeDiscountOpt.get();
                dailyDiscount = "PERCENT".equalsIgnoreCase(d.getDiscountType())
                        ? adjustedRate.multiply(d.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        : d.getDiscountValue();
            }

            // 4. Surcharges (Extra Person, Extra Bed)
            BigDecimal dailySurcharge = BigDecimal.ZERO;

            // Extra Adults
            if (request.getAdults() > hotelRoomType.getStandardAdults()) {
                int extraAdultCount = request.getAdults() - hotelRoomType.getStandardAdults();
                Optional<SurchargeRule> personSurchargeRule = surchargeRules.stream()
                        .filter(s -> "EXTRA_PERSON".equalsIgnoreCase(s.getRuleType()) && !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
                        .findFirst();

                BigDecimal extraAdultFee;
                if (personSurchargeRule.isPresent()) {
                    SurchargeRule sr = personSurchargeRule.get();
                    extraAdultFee = "PERCENT".equalsIgnoreCase(sr.getAdjustmentType())
                            ? basePrice.multiply(sr.getAdjustmentValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                            : sr.getAdjustmentValue();
                } else {
                    // Default fallback 20% of basePrice per extra adult
                    extraAdultFee = basePrice.multiply(BigDecimal.valueOf(0.20)).setScale(2, RoundingMode.HALF_UP);
                }
                dailySurcharge = dailySurcharge.add(extraAdultFee.multiply(BigDecimal.valueOf(extraAdultCount)));
            }

            // Extra Bed
            if (request.getExtraBeds() > 0) {
                Optional<SurchargeRule> bedSurchargeRule = surchargeRules.stream()
                        .filter(s -> "EXTRA_BED".equalsIgnoreCase(s.getRuleType()) && !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
                        .findFirst();

                BigDecimal extraBedFee;
                if (bedSurchargeRule.isPresent()) {
                    SurchargeRule sr = bedSurchargeRule.get();
                    extraBedFee = "PERCENT".equalsIgnoreCase(sr.getAdjustmentType())
                            ? basePrice.multiply(sr.getAdjustmentValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                            : sr.getAdjustmentValue();
                } else {
                    // Default fallback 30% of basePrice per extra bed
                    extraBedFee = basePrice.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
                }
                dailySurcharge = dailySurcharge.add(extraBedFee.multiply(BigDecimal.valueOf(request.getExtraBeds())));
            }

            BigDecimal netAmount = adjustedRate.subtract(dailyDiscount).add(dailySurcharge);

            totalDiscount = totalDiscount.add(dailyDiscount);
            totalSurcharge = totalSurcharge.add(dailySurcharge);
            totalPreTax = totalPreTax.add(netAmount);

            dailyPrices.add(DailyPriceDto.builder()
                    .date(date)
                    .baseRate(basePrice)
                    .seasonalAdjustment(seasonalAdj)
                    .holidayAdjustment(holidayAdj)
                    .adjustedRate(adjustedRate)
                    .discountAmount(dailyDiscount)
                    .surchargeAmount(dailySurcharge)
                    .netAmount(netAmount)
                    .build());

            cur = cur.plusDays(1);
        }

        // 5. VAT Tax Calculation
        BigDecimal vatPercent = BigDecimal.valueOf(10.00); // default 10%
        Optional<VatRule> vatRuleOpt = vatRuleRepository.findActiveVatRule(
                hotelRoomType.getTaxCategoryId(), request.getCheckInDate());
        if (vatRuleOpt.isPresent()) {
            vatPercent = vatRuleOpt.get().getVatPercent();
        }

        BigDecimal vatAmount = totalPreTax.multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal finalTotal = totalPreTax.add(vatAmount);

        return PriceBreakdownDto.builder()
                .hotelRoomTypeId(hotelRoomType.getId())
                .roomTypeName(hotelRoomType.getRoomType().getName())
                .totalNights(dailyPrices.size())
                .dailyPrices(dailyPrices)
                .totalBasePrice(totalBase)
                .totalDiscountAmount(totalDiscount)
                .totalSurchargeAmount(totalSurcharge)
                .preTaxAmount(totalPreTax)
                .vatPercent(vatPercent)
                .vatAmount(vatAmount)
                .finalTotalAmount(finalTotal)
                .build();
    }
}
