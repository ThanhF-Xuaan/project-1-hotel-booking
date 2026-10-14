package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BaseRateHandlerTest {

    @Test
    @DisplayName("BaseRateHandler: Khởi tạo chính xác danh sách 2 đêm với giá gốc 1.000.000 VNĐ/đêm")
    void shouldInitializeDailyRatesWithBasePrice() {
        // Given
        BaseRateHandler handler = new BaseRateHandler();

        HotelRoomType roomType = new HotelRoomType();
        roomType.setId(1);
        roomType.setBasePrice(new BigDecimal("1000000.00"));
        roomType.setTaxCategoryId(1);

        PriceCalculationRequest request = PriceCalculationRequest.builder()
                .hotelRoomTypeId(1)
                .checkInDate(LocalDate.of(2026, 12, 31))
                .checkOutDate(LocalDate.of(2027, 1, 2)) // 2 nights
                .adults((short) 3)
                .children((short) 0)
                .build();

        PricingContext context = PricingContext.builder()
                .request(request)
                .hotelRoomType(roomType)
                .serviceFeePercent(new BigDecimal("5.00"))
                .build();

        // When
        handler.handle(context);

        // Then
        assertThat(context.getDailyRates()).hasSize(2);
        assertThat(context.getDailyRates().get(0).getDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(context.getDailyRates().get(0).getBaseRate()).isEqualByComparingTo("1000000.00");
        assertThat(context.getDailyRates().get(0).getRateAfterSurcharge()).isEqualByComparingTo("1000000.00");
        assertThat(context.getDailyRates().get(1).getDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(context.getDailyRates().get(1).getBaseRate()).isEqualByComparingTo("1000000.00");
    }
}
