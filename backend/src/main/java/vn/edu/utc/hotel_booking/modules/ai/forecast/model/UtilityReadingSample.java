package vn.edu.utc.hotel_booking.modules.ai.forecast.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Synthetic normalized counter evidence, not the T04 mapped usage response. */
public record UtilityReadingSample(int hotelId, int meterId, Unit unit, LocalDate date,
                                   BigDecimal cumulativeValue, String counterEpoch, Marker marker) {
    public enum Unit { KWH, M3 }
    public enum Marker { NORMAL, RESET, REPLACEMENT }
}
