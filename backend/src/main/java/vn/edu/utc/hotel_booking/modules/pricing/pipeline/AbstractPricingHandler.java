package vn.edu.utc.hotel_booking.modules.pricing.pipeline;

import lombok.Getter;

public abstract class AbstractPricingHandler implements PricingHandler {

    @Getter
    protected PricingHandler nextHandler;

    @Override
    public void setNext(PricingHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    protected void executeNext(PricingContext context) {
        if (nextHandler != null) {
            nextHandler.handle(context);
        }
    }
}
