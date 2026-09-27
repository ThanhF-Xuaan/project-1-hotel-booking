package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDailyRateResponse {

    Long id;
    LocalDate stayDate;
    BigDecimal basePrice;
    BigDecimal discountAmount;
    BigDecimal surchargeAmount;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    Integer taxCategoryId;
    String taxCategoryName;
    BigDecimal vatPercent;
    BigDecimal vatAmount;
    BigDecimal netPrice;
    String status;
}
