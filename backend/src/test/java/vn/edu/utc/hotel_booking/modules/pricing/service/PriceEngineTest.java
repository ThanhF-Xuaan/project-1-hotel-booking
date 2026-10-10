package vn.edu.utc.hotel_booking.modules.pricing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;
import vn.edu.utc.hotel_booking.modules.pricing.entity.*;
import vn.edu.utc.hotel_booking.modules.pricing.repository.*;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.PriceEngineImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceEngineTest {

    @Mock
    HotelRoomTypeRepository hotelRoomTypeRepository;

    @Mock
    PricingRuleRepository pricingRuleRepository;

    @Mock
    HolidayCalendarRepository holidayCalendarRepository;

    @Mock
    DiscountRuleRepository discountRuleRepository;

    @Mock
    SurchargeRuleRepository surchargeRuleRepository;

    @Mock
    VatRuleRepository vatRuleRepository;

    @Mock
    VoucherRepository voucherRepository;

    @Mock
    vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository menuRepository;

    PriceEngineImpl priceEngine;

    HotelRoomType hotelRoomType;
    TaxCategory taxCategory;
    VatRule vatRule;

    @BeforeEach
    void setUp() {
        vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.BaseRateHandler baseRateHandler =
                new vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.BaseRateHandler();
        vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.SurchargeHandler surchargeHandler =
                new vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.SurchargeHandler(surchargeRuleRepository);
        vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.SeasonalityHandler seasonalityHandler =
                new vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.SeasonalityHandler(pricingRuleRepository);
        vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.PromotionHandler promotionHandler =
                new vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.PromotionHandler(discountRuleRepository, voucherRepository);
        vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.AddonServiceHandler addonServiceHandler =
                new vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.AddonServiceHandler(menuRepository);
        vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.TaxAndFeeHandler taxAndFeeHandler =
                new vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.TaxAndFeeHandler(vatRuleRepository);

        PricingPipelineFactory pipelineFactory = new PricingPipelineFactory(
                baseRateHandler,
                surchargeHandler,
                seasonalityHandler,
                promotionHandler,
                addonServiceHandler,
                taxAndFeeHandler
        );

        priceEngine = new PriceEngineImpl(hotelRoomTypeRepository, pipelineFactory);
        RoomType roomType = RoomType.builder()
                .id((short) 1)
                .code("DELUXE")
                .name("Phòng Deluxe Hướng Biển")
                .build();

        hotelRoomType = HotelRoomType.builder()
                .id(1)
                .roomType(roomType)
                .taxCategoryId(1)
                .basePrice(BigDecimal.valueOf(1000000.00))
                .standardAdults((short) 2)
                .maxAdults((short) 3)
                .maxTotalGuests((short) 4)
                .extraBeds((short) 1)
                .build();

        taxCategory = TaxCategory.builder()
                .id(1)
                .categoryCode("ROOM_TAX")
                .categoryName("Thuế phòng")
                .build();

        vatRule = VatRule.builder()
                .id(1)
                .taxCategory(taxCategory)
                .vatPercent(BigDecimal.valueOf(10.00))
                .startDate(LocalDate.of(2026, 1, 1))
                .build();
    }

    @Test
    @DisplayName("calculatePrice basic standard calculation for 2 nights")
    void testCalculatePrice_BasicFlow() {
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 10, 1))
                .checkOutDate(LocalDate.of(2026, 10, 3)) // 2 nights
                .adults((short) 2)
                .children((short) 0)
                .extraBeds((short) 0)
                .build();

        when(hotelRoomTypeRepository.findByIdWithDetails(1)).thenReturn(Optional.of(hotelRoomType));
        when(pricingRuleRepository.findActiveRulesForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());
        when(discountRuleRepository.findActiveDiscountsForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());
        when(surchargeRuleRepository.findActiveSurchargesForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());
        when(vatRuleRepository.findActiveVatRule(eq(1), any())).thenReturn(Optional.of(vatRule));

        PriceBreakdownDto breakdown = priceEngine.calculatePrice(request);

        assertThat(breakdown.getTotalNights()).isEqualTo(2);
        assertThat(breakdown.getTotalBasePrice()).isEqualByComparingTo("2000000.00");
        assertThat(breakdown.getTotalDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(breakdown.getTotalSurchargeAmount()).isEqualByComparingTo("0.00");
        assertThat(breakdown.getPreTaxAmount()).isEqualByComparingTo("2000000.00");
        assertThat(breakdown.getTotalRoomServiceFee()).isEqualByComparingTo("100000.00");
        assertThat(breakdown.getVatPercent()).isEqualByComparingTo("10.00");
        assertThat(breakdown.getVatAmount()).isEqualByComparingTo("210000.00");
        assertThat(breakdown.getFinalTotalAmount()).isEqualByComparingTo("2310000.00");
    }

    @Test
    @DisplayName("calculatePrice applies Seasonal percentage adjustment rule")
    void testCalculatePrice_SeasonalAdjustment() {
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 7, 1))
                .checkOutDate(LocalDate.of(2026, 7, 3)) // 2 nights
                .adults((short) 2)
                .children((short) 0)
                .extraBeds((short) 0)
                .build();

        PricingRuleType ruleType = PricingRuleType.builder().code("SEASONAL").displayName("Mùa cao điểm").build();
        PricingRule rule = PricingRule.builder()
                .hotelRoomType(hotelRoomType)
                .ruleType(ruleType)
                .adjustmentType("PERCENT")
                .adjustmentValue(BigDecimal.valueOf(15.00)) // +15%
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .build();

        when(hotelRoomTypeRepository.findByIdWithDetails(1)).thenReturn(Optional.of(hotelRoomType));
        when(pricingRuleRepository.findActiveRulesForRoomType(eq(1), any(), any())).thenReturn(List.of(rule));
        when(discountRuleRepository.findActiveDiscountsForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());
        when(surchargeRuleRepository.findActiveSurchargesForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());
        when(vatRuleRepository.findActiveVatRule(eq(1), any())).thenReturn(Optional.of(vatRule));

        PriceBreakdownDto breakdown = priceEngine.calculatePrice(request);

        // Base 1,000,000 + 150,000 = 1,150,000 / night -> 2 nights = 2,300,000
        assertThat(breakdown.getPreTaxAmount()).isEqualByComparingTo("2300000.00");
        assertThat(breakdown.getTotalRoomServiceFee()).isEqualByComparingTo("115000.00");
        assertThat(breakdown.getVatAmount()).isEqualByComparingTo("241500.00");
        assertThat(breakdown.getFinalTotalAmount()).isEqualByComparingTo("2656500.00");
    }

    @Test
    @DisplayName("calculatePrice applies Discount rule")
    void testCalculatePrice_DiscountRule() {
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 10, 1))
                .checkOutDate(LocalDate.of(2026, 10, 2)) // 1 night
                .adults((short) 2)
                .children((short) 0)
                .extraBeds((short) 0)
                .build();

        DiscountRuleType discountRuleType = DiscountRuleType.builder().code("EARLY_BIRD").displayName("Đặt sớm").build();
        DiscountRule discountRule = DiscountRule.builder()
                .hotelRoomType(hotelRoomType)
                .ruleType(discountRuleType)
                .discountType("PERCENT")
                .discountValue(BigDecimal.valueOf(10.00)) // -10%
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 31))
                .build();

        when(hotelRoomTypeRepository.findByIdWithDetails(1)).thenReturn(Optional.of(hotelRoomType));
        when(pricingRuleRepository.findActiveRulesForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());
        when(discountRuleRepository.findActiveDiscountsForRoomType(eq(1), any(), any())).thenReturn(List.of(discountRule));
        when(surchargeRuleRepository.findActiveSurchargesForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());
        when(vatRuleRepository.findActiveVatRule(eq(1), any())).thenReturn(Optional.of(vatRule));

        PriceBreakdownDto breakdown = priceEngine.calculatePrice(request);

        // Base 1,000,000 - 100,000 = 900,000
        assertThat(breakdown.getTotalDiscountAmount()).isEqualByComparingTo("100000.00");
        assertThat(breakdown.getPreTaxAmount()).isEqualByComparingTo("900000.00");
        assertThat(breakdown.getTotalRoomServiceFee()).isEqualByComparingTo("45000.00");
        assertThat(breakdown.getVatAmount()).isEqualByComparingTo("94500.00");
        assertThat(breakdown.getFinalTotalAmount()).isEqualByComparingTo("1039500.00");
    }

    @Test
    @DisplayName("calculatePrice applies Extra Adult surcharge")
    void testCalculatePrice_ExtraPersonSurcharge() {
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 10, 1))
                .checkOutDate(LocalDate.of(2026, 10, 2)) // 1 night
                .adults((short) 3) // Standard is 2 -> 1 extra adult
                .children((short) 0)
                .extraBeds((short) 0)
                .build();

        when(hotelRoomTypeRepository.findByIdWithDetails(1)).thenReturn(Optional.of(hotelRoomType));
        when(pricingRuleRepository.findActiveRulesForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());
        when(discountRuleRepository.findActiveDiscountsForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());
        when(surchargeRuleRepository.findActiveSurchargesForRoomType(eq(1), any(), any())).thenReturn(Collections.emptyList());
        when(vatRuleRepository.findActiveVatRule(eq(1), any())).thenReturn(Optional.of(vatRule));

        PriceBreakdownDto breakdown = priceEngine.calculatePrice(request);

        // Fallback 20% of basePrice (1,000,000) = 200,000
        assertThat(breakdown.getTotalSurchargeAmount()).isEqualByComparingTo("200000.00");
        assertThat(breakdown.getPreTaxAmount()).isEqualByComparingTo("1200000.00");
        assertThat(breakdown.getTotalRoomServiceFee()).isEqualByComparingTo("60000.00");
        assertThat(breakdown.getVatAmount()).isEqualByComparingTo("126000.00");
        assertThat(breakdown.getFinalTotalAmount()).isEqualByComparingTo("1386000.00");
    }

    @Test
    @DisplayName("calculatePrice throws validation error when checkOutDate is not after checkInDate")
    void testCalculatePrice_InvalidDateRange() {
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 10, 5))
                .checkOutDate(LocalDate.of(2026, 10, 5))
                .build();

        assertThatThrownBy(() -> priceEngine.calculatePrice(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }
}
