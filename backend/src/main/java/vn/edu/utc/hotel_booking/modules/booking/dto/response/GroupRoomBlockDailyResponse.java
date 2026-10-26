package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockDailyStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupRoomBlockDailyResponse {

    Long id;
    Long roomBlockId;
    Integer hotelRoomTypeId;
    String roomTypeName;
    LocalDate stayDate;
    Integer blockedQuantity;
    Integer pickedUpQuantity;
    Integer releasedQuantity;
    GroupRoomBlockDailyStatus status;
    OffsetDateTime createdAt;
}
