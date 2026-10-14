package vn.edu.utc.hotel_booking.modules.ai.advisor.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CandidateAvailability {
    Integer hotelRoomTypeId;
    LocalDate availabilityDate;
    Integer totalRooms;
    Integer bookedRooms;
    Integer lockedRooms;
    Integer oooRooms;
    Integer availableCount;
}
