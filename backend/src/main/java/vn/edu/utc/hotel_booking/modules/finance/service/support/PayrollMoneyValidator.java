package vn.edu.utc.hotel_booking.modules.finance.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact NUMERIC(15,2) validation, before arithmetic or persistence; no implicit rounding. */
public final class PayrollMoneyValidator {
    private PayrollMoneyValidator() { }
    public static BigDecimal parse(String raw, String field) {
        if (raw == null || raw.length() > 128) throw invalid(field);
        try {
            BigDecimal value = new BigDecimal(raw);
            BigDecimal effective = value.stripTrailingZeros();
            // Reject extreme exponents before setScale could allocate a huge integer.
            if (effective.scale() > 2 || (long)effective.precision() - effective.scale() > 13) throw invalid(field);
            // Keep existing in-memory representation for already compatible values (e.g. 1000).
            // Excess trailing zeroes may be removed exactly, never rounded.
            return value.scale() <= 2 ? value : value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException error) {
            throw invalid(field);
        }
    }
    private static IllegalArgumentException invalid(String field) {
        return new IllegalArgumentException("SALARY_AMOUNT_NOT_REPRESENTABLE: " + field);
    }
}
