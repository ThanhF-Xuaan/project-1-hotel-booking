package vn.edu.utc.hotel_booking.pricing;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.pricing.service.PriceCalculator;
import vn.edu.utc.hotel_booking.pricing.service.PriceCalculator.Adjustment;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriceCalculatorTest {
    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void weekendPriceUsesFeeAndVatOnAdjustedAmount() {
        var result = calculator.calculate(new BigDecimal("600000.00"),
                List.of(new Adjustment("PERCENT", new BigDecimal("15"))), null,
                BigDecimal.ZERO, new BigDecimal("5"), new BigDecimal("8"));
        assertEquals(new BigDecimal("90000.00"), result.adjustmentAmount());
        assertEquals(new BigDecimal("34500.00"), result.serviceFeeAmount());
        assertEquals(new BigDecimal("57960.00"), result.vatAmount());
        assertEquals(new BigDecimal("782460.00"), result.netPrice());
    }

    @Test
    void fixedDiscountCannotMakeTaxableAmountNegative() {
        var result = calculator.calculate(new BigDecimal("100000.00"), List.of(),
                new Adjustment("FIXED", new BigDecimal("150000.00")), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("8"));
        assertEquals(new BigDecimal("100000.00"), result.discountAmount());
        assertEquals(new BigDecimal("0.00"), result.netPrice());
    }

    @Test
    void unknownAdjustmentFailsClosed() {
        assertThrows(AppException.class, () -> calculator.amount(BigDecimal.TEN,
                new Adjustment("CUSTOM", BigDecimal.ONE)));
    }
}
