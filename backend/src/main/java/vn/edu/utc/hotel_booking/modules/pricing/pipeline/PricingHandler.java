package vn.edu.utc.hotel_booking.modules.pricing.pipeline;

public interface PricingHandler {
    void handle(PricingContext context);
    void setNext(PricingHandler nextHandler);
}
