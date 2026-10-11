package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HotelAgePolicy;
import vn.edu.utc.hotel_booking.modules.pricing.entity.SurchargeRule;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.DailyRateContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.repository.SurchargeRuleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurchargeHandlerTest {

    @Mock
    private SurchargeRuleRepository surchargeRuleRepository;

    private SurchargeHandler surchargeHandler;

    @BeforeEach
    void setUp() {
        surchargeHandler = new SurchargeHandler(surchargeRuleRepository);
    }

    @Test
    @DisplayName("Should apply extra adult surcharge when occupancy exceeds base standard (2 adults standard, 3 booked -> +200.000/night)")
    void shouldApplyExtraAdultSurcharge() {
        // Arrange
        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);
        roomType.setBasePrice(new BigDecimal("1000000.00"));
        roomType.setStandardAdults((short) 2);
        roomType.setMaxAdults((short) 4);

        SurchargeRule extraAdultRule = SurchargeRule.builder()
                .id(1)
                .ruleType("EXTRA_PERSON")
                .adjustmentType("FIXED")
                .adjustmentValue(new BigDecimal("200000.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2027, 12, 31))
                .build();

        when(surchargeRuleRepository.findActiveSurchargesForRoomType(eq(1), any(), any()))
                .thenReturn(List.of(extraAdultRule));

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 2))
                .adults((short) 3)
                .children((short) 0)
                .build();

        DailyRateContext night1 = DailyRateContext.builder()
                .date(LocalDate.of(2026, 12, 31))
                .baseRate(new BigDecimal("1000000.00"))
                .build();
        DailyRateContext night2 = DailyRateContext.builder()
                .date(LocalDate.of(2027, 1, 1))
                .baseRate(new BigDecimal("1000000.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .dailyRates(List.of(night1, night2))
                .build();

        // Act
        surchargeHandler.handle(context);

        // Assert
        assertThat(night1.getSurchargeAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(night1.getExtraAdultsSurcharge()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(night1.getRateAfterSurcharge()).isEqualByComparingTo(new BigDecimal("1200000.00"));

        assertThat(night2.getSurchargeAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(night2.getExtraAdultsSurcharge()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(night2.getRateAfterSurcharge()).isEqualByComparingTo(new BigDecimal("1200000.00"));
    }

    @Test
    @DisplayName("Should apply extra child surcharge when exceeding standard children (standard 0, requested 1 -> +100.000/night)")
    void shouldApplyExtraChildSurcharge() {
        // Arrange
        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);
        roomType.setBasePrice(new BigDecimal("1000000.00"));
        roomType.setStandardAdults((short) 2);
        roomType.setStandardChildren((short) 0);
        roomType.setMaxChildren((short) 2);

        HotelAgePolicy childPolicy = HotelAgePolicy.builder()
                .guestType("CHILD")
                .build();

        SurchargeRule extraChildRule = SurchargeRule.builder()
                .id(2)
                .ruleType("EXTRA_PERSON")
                .agePolicy(childPolicy)
                .adjustmentType("FIXED")
                .adjustmentValue(new BigDecimal("150000.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2027, 12, 31))
                .build();

        when(surchargeRuleRepository.findActiveSurchargesForRoomType(eq(1), any(), any()))
                .thenReturn(List.of(extraChildRule));

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 1))
                .adults((short) 2)
                .children((short) 1)
                .build();

        DailyRateContext night1 = DailyRateContext.builder()
                .date(LocalDate.of(2026, 12, 31))
                .baseRate(new BigDecimal("1000000.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .dailyRates(List.of(night1))
                .build();

        // Act
        surchargeHandler.handle(context);

        // Assert
        assertThat(night1.getExtraChildrenSurcharge()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(night1.getExtraAdultsSurcharge()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(night1.getSurchargeAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(night1.getRateAfterSurcharge()).isEqualByComparingTo(new BigDecimal("1150000.00"));
    }

    @Test
    @DisplayName("Should not surcharge for infants (0 VND) even when infants are present")
    void shouldNotSurchargeForInfants() {
        // Arrange
        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);
        roomType.setBasePrice(new BigDecimal("1000000.00"));
        roomType.setStandardAdults((short) 2);
        roomType.setStandardChildren((short) 0);

        when(surchargeRuleRepository.findActiveSurchargesForRoomType(eq(1), any(), any()))
                .thenReturn(Collections.emptyList());

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 1))
                .adults((short) 2)
                .children((short) 0)
                .infants((short) 2)
                .build();

        DailyRateContext night1 = DailyRateContext.builder()
                .date(LocalDate.of(2026, 12, 31))
                .baseRate(new BigDecimal("1000000.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .dailyRates(List.of(night1))
                .build();

        // Act
        surchargeHandler.handle(context);

        // Assert
        assertThat(night1.getSurchargeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(night1.getRateAfterSurcharge()).isEqualByComparingTo(new BigDecimal("1000000.00"));
    }

    @Test
    @DisplayName("Should apply combined surcharges for extra adults, extra children, and extra bed")
    void shouldApplyCombinedSurcharges() {
        // Arrange
        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);
        roomType.setBasePrice(new BigDecimal("1000000.00"));
        roomType.setStandardAdults((short) 2);
        roomType.setStandardChildren((short) 0);

        HotelAgePolicy adultPolicy = HotelAgePolicy.builder().guestType("ADULT").build();
        HotelAgePolicy childPolicy = HotelAgePolicy.builder().guestType("CHILD").build();

        SurchargeRule adultRule = SurchargeRule.builder()
                .id(1)
                .ruleType("EXTRA_PERSON")
                .agePolicy(adultPolicy)
                .adjustmentType("FIXED")
                .adjustmentValue(new BigDecimal("200000.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2027, 12, 31))
                .build();

        SurchargeRule childRule = SurchargeRule.builder()
                .id(2)
                .ruleType("EXTRA_PERSON")
                .agePolicy(childPolicy)
                .adjustmentType("FIXED")
                .adjustmentValue(new BigDecimal("100000.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2027, 12, 31))
                .build();

        SurchargeRule bedRule = SurchargeRule.builder()
                .id(3)
                .ruleType("EXTRA_BED")
                .adjustmentType("FIXED")
                .adjustmentValue(new BigDecimal("300000.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2027, 12, 31))
                .build();

        when(surchargeRuleRepository.findActiveSurchargesForRoomType(eq(1), any(), any()))
                .thenReturn(List.of(adultRule, childRule, bedRule));

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 1))
                .adults((short) 3)     // 1 extra adult -> 200k
                .children((short) 1)   // 1 extra child -> 100k
                .infants((short) 1)    // 1 infant -> free (0k)
                .extraBeds((short) 1)  // 1 extra bed -> 300k
                .build();

        DailyRateContext night1 = DailyRateContext.builder()
                .date(LocalDate.of(2026, 12, 31))
                .baseRate(new BigDecimal("1000000.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .dailyRates(List.of(night1))
                .build();

        // Act
        surchargeHandler.handle(context);

        // Assert
        assertThat(night1.getExtraAdultsSurcharge()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(night1.getExtraChildrenSurcharge()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(night1.getExtraBedsSurcharge()).isEqualByComparingTo(new BigDecimal("300000.00"));
        assertThat(night1.getSurchargeAmount()).isEqualByComparingTo(new BigDecimal("600000.00"));
        assertThat(night1.getRateAfterSurcharge()).isEqualByComparingTo(new BigDecimal("1600000.00"));
    }

    @Test
    @DisplayName("Should not apply any surcharge when exceeding standard occupancy if no SurchargeRule is configured (Zero Fallback)")
    void shouldNotApplySurchargeWhenNoRuleConfigured() {
        // Arrange
        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);
        roomType.setBasePrice(new BigDecimal("1000000.00"));
        roomType.setStandardAdults((short) 2);
        roomType.setStandardChildren((short) 0);

        when(surchargeRuleRepository.findActiveSurchargesForRoomType(eq(1), any(), any()))
                .thenReturn(Collections.emptyList());

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 1))
                .adults((short) 3)     // 1 extra adult but no rule
                .children((short) 1)   // 1 extra child but no rule
                .extraBeds((short) 1)  // 1 extra bed but no rule
                .build();

        DailyRateContext night1 = DailyRateContext.builder()
                .date(LocalDate.of(2026, 12, 31))
                .baseRate(new BigDecimal("1000000.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .dailyRates(List.of(night1))
                .build();

        // Act
        surchargeHandler.handle(context);

        // Assert: Không áp bất kỳ fallback cứng nào, surcharge phải hoàn toàn bằng 0
        assertThat(night1.getExtraAdultsSurcharge()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(night1.getExtraChildrenSurcharge()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(night1.getExtraBedsSurcharge()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(night1.getSurchargeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(night1.getRateAfterSurcharge()).isEqualByComparingTo(new BigDecimal("1000000.00"));
    }
}
