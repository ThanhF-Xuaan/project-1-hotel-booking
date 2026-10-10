package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRule;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.DailyRateContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VoucherRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionHandlerTest {

    @Mock
    private DiscountRuleRepository discountRuleRepository;

    @Mock
    private VoucherRepository voucherRepository;

    private PromotionHandler promotionHandler;

    @BeforeEach
    void setUp() {
        promotionHandler = new PromotionHandler(discountRuleRepository, voucherRepository);
    }

    @Test
    @DisplayName("Should apply user voucher (NEWYEAR10: -10%) on adjustedRate (1.440.000 -> 1.296.000)")
    void shouldApplyUserVoucher() {
        // Arrange
        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);

        Voucher voucher = Voucher.builder()
                .id(100)
                .voucherCode("NEWYEAR10")
                .title("New Year 10% Off")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("10.00")) // 10%
                .validFrom(OffsetDateTime.now().minusDays(1))
                .validTo(OffsetDateTime.now().plusDays(30))
                .status("ACTIVE")
                .build();

        when(voucherRepository.findValidVoucher(eq("NEWYEAR10"), any(), any())).thenReturn(Optional.of(voucher));
        when(discountRuleRepository.findActiveDiscountsForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 2))
                .voucherCode("NEWYEAR10")
                .build();

        DailyRateContext night1 = DailyRateContext.builder()
                .date(LocalDate.of(2026, 12, 31))
                .baseRate(new BigDecimal("1000000.00"))
                .rateAfterSurcharge(new BigDecimal("1200000.00"))
                .holidayAdjustment(new BigDecimal("240000.00"))
                .adjustedRate(new BigDecimal("1440000.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .dailyRates(List.of(night1))
                .build();

        // Act
        promotionHandler.handle(context);

        // Assert: 1.440.000 * 10% = 144.000 -> netRoomRate = 1.296.000
        assertThat(night1.getVoucherDiscountAmount()).isEqualByComparingTo(new BigDecimal("144000.00"));
        assertThat(night1.getTotalDiscountAmount()).isEqualByComparingTo(new BigDecimal("144000.00"));
        assertThat(night1.getNetRoomRate()).isEqualByComparingTo(new BigDecimal("1296000.00"));
    }

    @Test
    @DisplayName("Should stack automatic discount rule and user voucher together")
    void shouldStackRuleAndVoucher() {
        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);

        // Automatic rule: EARLY_BIRD (-50.000 fixed)
        DiscountRuleType earlyBirdType = DiscountRuleType.builder().code("EARLY_BIRD").displayName("Early Bird").priority((short) 5).build();
        DiscountRule rule = DiscountRule.builder()
                .id(1)
                .ruleType(earlyBirdType)
                .discountType("FIXED")
                .discountValue(new BigDecimal("50000.00"))
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2027, 1, 31))
                .status("ACTIVE")
                .build();

        // Voucher: NEWYEAR10 (-10%)
        Voucher voucher = Voucher.builder()
                .id(100)
                .voucherCode("NEWYEAR10")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("10.00"))
                .status("ACTIVE")
                .build();

        when(discountRuleRepository.findActiveDiscountsForRoomType(eq(1), any(), any())).thenReturn(List.of(rule));
        when(voucherRepository.findValidVoucher(eq("NEWYEAR10"), any(), any())).thenReturn(Optional.of(voucher));

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 1))
                .voucherCode("NEWYEAR10")
                .build();

        DailyRateContext night = DailyRateContext.builder()
                .date(LocalDate.of(2026, 12, 31))
                .adjustedRate(new BigDecimal("1440000.00"))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .dailyRates(List.of(night))
                .build();

        promotionHandler.handle(context);

        // Auto discount = 50.000, Voucher discount = 144.000 (10% of adjustedRate 1.440.000)
        // Total discount = 194.000, Net rate = 1.246.000
        assertThat(night.getAutoDiscountAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(night.getVoucherDiscountAmount()).isEqualByComparingTo(new BigDecimal("144000.00"));
        assertThat(night.getTotalDiscountAmount()).isEqualByComparingTo(new BigDecimal("194000.00"));
        assertThat(night.getNetRoomRate()).isEqualByComparingTo(new BigDecimal("1246000.00"));
    }
}
