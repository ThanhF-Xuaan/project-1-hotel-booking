package vn.edu.utc.hotel_booking.pricing.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.inventory.api.StayCriteria;
import vn.edu.utc.hotel_booking.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.organization.service.HotelCatalogService;
import vn.edu.utc.hotel_booking.pricing.repository.PricingRuleRepository;
import vn.edu.utc.hotel_booking.pricing.repository.PricingRuleRepository.DatedRule;
import vn.edu.utc.hotel_booking.pricing.repository.PricingRuleRepository.DiscountRule;
import vn.edu.utc.hotel_booking.pricing.repository.PricingRuleRepository.PricingRule;
import vn.edu.utc.hotel_booking.pricing.repository.PricingRuleRepository.SurchargeRule;
import vn.edu.utc.hotel_booking.property.service.RoomTypeCatalogService;
import vn.edu.utc.hotel_booking.property.service.RoomTypeCatalogService.RoomTypeInfo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PricingQuoteService {
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final RoomTypeCatalogService roomTypes;
    private final HotelCatalogService hotels;
    private final RoomAvailabilityService availability;
    private final PricingRuleRepository rules;
    private final PriceCalculator calculator;
    private final ObjectMapper mapper;
    private final Clock clock;

    public PricingQuoteService(RoomTypeCatalogService roomTypes, HotelCatalogService hotels,
                               RoomAvailabilityService availability, PricingRuleRepository rules,
                               PriceCalculator calculator, ObjectMapper mapper, Clock clock) {
        this.roomTypes = roomTypes;
        this.hotels = hotels;
        this.availability = availability;
        this.rules = rules;
        this.calculator = calculator;
        this.mapper = mapper;
        this.clock = clock;
    }

    public Quote quote(int hotelRoomTypeId, StayCriteria stay) {
        RoomTypeInfo type = roomTypes.find(hotelRoomTypeId);
        stay.validateCapacity(type);
        var hotel = hotels.find(type.hotelId());
        int availableRooms = availability.availability(type.id(), stay).availableRooms();
        var vatRules = rules.vatRules(type.taxCategoryId(), stay.checkIn(), stay.checkOut());
        var pricingRules = rules.pricingRules(type.id(), stay.checkIn(), stay.checkOut());
        var discountRules = rules.discountRules(type.id(), stay.checkIn(), stay.checkOut());
        var surchargeRules = rules.surchargeRules(type.id(), stay.checkIn(), stay.checkOut());
        List<DailyQuote> nights = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate day = stay.checkIn(); day.isBefore(stay.checkOut()); day = day.plusDays(1)) {
            var vat = activeOn(vatRules, day);
            if (vat.size() != 1) {
                throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
            }
            List<PriceCalculator.Adjustment> increases = new ArrayList<>();
            for (PricingRule rule : activeOn(pricingRules, day)) {
                if (applies(rule, day)) {
                    increases.add(new PriceCalculator.Adjustment(rule.adjustmentType(), rule.value()));
                }
            }
            PriceCalculator.Adjustment discount = chooseDiscount(activeOn(discountRules, day), stay);
            BigDecimal surcharge = extraPersonSurcharge(type, activeOn(surchargeRules, day), stay);
            var amounts = calculator.calculate(type.basePrice(), increases, discount, surcharge,
                    hotel.serviceFeePercent(), vat.getFirst());
            nights.add(new DailyQuote(day, text(amounts.basePrice()), text(amounts.adjustmentAmount()),
                    text(amounts.discountAmount()), text(amounts.surchargeAmount()),
                    text(amounts.serviceFeeAmount()), text(vat.getFirst()),
                    text(amounts.vatAmount()), text(amounts.netPrice())));
            total = total.add(amounts.netPrice());
        }
        return new Quote(type.id(), stay.checkIn(), stay.checkOut(), availableRooms, "VND",
                text(total), Instant.now(clock), List.copyOf(nights));
    }

    private boolean applies(PricingRule rule, LocalDate day) {
        return switch (rule.ruleType()) {
            case "HOLIDAY" -> {
                if (rule.holidayDate() == null) throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
                yield rule.holidayDate().equals(day);
            }
            case "WEEKEND" -> day.getDayOfWeek() == DayOfWeek.FRIDAY || day.getDayOfWeek() == DayOfWeek.SATURDAY;
            case "PEAK_SEASON" -> true;
            default -> throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
        };
    }

    private <T> List<T> activeOn(List<DatedRule<T>> candidates, LocalDate day) {
        return candidates.stream().filter(rule -> rule.activeOn(day)).map(DatedRule::rule).toList();
    }

    private PriceCalculator.Adjustment chooseDiscount(List<DiscountRule> candidates, StayCriteria stay) {
        for (DiscountRule rule : candidates) {
            JsonNode conditions = conditions(rule.conditions());
            boolean applies = switch (rule.ruleType()) {
                case "LONG_STAY" -> {
                    expectKeys(conditions, Set.of("minNights"));
                    int minNights = requiredPositiveInt(conditions, "minNights");
                    yield ChronoUnit.DAYS.between(stay.checkIn(), stay.checkOut()) >= minNights;
                }
                case "EARLY_BIRD" -> {
                    expectKeys(conditions, Set.of("minAdvanceBookingDays"));
                    int advanceDays = requiredPositiveInt(conditions, "minAdvanceBookingDays");
                    yield ChronoUnit.DAYS.between(LocalDate.now(clock.withZone(HOTEL_ZONE)), stay.checkIn())
                            >= advanceDays;
                }
                case "SPECIAL_CAMPAIGN" -> {
                    expectKeys(conditions, Set.of("promoCode"));
                    yield !conditions.has("promoCode");
                }
                case "LAST_MINUTE" -> {
                    expectKeys(conditions, Set.of());
                    yield stay.checkIn().equals(LocalDate.now(clock.withZone(HOTEL_ZONE)));
                }
                default -> throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
            };
            if (applies) {
                return new PriceCalculator.Adjustment(rule.adjustmentType(), rule.value());
            }
        }
        return null;
    }

    private BigDecimal extraPersonSurcharge(RoomTypeInfo type, List<SurchargeRule> candidates, StayCriteria stay) {
        BigDecimal result = BigDecimal.ZERO;
        for (SurchargeRule rule : candidates) {
            if (!rule.ruleType().equals("EXTRA_PERSON")) {
                if (!Set.of("EXTRA_BED", "EARLY_CHECKIN", "LATE_CHECKOUT").contains(rule.ruleType())) {
                    throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
                }
                continue; // Those services are not requested by a standard room quote.
            }
            expectKeys(conditions(rule.conditions()), Set.of());
            if (!rule.pricingType().equals("PER_NIGHT")) {
                throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
            }
            if (rule.guestType() == null) throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
            int extra = switch (rule.guestType()) {
                case "ADULT" -> Math.max(0, stay.adults() - type.standardAdults());
                case "CHILD" -> Math.max(0, stay.children() - type.standardChildren());
                default -> throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
            };
            result = result.add(calculator.amount(type.basePrice(),
                    new PriceCalculator.Adjustment(rule.adjustmentType(), rule.value()))
                    .multiply(BigDecimal.valueOf(extra)));
        }
        return result;
    }

    private JsonNode conditions(String json) {
        try {
            JsonNode parsed = mapper.readTree(json);
            if (!parsed.isObject()) throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
            return parsed;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.PRICING_UNAVAILABLE, exception);
        }
    }

    private void expectKeys(JsonNode node, Set<String> allowed) {
        for (String name : node.propertyNames()) {
            if (!allowed.contains(name)) throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
        }
    }

    private int requiredPositiveInt(JsonNode node, String name) {
        if (!node.has(name) || !node.get(name).isInt() || node.get(name).asInt() < 1) {
            throw new AppException(ErrorCode.PRICING_UNAVAILABLE);
        }
        return node.get(name).asInt();
    }

    private String text(BigDecimal money) {
        return money.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public record Quote(int hotelRoomTypeId, LocalDate checkIn, LocalDate checkOut,
                        int availableRooms, String currency, String totalAmount, Instant pricedAt,
                        List<DailyQuote> nights) {}
    public record DailyQuote(LocalDate stayDate, String basePrice, String adjustmentAmount,
                             String discountAmount, String surchargeAmount, String serviceFeeAmount,
                             String vatPercent, String vatAmount, String netPrice) {}
}
