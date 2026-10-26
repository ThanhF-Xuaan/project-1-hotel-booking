package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.finance.entity.ChargeCategory;
import vn.edu.utc.hotel_booking.modules.finance.entity.RoutingTarget;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MasterFolioLineItemDto {

    Long referenceId;
    String itemType; // 'ROOM_RATE', 'BOOKING_CHARGE', 'SERVICE_ORDER_DETAIL'
    ChargeCategory chargeCategory;
    String description;
    LocalDate itemDate;
    String roomNumber;
    String guestName;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal subtotal;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    BigDecimal vatRate;
    BigDecimal vatAmount;
    BigDecimal totalAmount;
    RoutingTarget routingTarget;
    Boolean isManuallyAdjusted;
    String adjustmentReason;
}
