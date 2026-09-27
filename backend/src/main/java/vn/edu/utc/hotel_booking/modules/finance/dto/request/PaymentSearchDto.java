package vn.edu.utc.hotel_booking.modules.finance.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentPurpose;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentSearchDto extends BaseSearchDto {

    Long bookingId;
    PaymentMethod paymentMethod;
    PaymentPurpose paymentPurpose;
    PaymentStatus status;
}
