package vn.edu.utc.hotel_booking.modules.pricing.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface VatRuleService {

    BigDecimal getActiveVatPercent(Integer taxCategoryId, LocalDate effectiveDate);
}
