package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
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
        assertThat(night1.getRateAfterSurcharge()).isEqualByComparingTo(new BigDecimal("1200000.00"));

        assertThat(night2.getSurchargeAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(night2.getRateAfterSurcharge()).isEqualByComparingTo(new BigDecimal("1200000.00"));
    }
}
