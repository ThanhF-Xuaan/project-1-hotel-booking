package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DailyPriceDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalkInQuoteResponse {

    Short hotelId;
    LocalDate checkInDate;
    LocalDate checkOutDate;

    @Builder.Default
    List<WalkInQuoteRoomItemResponse> rooms = new ArrayList<>();

    @Builder.Default
    BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal totalServiceFeeAmount = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal totalVatAmount = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal totalSurchargesAmount = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal totalAmount = BigDecimal.ZERO;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WalkInQuoteRoomItemResponse {
        Integer roomIndex;
        Integer hotelRoomTypeId;
        String roomTypeName;
        Short adultCount;
        Short childCount;
        Short infantCount;
        Short extraBeds;

        @Builder.Default
        List<DailyPriceDto> dailyRates = new ArrayList<>();

        @Builder.Default
        BigDecimal netPrice = BigDecimal.ZERO;

        @Builder.Default
        BigDecimal serviceFeeAmount = BigDecimal.ZERO;

        @Builder.Default
        BigDecimal vatAmount = BigDecimal.ZERO;

        @Builder.Default
        BigDecimal totalPrice = BigDecimal.ZERO;

        ItemizedSurchargeDto capacitySurcharge;

        SurchargeProposalResponse earlyCheckinProposal;
    }
}
