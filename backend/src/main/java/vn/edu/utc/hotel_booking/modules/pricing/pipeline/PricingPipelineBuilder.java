package vn.edu.utc.hotel_booking.modules.pricing.pipeline;

import java.util.ArrayList;
import java.util.List;

public class PricingPipelineBuilder {
    private final List<PricingHandler> handlers = new ArrayList<>();

    public static PricingPipelineBuilder create() {
        return new PricingPipelineBuilder();
    }

    public PricingPipelineBuilder addHandler(PricingHandler handler) {
        if (handler != null) {
            handlers.add(handler);
        }
        return this;
    }

    public PricingHandler build() {
        if (handlers.isEmpty()) {
            throw new IllegalStateException("Pipeline must have at least one handler");
        }
        for (int i = 0; i < handlers.size() - 1; i++) {
            handlers.get(i).setNext(handlers.get(i + 1));
        }
        return handlers.get(0);
    }
}
