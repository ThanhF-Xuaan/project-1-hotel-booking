package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.DailyRateContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.ServiceItemContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.ServicePricingType;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxAndFeeHandlerTest {

    @Mock
    private VatRuleRepository vatRuleRepository;

    private TaxAndFeeHandler taxAndFeeHandler;

    @BeforeEach
    void setUp() {
        taxAndFeeHandler = new TaxAndFeeHandler(vatRuleRepository);
    }

    @Test
    @DisplayName("Should correctly compute 5% service fee and date-specific VAT (8% for 31/12, 10% for 01/01)")
    void shouldComputeDateSpecificTaxAndFees() {
        // Arrange
        // VAT 8% for 31/12/2026
        TaxCategory catRoom = TaxCategory.builder().id(1).categoryCode("ROOM").categoryName("Room Tax").build();
        TaxCategory catService = TaxCategory.builder().id(2).categoryCode("SERVICE").categoryName("F&B Tax").build();

        VatRule vat8 = VatRule.builder()
                .taxCategory(catRoom)
                .vatCode("VAT8")
                .vatPercent(new BigDecimal("8.00"))
                .build();

        // VAT 10% for 01/01/2027 and Services
        VatRule vat10 = VatRule.builder()
                .taxCategory(catRoom)
                .vatCode("VAT10")
                .vatPercent(new BigDecimal("10.00"))
                .build();

        when(vatRuleRepository.findActiveVatRule(eq(1), eq(LocalDate.of(2026, 12, 31))))
                .thenReturn(Optional.of(vat8));
        when(vatRuleRepository.findActiveVatRule(eq(1), eq(LocalDate.of(2027, 1, 1))))
                .thenReturn(Optional.of(vat10));
        when(vatRuleRepository.findActiveVatRule(eq(2), any(LocalDate.class)))
                .thenReturn(Optional.of(vat10));

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 2))
                .build();

        DailyRateContext night1 = DailyRateContext.builder()
                .date(LocalDate.of(2026, 12, 31))
                .taxCategoryId(1)
                .baseRate(new BigDecimal("1000000.00"))
                .netRoomRate(new BigDecimal("1296000.00"))
                .build();
        DailyRateContext night2 = DailyRateContext.builder()
                .date(LocalDate.of(2027, 1, 1))
                .taxCategoryId(1)
                .baseRate(new BigDecimal("1000000.00"))
                .netRoomRate(new BigDecimal("1296000.00"))
                .build();

        ServiceItemContext item1 = ServiceItemContext.builder()
                .serviceId(101)
                .serviceName("Honeymoon Setup")
                .pricingType(ServicePricingType.PER_STAY)
                .unitPrice(new BigDecimal("500000.00"))
                .quantity(1)
                .subtotal(new BigDecimal("500000.00"))
                .taxCategoryId(2)
                .build();
        ServiceItemContext item2 = ServiceItemContext.builder()
                .serviceId(102)
                .serviceName("Buffet Breakfast")
                .pricingType(ServicePricingType.PER_PERSON_PER_NIGHT)
                .unitPrice(new BigDecimal("300000.00"))
                .quantity(6)
                .subtotal(new BigDecimal("1800000.00"))
                .taxCategoryId(2)
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .serviceFeePercent(new BigDecimal("5.00"))
                .dailyRates(List.of(night1, night2))
                .serviceItems(List.of(item1, item2))
                .build();

        // Act
        taxAndFeeHandler.handle(context);

        // Assert Night 1 (31/12): Net 1.296.000 + 5% Fee (64.800) + 8% VAT (108.864) = 1.469.664
        assertThat(night1.getServiceFeeAmount()).isEqualByComparingTo(new BigDecimal("64800.00"));
        assertThat(night1.getVatPercent()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(night1.getVatAmount()).isEqualByComparingTo(new BigDecimal("108864.00"));
        assertThat(night1.getGrossDailyTotal()).isEqualByComparingTo(new BigDecimal("1469664.00"));

        // Assert Night 2 (01/01): Net 1.296.000 + 5% Fee (64.800) + 10% VAT (136.080) = 1.496.880
        assertThat(night2.getServiceFeeAmount()).isEqualByComparingTo(new BigDecimal("64800.00"));
        assertThat(night2.getVatPercent()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(night2.getVatAmount()).isEqualByComparingTo(new BigDecimal("136080.00"));
        assertThat(night2.getGrossDailyTotal()).isEqualByComparingTo(new BigDecimal("1496880.00"));

        // Assert Services: 2.300.000 + 5% Fee (115.000) + 10% VAT (241.500) = 2.656.500
        assertThat(context.getSummary().getTotalServiceFee()).isEqualByComparingTo(new BigDecimal("115000.00"));
        assertThat(context.getSummary().getTotalServiceVat()).isEqualByComparingTo(new BigDecimal("241500.00"));
        assertThat(context.getSummary().getTotalServiceGrossAmount()).isEqualByComparingTo(new BigDecimal("2656500.00"));

        // Grand Total: 1.469.664 + 1.496.880 + 2.656.500 = 5.623.044
        assertThat(context.getSummary().getGrandTotal()).isEqualByComparingTo(new BigDecimal("5623044.00"));
    }
}
