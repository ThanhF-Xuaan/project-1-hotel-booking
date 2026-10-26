package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FolioResponse {

    Long bookingId;
    String bookingNumber;
    String bookingStatus;

    Short hotelId;
    String hotelName;

    String leadGuestName;
    String leadGuestPhone;
    String leadGuestEmail;

    LocalDate checkInDate;
    LocalDate checkOutDate;

    @Builder.Default
    List<FolioRoomSummaryDto> rooms = new ArrayList<>();

    @Builder.Default
    List<FolioLineItemDto> debits = new ArrayList<>();

    @Builder.Default
    List<FolioPaymentDto> credits = new ArrayList<>();

    @Builder.Default
    BigDecimal subTotal = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal serviceFeeAmount = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal vatAmount = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal totalDebits = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal totalCredits = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal balance = BigDecimal.ZERO;

    @Builder.Default
    Boolean canCheckOut = false;

    @Builder.Default
    Boolean hasOverpayment = false;

    @Builder.Default
    BigDecimal overpaymentAmount = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal coveredByMasterFolioAmount = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal personalBalance = BigDecimal.ZERO;
}
