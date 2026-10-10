package vn.edu.utc.hotel_booking.modules.pricing.pipeline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.operation.entity.HotelServiceItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SelectedServiceRequest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.AddonServiceHandler;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.TaxAndFeeHandler;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandalonePosAndSurchargeTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private VatRuleRepository vatRuleRepository;

    @Test
    @DisplayName("Should calculate standalone POS service order without room charges")
    void shouldCalculateStandalonePosOrder() {
        // Arrange: Standalone F&B Order: 2 Buffet Breakfasts (300.000 each = 600.000)
        TaxCategory catService = TaxCategory.builder().id(2).categoryCode("SERVICE").categoryName("F&B").build();
        Menu buffet = Menu.builder()
                .id(102)
                .name("Buffet Breakfast")
                .basePrice(new BigDecimal("300000.00"))
                .taxCategory(catService)
                .hotelServiceItem(HotelServiceItem.builder().pricingType(vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType.PER_PERSON).build())
                .build();

        when(menuRepository.findByIdAndIsDeletedFalse(102)).thenReturn(Optional.of(buffet));

        VatRule vat10 = VatRule.builder()
                .taxCategory(catService)
                .vatPercent(new BigDecimal("10.00"))
                .build();

        when(vatRuleRepository.findActiveVatRule(eq(2), any(LocalDate.class)))
                .thenReturn(Optional.of(vat10));

        AddonServiceHandler addonHandler = new AddonServiceHandler(menuRepository);
        TaxAndFeeHandler taxHandler = new TaxAndFeeHandler(vatRuleRepository);

        // Chain pipeline handlers: Addon -> Tax
        addonHandler.setNext(taxHandler);

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .adults((short) 2)
                .selectedServices(List.of(
                        SelectedServiceRequest.builder()
                                .serviceId(102)
                                .quantity(1)
                                .build()
                ))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .serviceFeePercent(new BigDecimal("5.00"))
                .dailyRates(Collections.emptyList()) // No room nights
                .build();

        // Act
        addonHandler.handle(context);

        // Assert: 2 x 300.000 = 600.000 + 5% Fee (30.000) = 630.000 + 10% VAT (63.000) = 693.000
        assertThat(context.getServiceItems()).hasSize(1);
        assertThat(context.getServiceItems().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("600000.00"));
        assertThat(context.getSummary().getTotalRoomGrossAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(context.getSummary().getTotalServiceGrossAmount()).isEqualByComparingTo(new BigDecimal("693000.00"));
        assertThat(context.getSummary().getGrandTotal()).isEqualByComparingTo(new BigDecimal("693000.00"));
    }
}
