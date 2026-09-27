package vn.edu.utc.hotel_booking.modules.finance.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;
import vn.edu.utc.hotel_booking.modules.finance.entity.InvoiceStatus;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceSearchDto extends BaseSearchDto {

    Long bookingId;
    String invoiceNumber;
    InvoiceStatus status;
}
