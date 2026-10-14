package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCheck;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCoverage;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ServiceFeeStatus;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceQuoteDto {
    Integer hotelRoomTypeId;

    @Builder.Default
    String source = "BACKEND_PRICING";

    String quoteId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime observedAt;

    Integer totalNights;
    String currency;
    String preTaxAmount;
    String vatPercent;
    String vatAmount;
    String finalTotalAmount;
    ServiceFeeStatus serviceFeeStatus;
    String serviceFeeAmount;
    BudgetCoverage chargeCoverage;
    BudgetCheck budgetCheck;
    String displayLabel;
}
