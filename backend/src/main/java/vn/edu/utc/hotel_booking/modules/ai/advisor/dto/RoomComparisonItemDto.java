package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCheck;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCoverage;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ServiceFeeStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Single room column in a multi-criteria room comparison matrix.
 * Dynamic price, capacity, and feature facts strictly originate from Core contracts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomComparisonItemDto {

    String recommendationId;
    Integer hotelRoomTypeId;
    Integer hotelId;
    String hotelName;
    String roomTypeCode;
    String roomTypeName;

    String quoteId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    OffsetDateTime observedAt;

    String finalTotalAmount;
    String perNightAverageAmount;
    String currency;
    Integer totalNights;
    BudgetCoverage chargeCoverage;
    ServiceFeeStatus serviceFeeStatus;
    BudgetCheck budgetCheck;

    String priceDeltaFromCheapest;
    BigDecimal priceDiffPercentFromCheapest;

    Integer maxAdults;
    Integer maxChildren;
    Integer maxTotalGuests;
    Integer extraBedsAllowed;

    String staticAreaAndLayout;

    @Builder.Default
    List<String> uniqueFeatureNames = new ArrayList<>();

    @Builder.Default
    List<String> bestForBullets = new ArrayList<>();

    RecommendationActionDto action;
}
