package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPenaltyType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoShowPolicyResponse {

    Integer id;
    String policyCode;
    String name;
    BookingType bookingType;
    String description;
    LocalTime releaseRoomTime;
    NoShowPenaltyType penaltyType;
    BigDecimal penaltyValue;
    Boolean isActive;
    Boolean isDeleted;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    String createdBy;
    String updatedBy;
}
