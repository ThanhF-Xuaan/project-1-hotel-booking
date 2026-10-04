package vn.edu.utc.hotel_booking.modules.operation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LodgingQueueResponse {

    Long id;
    Long stayGuestId;
    StayGuestResponse stayGuest;
    Short hotelId;
    String hotelName;
    LodgingQueueStatus status;
    String errorMessage;
    OffsetDateTime exportedAt;
    String batchReference;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
