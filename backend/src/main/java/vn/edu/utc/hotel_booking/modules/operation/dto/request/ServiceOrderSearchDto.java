package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderStatus;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceOrderSearchDto extends BaseSearchDto {

    Long bookingId;
    Integer roomInstanceId;
    String orderNumber;
    ServiceOrderStatus status;
}
