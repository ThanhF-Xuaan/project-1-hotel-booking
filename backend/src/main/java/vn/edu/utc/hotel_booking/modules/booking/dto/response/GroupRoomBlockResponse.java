package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupRoomBlockResponse {

    Long id;
    Long contractId;
    Integer hotelRoomTypeId;
    String roomTypeName;
    Integer blockedRooms;
    Integer pickedUpRooms;
    BigDecimal negotiatedRatePerNight;
    GroupRoomBlockStatus status;
}
