package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HolidayCalendar;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.DailyRateContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonalityHandlerTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    private SeasonalityHandler seasonalityHandler;

    @BeforeEach
    void setUp() {
        seasonalityHandler = new SeasonalityHandler(pricingRuleRepository);
    }

    @Test
    @DisplayName("Should apply +20% holiday rate modifier on top of rateAfterSurcharge (1.200.000 * 20% = +240.000 -> 1.440.000)")
    void shouldApplyHolidayModifier() {
        // Arrange
        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);

        PricingRuleType holidayType = PricingRuleType.builder()
                .code("HOLIDAY")
                .displayName("Holiday Adjustment")
                .priority((short) 10)
                .build();

        PricingRule holidayRule = PricingRule.builder()
                .id(1)
                .ruleType(holidayType)
                .holidayCalendar(HolidayCalendar.builder().id(1).name("New Year").build())
                .adjustmentType("PERCENT")
                .adjustmentValue(new BigDecimal("20.00")) // +20%
                .startDate(LocalDate.of(2026, 12, 31))
                .endDate(LocalDate.of(2027, 1, 2))
                .status("ACTIVE")
                .build();

        when(pricingRuleRepository.findActiveRulesForRoomType(eq(1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(holidayRule));

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 2))
                .build();

        DailyRateContext night1 = DailyRateContext.builder()
                .date(LocalDate.of(2026, 12, 31))
                .baseRate(new BigDecimal("1000000.00"))
                .rateAfterSurcharge(new BigDecimal("1200000.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .dailyRates(List.of(night1))
                .build();

        // Act
        seasonalityHandler.handle(context);

        // Assert: 1.200.000 * 20% = +240.000 -> adjustedRate = 1.440.000
        assertThat(night1.getHolidayAdjustment()).isEqualByComparingTo(new BigDecimal("240000.00"));
        assertThat(night1.getRateAdjustmentAmount()).isEqualByComparingTo(new BigDecimal("240000.00"));
        assertThat(night1.getAdjustedRate()).isEqualByComparingTo(new BigDecimal("1440000.00"));
        assertThat(night1.getAppliedAdjustments()).hasSize(1);
        assertThat(night1.getAppliedAdjustments().get(0).getRuleCode()).isEqualTo("HOLIDAY");
        assertThat(night1.getAppliedAdjustments().get(0).getRuleName()).isEqualTo("New Year");
        assertThat(night1.getAppliedAdjustments().get(0).getAppliedAmount()).isEqualByComparingTo(new BigDecimal("240000.00"));
    }

    @Test
    @DisplayName("Should dynamically support WEEKEND rate adjustment without hardcoded fields")
    void shouldApplyWeekendModifierDynamically() {
        // Arrange
        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);

        PricingRuleType weekendType = PricingRuleType.builder()
                .code("WEEKEND")
                .displayName("Phụ thu Cuối tuần")
                .priority((short) 5)
                .build();

        PricingRule weekendRule = PricingRule.builder()
                .id(10)
                .ruleType(weekendType)
                .adjustmentType("PERCENT")
                .adjustmentValue(new BigDecimal("15.00")) // +15%
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 31))
                .status("ACTIVE")
                .build();

        when(pricingRuleRepository.findActiveRulesForRoomType(eq(1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(weekendRule));

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 10, 10))
                .checkOutDate(LocalDate.of(2026, 10, 11))
                .build();

        DailyRateContext night = DailyRateContext.builder()
                .date(LocalDate.of(2026, 10, 10))
                .baseRate(new BigDecimal("1000000.00"))
                .rateAfterSurcharge(new BigDecimal("1000000.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .dailyRates(List.of(night))
                .build();

        // Act
        seasonalityHandler.handle(context);

        // Assert: 1.000.000 * 15% = +150.000 -> 1.150.000
        assertThat(night.getRateAdjustmentAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(night.getAdjustedRate()).isEqualByComparingTo(new BigDecimal("1150000.00"));
        assertThat(night.getAppliedAdjustments()).hasSize(1);
        var adj = night.getAppliedAdjustments().get(0);
        assertThat(adj.getRuleCode()).isEqualTo("WEEKEND");
        assertThat(adj.getRuleName()).isEqualTo("Phụ thu Cuối tuần");
        assertThat(adj.getAdjustmentType()).isEqualTo("PERCENT");
        assertThat(adj.getAppliedAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));
    }

    @Test
    @DisplayName("Should resolve two-tier priority (Priority DESC, Amount DESC) when multiple rules match")
    void shouldResolveModifierPriority() {
        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);

        // Rule 1: Priority 5, +10%
        PricingRuleType weekendType = PricingRuleType.builder().code("WEEKEND").displayName("Weekend").priority((short) 5).build();
        PricingRule rule1 = PricingRule.builder()
                .id(1)
                .ruleType(weekendType)
                .adjustmentType("PERCENT")
                .adjustmentValue(new BigDecimal("10.00"))
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2027, 1, 31))
                .build();

        // Rule 2: Priority 10, +20% (Higher priority should win)
        PricingRuleType holidayType = PricingRuleType.builder().code("HOLIDAY").displayName("Holiday").priority((short) 10).build();
        PricingRule rule2 = PricingRule.builder()
                .id(2)
                .ruleType(holidayType)
                .adjustmentType("PERCENT")
                .adjustmentValue(new BigDecimal("20.00"))
                .startDate(LocalDate.of(2026, 12, 31))
                .endDate(LocalDate.of(2027, 1, 2))
                .build();

        when(pricingRuleRepository.findActiveRulesForRoomType(eq(1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(rule1, rule2));

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 1))
                .build();

        DailyRateContext night = DailyRateContext.builder()
                .date(LocalDate.of(2026, 12, 31))
                .rateAfterSurcharge(new BigDecimal("1200000.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .dailyRates(List.of(night))
                .build();

        seasonalityHandler.handle(context);

        // Rule 2 wins
        assertThat(night.getHolidayAdjustment()).isEqualByComparingTo(new BigDecimal("240000.00"));
        assertThat(night.getRateAdjustmentAmount()).isEqualByComparingTo(new BigDecimal("240000.00"));
        assertThat(night.getAdjustedRate()).isEqualByComparingTo(new BigDecimal("1440000.00"));
        assertThat(night.getAppliedAdjustments()).hasSize(1);
        assertThat(night.getAppliedAdjustments().get(0).getRuleCode()).isEqualTo("HOLIDAY");
    }
}
