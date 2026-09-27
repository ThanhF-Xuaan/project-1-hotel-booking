package vn.edu.utc.hotel_booking.modules.operation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NightAuditResponse {

    Short hotelId;
    LocalDate auditDate;
    Integer noShowBookingsCount;
    Integer occupiedRoomsCount;
    BigDecimal totalDailyRoomRevenue;
    BigDecimal totalDailyServiceRevenue;
    BigDecimal totalDailyRevenue;
    OffsetDateTime auditTimestamp;
    String status;
}
