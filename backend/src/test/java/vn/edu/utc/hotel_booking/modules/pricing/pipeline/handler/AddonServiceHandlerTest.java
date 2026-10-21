package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.SelectedServiceRequest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.ServiceItemContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.ServicePricingType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddonServiceHandlerTest {

    @Mock
    private MenuRepository menuRepository;

    private AddonServiceHandler addonServiceHandler;

    @BeforeEach
    void setUp() {
        addonServiceHandler = new AddonServiceHandler(menuRepository);
    }

    @Test
    @DisplayName("Should correctly calculate add-on services and resolve taxCategoryId automatically from Menu entity")
    void shouldCalculateAddonServices() {
        // Arrange
        TaxCategory catService = TaxCategory.builder().id(2).categoryCode("SERVICE").categoryName("F&B").build();

        MasterItem masterHoneymoon = MasterItem.builder()
                .id(101)
                .code("SRV_HONEYMOON")
                .name("Honeymoon Setup")
                .pricingType(vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType.PER_STAY)
                .defaultBasePrice(new BigDecimal("500000.00"))
                .build();

        Menu menu101 = Menu.builder()
                .id(101)
                .name("Honeymoon Setup")
                .basePrice(new BigDecimal("500000.00"))
                .taxCategory(catService)
                .masterItem(masterHoneymoon)
                .build();

        MasterItem masterBuffet = MasterItem.builder()
                .id(102)
                .code("SRV_BUFFET")
                .name("Buffet Breakfast")
                .pricingType(vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType.PER_PERSON_PER_NIGHT)
                .defaultBasePrice(new BigDecimal("300000.00"))
                .build();

        Menu menu102 = Menu.builder()
                .id(102)
                .name("Buffet Breakfast")
                .basePrice(new BigDecimal("300000.00"))
                .taxCategory(catService)
                .masterItem(masterBuffet)
                .build();

        when(menuRepository.findByIdAndIsDeletedFalse(101)).thenReturn(Optional.of(menu101));
        when(menuRepository.findByIdAndIsDeletedFalse(102)).thenReturn(Optional.of(menu102));

        // Frontend only passes serviceId and quantity (NO taxCategoryId from frontend)
        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 2)) // 2 nights
                .adults((short) 3) // 3 pax
                .children((short) 0)
                .selectedServices(List.of(
                        SelectedServiceRequest.builder().serviceId(101).quantity(1).build(),
                        SelectedServiceRequest.builder().serviceId(102).quantity(1).build()
                ))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .serviceFeePercent(new BigDecimal("5.00"))
                .build();

        // Act
        addonServiceHandler.handle(context);

        // Assert
        List<ServiceItemContext> items = context.getServiceItems();
        assertThat(items).hasSize(2);

        // Honeymoon: 500.000 * 1 = 500.000, Tax category = 2
        ServiceItemContext item1 = items.get(0);
        assertThat(item1.getServiceName()).isEqualTo("Honeymoon Setup");
        assertThat(item1.getQuantity()).isEqualTo(1);
        assertThat(item1.getSubtotal()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(item1.getTaxCategoryId()).isEqualTo(2);

        // Buffet: 300.000 * 3 pax * 2 nights = 1.800.000 (qty = 6), Tax category = 2
        ServiceItemContext item2 = items.get(1);
        assertThat(item2.getServiceName()).isEqualTo("Buffet Breakfast");
        assertThat(item2.getQuantity()).isEqualTo(6);
        assertThat(item2.getSubtotal()).isEqualByComparingTo(new BigDecimal("1800000.00"));
        assertThat(item2.getTaxCategoryId()).isEqualTo(2);

        // Total service subtotal
        BigDecimal totalServices = items.stream()
                .map(ServiceItemContext::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalServices).isEqualByComparingTo(new BigDecimal("2300000.00"));
    }

    @Test
    @DisplayName("Should fallback to MasterItem defaultBasePrice when Menu basePrice is null")
    void shouldFallbackToMasterItemDefaultPrice() {
        TaxCategory catService = TaxCategory.builder().id(2).categoryCode("SERVICE").categoryName("F&B").build();

        MasterItem masterAirport = MasterItem.builder()
                .id(103)
                .code("SRV_AIRPORT")
                .name("Airport Shuttle")
                .pricingType(vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType.PER_STAY)
                .defaultBasePrice(new BigDecimal("250000.00"))
                .build();

        // Menu has null basePrice, should use masterItem default
        Menu menu103 = Menu.builder()
                .id(103)
                .name("Airport Shuttle")
                .basePrice(null)
                .taxCategory(catService)
                .masterItem(masterAirport)
                .build();

        when(menuRepository.findByIdAndIsDeletedFalse(103)).thenReturn(Optional.of(menu103));

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 2))
                .adults((short) 2)
                .selectedServices(List.of(
                        SelectedServiceRequest.builder().serviceId(103).quantity(1).build()
                ))
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .build();

        addonServiceHandler.handle(context);

        List<ServiceItemContext> items = context.getServiceItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("250000.00"));
        assertThat(items.get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("250000.00"));
    }
}
