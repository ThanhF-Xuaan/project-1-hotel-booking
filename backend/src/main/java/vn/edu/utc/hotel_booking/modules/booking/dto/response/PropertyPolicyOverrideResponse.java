package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyPolicyOverrideResponse {

    Integer id;
    Short hotelId;
    String hotelName;
    Integer hotelRoomTypeId;
    String roomTypeName;

    Integer cancellationPolicyId;
    String cancellationPolicyCode;
    String cancellationPolicyName;

    Integer noShowPolicyId;
    String noShowPolicyCode;
    String noShowPolicyName;

    Integer freeCancellationBeforeDaysOverride;
    LocalTime noShowReleaseTimeOverride;
    String overrideReason;

    Boolean isActive;
    Boolean isDeleted;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    String createdBy;
    String updatedBy;
}
