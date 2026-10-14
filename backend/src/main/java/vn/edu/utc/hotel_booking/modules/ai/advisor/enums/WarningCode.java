package vn.edu.utc.hotel_booking.modules.ai.advisor.enums;

public final class WarningCode {
    private WarningCode() {}

    public static final String SERVICE_FEE_UNVERIFIED = "SERVICE_FEE_UNVERIFIED";
    public static final String CURRENCY_SYNTHETIC_ASSUMPTION = "CURRENCY_SYNTHETIC_ASSUMPTION";
    public static final String INCOMPLETE_COVERAGE = "INCOMPLETE_COVERAGE";
    public static final String UNKNOWN_SERVICE_FEE = "UNKNOWN_SERVICE_FEE";
    public static final String UNVERIFIED_BUDGET = "UNVERIFIED_BUDGET";
    public static final String BUDGET_UNVERIFIABLE = "BUDGET_UNVERIFIABLE";
    public static final String CURRENCY_MISMATCH = "CURRENCY_MISMATCH";
    public static final String RATE_LIMIT_NEAR = "RATE_LIMIT_NEAR";
    public static final String STALE_SNAPSHOT = "STALE_SNAPSHOT";
}
