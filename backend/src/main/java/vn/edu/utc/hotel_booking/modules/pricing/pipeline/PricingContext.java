package vn.edu.utc.hotel_booking.modules.pricing.pipeline;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Voucher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricingContext {
    PriceCalculationRequest request;
    HotelRoomType hotelRoomType;
    Hotel hotel;
    BigDecimal serviceFeePercent;

    // Voucher Snapshot nếu khách hàng áp dụng voucher
    Voucher appliedVoucher;
    String appliedPromotionName;

    @Builder.Default
    List<DailyRateContext> dailyRates = new ArrayList<>();

    @Builder.Default
    List<ServiceItemContext> serviceItems = new ArrayList<>();

    PricingSummary summary;

    public void addDailyRate(DailyRateContext rate) {
        if (this.dailyRates == null) {
            this.dailyRates = new ArrayList<>();
        }
        this.dailyRates.add(rate);
    }

    public void addServiceItem(ServiceItemContext item) {
        if (this.serviceItems == null) {
            this.serviceItems = new ArrayList<>();
        }
        this.serviceItems.add(item);
    }
}
