package vn.edu.utc.hotel_booking.modules.pricing.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.operation.entity.HotelServiceItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SelectedServiceRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;
import vn.edu.utc.hotel_booking.modules.pricing.entity.*;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.*;
import vn.edu.utc.hotel_booking.modules.pricing.repository.*;
import vn.edu.utc.hotel_booking.modules.pricing.service.PricingPipelineFactory;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.PriceEngineImpl;

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
class PricingPipelineEndToEndTest {

    @Mock
    private HotelRoomTypeRepository hotelRoomTypeRepository;

    @Mock
    private SurchargeRuleRepository surchargeRuleRepository;

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private DiscountRuleRepository discountRuleRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private VatRuleRepository vatRuleRepository;

    private PriceEngineImpl priceEngine;

    @BeforeEach
    void setUp() {
        BaseRateHandler baseRateHandler = new BaseRateHandler();
        SurchargeHandler surchargeHandler = new SurchargeHandler(surchargeRuleRepository);
        SeasonalityHandler seasonalityHandler = new SeasonalityHandler(pricingRuleRepository);
        PromotionHandler promotionHandler = new PromotionHandler(discountRuleRepository, voucherRepository);
        AddonServiceHandler addonServiceHandler = new AddonServiceHandler(menuRepository);
        TaxAndFeeHandler taxAndFeeHandler = new TaxAndFeeHandler(vatRuleRepository);

        PricingPipelineFactory pipelineFactory = new PricingPipelineFactory(
                baseRateHandler,
                surchargeHandler,
                seasonalityHandler,
                promotionHandler,
                addonServiceHandler,
                taxAndFeeHandler
        );

        priceEngine = new PriceEngineImpl(hotelRoomTypeRepository, pipelineFactory);
    }

    @Test
    @DisplayName("Complete End-to-End Pricing Pipeline Test: Exact Target 5.623.044 VNĐ")
    void shouldCalculateExactTargetPriceBreakdown() {
        // Arrange
        Hotel hotel = new Hotel();
        hotel.setId((short) 1);
        hotel.setName("Grand Hotel");
        hotel.setServiceFeePercent(new BigDecimal("5.00"));

        RoomType masterRoomType = new RoomType();
        masterRoomType.setId((short) 10);
        masterRoomType.setName("Deluxe Suite");

        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);
        roomType.setHotel(hotel);
        roomType.setRoomType(masterRoomType);
        roomType.setBasePrice(new BigDecimal("1000000.00"));
        roomType.setStandardAdults((short) 2);
        roomType.setMaxAdults((short) 4);
        roomType.setMaxChildren((short) 2);
        roomType.setMaxTotalGuests((short) 4);
        roomType.setExtraBeds((short) 1);
        roomType.setTaxCategoryId(1);

        when(hotelRoomTypeRepository.findByIdWithDetails(1)).thenReturn(Optional.of(roomType));

        // 1. Surcharges: Extra 1 Adult = 200.000 VNĐ
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

        // 2. Seasonality: Holiday +20% for both nights
        PricingRuleType holidayType = PricingRuleType.builder().code("HOLIDAY").displayName("Holiday").priority((short) 10).build();
        PricingRule holidayMod = PricingRule.builder()
                .id(1)
                .ruleType(holidayType)
                .holidayCalendar(HolidayCalendar.builder().id(1).name("New Year").build())
                .adjustmentType("PERCENT")
                .adjustmentValue(new BigDecimal("20.00"))
                .startDate(LocalDate.of(2026, 12, 31))
                .endDate(LocalDate.of(2027, 1, 2))
                .build();
        when(pricingRuleRepository.findActiveRulesForRoomType(eq(1), any(), any()))
                .thenReturn(List.of(holidayMod));

        // 3. Promotions: User Voucher NEWYEAR10 (-10%)
        when(discountRuleRepository.findActiveDiscountsForRoomType(eq(1), any(), any()))
                .thenReturn(Collections.emptyList());

