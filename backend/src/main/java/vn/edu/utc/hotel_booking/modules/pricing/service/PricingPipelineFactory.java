package vn.edu.utc.hotel_booking.modules.pricing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingHandler;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.PricingPipelineBuilder;
import vn.edu.utc.hotel_booking.modules.pricing.pipeline.handler.*;

@Component
@RequiredArgsConstructor
public class PricingPipelineFactory {

    private final BaseRateHandler baseRateHandler;
    private final SurchargeHandler surchargeHandler;
    private final SeasonalityHandler seasonalityHandler;
    private final PromotionHandler promotionHandler;
    private final AddonServiceHandler addonServiceHandler;
    private final TaxAndFeeHandler taxAndFeeHandler;

    /**
     * Pipeline đầy đủ 6 bước cho Đặt phòng
     */
    public PricingHandler createFullBookingPipeline() {
        return PricingPipelineBuilder.create()
                .addHandler(baseRateHandler)
                .addHandler(surchargeHandler)
                .addHandler(seasonalityHandler)
                .addHandler(promotionHandler)
                .addHandler(addonServiceHandler)
                .addHandler(taxAndFeeHandler)
                .build();
    }

    /**
     * Pipeline riêng cho POS Order (Chỉ tính dịch vụ + Thuế phí)
     */
    public PricingHandler createPosOrderPipeline() {
        return PricingPipelineBuilder.create()
                .addHandler(addonServiceHandler)
                .addHandler(taxAndFeeHandler)
                .build();
    }

    /**
     * Pipeline riêng cho Phụ thu (Chỉ tính phụ thu + Thuế phí)
     */
    public PricingHandler createSurchargeOnlyPipeline() {
        return PricingPipelineBuilder.create()
                .addHandler(surchargeHandler)
                .addHandler(taxAndFeeHandler)
                .build();
    }

    /**
     * Pipeline chỉ tính phòng (Không kèm dịch vụ add-on)
     */
    public PricingHandler createRoomOnlyPipeline() {
        return PricingPipelineBuilder.create()
                .addHandler(baseRateHandler)
                .addHandler(surchargeHandler)
                .addHandler(seasonalityHandler)
                .addHandler(promotionHandler)
                .addHandler(taxAndFeeHandler)
                .build();
    }
}
