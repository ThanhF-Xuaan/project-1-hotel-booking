package vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler;

import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.AbstractPricingHandler;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.DailyRateContext;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingContext;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class BaseRateHandler extends AbstractPricingHandler {

    @Override
    public void handle(PricingContext context) {
        if (context.getHotelRoomType() != null && context.getRequest() != null) {
            BigDecimal basePrice = context.getHotelRoomType().getBasePrice();
            Integer taxCategoryId = context.getHotelRoomType().getTaxCategoryId();

            LocalDate cur = context.getRequest().getCheckInDate();
            LocalDate checkOut = context.getRequest().getCheckOutDate();

            if (cur != null && checkOut != null) {
                while (cur.isBefore(checkOut)) {
                    DailyRateContext dailyRate = DailyRateContext.builder()
                            .date(cur)
                            .baseRate(basePrice)
                            .surchargeAmount(BigDecimal.ZERO)
                            .rateAfterSurcharge(basePrice)
                            .seasonalAdjustment(BigDecimal.ZERO)
                            .holidayAdjustment(BigDecimal.ZERO)
                            .adjustedRate(basePrice)
                            .autoDiscountAmount(BigDecimal.ZERO)
                            .voucherDiscountAmount(BigDecimal.ZERO)
                            .totalDiscountAmount(BigDecimal.ZERO)
                            .discountAmount(BigDecimal.ZERO)
                            .netRoomRate(basePrice)
                            .taxCategoryId(taxCategoryId)
                            .serviceFeeRate(context.getServiceFeePercent())
                            .serviceFeeAmount(BigDecimal.ZERO)
                            .roomRateWithServiceFee(BigDecimal.ZERO)
                            .vatPercent(BigDecimal.ZERO)
                            .vatAmount(BigDecimal.ZERO)
                            .grossDailyTotal(BigDecimal.ZERO)
                            .build();

                    context.addDailyRate(dailyRate);
                    cur = cur.plusDays(1);
                }
            }
        }

        executeNext(context);
    }
}