        Voucher voucher = Voucher.builder()
                .id(100)
                .voucherCode("NEWYEAR10")
                .title("New Year Voucher 10%")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("10.00"))
                .validFrom(OffsetDateTime.now().minusDays(1))
                .validTo(OffsetDateTime.now().plusDays(30))
                .status("ACTIVE")
                .build();
        when(voucherRepository.findValidVoucher(eq("NEWYEAR10"), any(), any()))
                .thenReturn(Optional.of(voucher));

        // 4. Taxes & Services
        TaxCategory catRoom = TaxCategory.builder().id(1).categoryCode("ROOM").categoryName("Room Tax").build();
        TaxCategory catService = TaxCategory.builder().id(2).categoryCode("SERVICE").categoryName("Service Tax").build();

        Menu honeymoonMenu = Menu.builder()
                .id(101)
                .name("Honeymoon Setup")
                .basePrice(new BigDecimal("500000.00"))
                .taxCategory(catService)
                .hotelServiceItem(HotelServiceItem.builder().pricingType(vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType.PER_STAY).build())
                .build();

        Menu buffetMenu = Menu.builder()
                .id(102)
                .name("Buffet Breakfast")
                .basePrice(new BigDecimal("300000.00"))
                .taxCategory(catService)
                .hotelServiceItem(HotelServiceItem.builder().pricingType(vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType.PER_PERSON_PER_NIGHT).build())
                .build();

        when(menuRepository.findByIdAndIsDeletedFalse(101)).thenReturn(Optional.of(honeymoonMenu));
        when(menuRepository.findByIdAndIsDeletedFalse(102)).thenReturn(Optional.of(buffetMenu));

        VatRule vat8 = VatRule.builder().taxCategory(catRoom).vatCode("VAT8").vatPercent(new BigDecimal("8.00")).build();
        VatRule vat10 = VatRule.builder().taxCategory(catRoom).vatCode("VAT10").vatPercent(new BigDecimal("10.00")).build();
        VatRule vatService10 = VatRule.builder().taxCategory(catService).vatCode("VAT10_SVC").vatPercent(new BigDecimal("10.00")).build();

        when(vatRuleRepository.findActiveVatRule(eq(1), eq(LocalDate.of(2026, 12, 31)))).thenReturn(Optional.of(vat8));
        when(vatRuleRepository.findActiveVatRule(eq(1), eq(LocalDate.of(2027, 1, 1)))).thenReturn(Optional.of(vat10));
        when(vatRuleRepository.findActiveVatRule(eq(2), any(LocalDate.class))).thenReturn(Optional.of(vatService10));

        // Calculation Request (NO taxCategoryId needed from frontend)
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 2)) // 2 nights
                .adults((short) 3) // 3 adults
                .children((short) 0)
                .voucherCode("NEWYEAR10")
                .selectedServices(List.of(
                        SelectedServiceRequest.builder().serviceId(101).quantity(1).build(),
                        SelectedServiceRequest.builder().serviceId(102).quantity(1).build()
                ))
                .build();

        // Act
        PriceBreakdownDto result = priceEngine.calculatePrice(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalNights()).isEqualTo(2);
        assertThat(result.getDailyPrices()).hasSize(2);

        // Night 1 (31/12/2026): Target 1.469.664 VNĐ
        var night1 = result.getDailyPrices().get(0);
        assertThat(night1.getDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(night1.getBaseRate()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(night1.getSurchargeAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(night1.getRateAfterSurcharge()).isEqualByComparingTo(new BigDecimal("1200000.00"));
        assertThat(night1.getHolidayAdjustment()).isEqualByComparingTo(new BigDecimal("240000.00"));
        assertThat(night1.getRateAdjustmentAmount()).isEqualByComparingTo(new BigDecimal("240000.00"));
        assertThat(night1.getAppliedAdjustments()).hasSize(1);
        assertThat(night1.getAppliedAdjustments().get(0).getRuleCode()).isEqualTo("HOLIDAY");
        assertThat(night1.getAdjustedRate()).isEqualByComparingTo(new BigDecimal("1440000.00"));
        assertThat(night1.getVoucherDiscountAmount()).isEqualByComparingTo(new BigDecimal("144000.00"));
        assertThat(night1.getNetAmount()).isEqualByComparingTo(new BigDecimal("1296000.00"));
        assertThat(night1.getServiceFeeAmount()).isEqualByComparingTo(new BigDecimal("64800.00"));
        assertThat(night1.getVatPercent()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(night1.getVatAmount()).isEqualByComparingTo(new BigDecimal("108864.00"));
        assertThat(night1.getGrossDailyTotal()).isEqualByComparingTo(new BigDecimal("1469664.00"));

        // Night 2 (01/01/2027): Target 1.496.880 VNĐ
        var night2 = result.getDailyPrices().get(1);
        assertThat(night2.getDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(night2.getBaseRate()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(night2.getSurchargeAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(night2.getRateAfterSurcharge()).isEqualByComparingTo(new BigDecimal("1200000.00"));
        assertThat(night2.getHolidayAdjustment()).isEqualByComparingTo(new BigDecimal("240000.00"));
        assertThat(night2.getRateAdjustmentAmount()).isEqualByComparingTo(new BigDecimal("240000.00"));
        assertThat(night2.getAppliedAdjustments()).hasSize(1);
        assertThat(night2.getAppliedAdjustments().get(0).getRuleCode()).isEqualTo("HOLIDAY");
        assertThat(night2.getAdjustedRate()).isEqualByComparingTo(new BigDecimal("1440000.00"));
        assertThat(night2.getVoucherDiscountAmount()).isEqualByComparingTo(new BigDecimal("144000.00"));
        assertThat(night2.getNetAmount()).isEqualByComparingTo(new BigDecimal("1296000.00"));
        assertThat(night2.getServiceFeeAmount()).isEqualByComparingTo(new BigDecimal("64800.00"));
        assertThat(night2.getVatPercent()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(night2.getVatAmount()).isEqualByComparingTo(new BigDecimal("136080.00"));
        assertThat(night2.getGrossDailyTotal()).isEqualByComparingTo(new BigDecimal("1496880.00"));

        // Total Rate Adjustment Amount: 240.000 + 240.000 = 480.000 VNĐ
        assertThat(result.getTotalRateAdjustmentAmount()).isEqualByComparingTo(new BigDecimal("480000.00"));

        // Total Room Gross: 1.469.664 + 1.496.880 = 2.966.544 VNĐ
        assertThat(result.getTotalRoomGrossAmount()).isEqualByComparingTo(new BigDecimal("2966544.00"));

        // Add-on Services: Honeymoon (500k) + Buffet (1800k) = 2.300.000 + 5% Fee (115.000) + 10% VAT (241.500) = 2.656.500 VNĐ
        assertThat(result.getServiceItems()).hasSize(2);
        assertThat(result.getTotalServiceGrossAmount()).isEqualByComparingTo(new BigDecimal("2656500.00"));

        // Grand Total: 2.966.544 + 2.656.500 = 5.623.044 VNĐ
        assertThat(result.getFinalTotalAmount()).isEqualByComparingTo(new BigDecimal("5623044.00"));
    }
}
