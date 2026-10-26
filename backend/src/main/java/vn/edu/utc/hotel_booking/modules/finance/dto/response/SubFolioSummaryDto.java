package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubFolioSummaryDto {

    Long bookingId;
    String bookingNumber;
    Long bookingRoomId;
    String roomNumber;
    String roomTypeName;
    String guestName;
    String roomStatus;
    BigDecimal coveredByMasterAmount;
    BigDecimal personalDebitAmount;
    BigDecimal personalPaidAmount;
    BigDecimal personalBalance;
}
