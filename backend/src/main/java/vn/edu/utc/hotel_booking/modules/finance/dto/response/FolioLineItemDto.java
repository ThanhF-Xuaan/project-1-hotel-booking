package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FolioLineItemDto {

    Long referenceId;
    String lineType; // ROOM_RATE, SURCHARGE, PENALTY, PRODUCT, SERVICE, UPGRADE_DIFFERENCE
    String description;
    LocalDate itemDate;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal subtotal;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    BigDecimal vatRate;
    BigDecimal vatAmount;
    BigDecimal totalAmount;
    String roomNumber;
}
