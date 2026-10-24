package vn.edu.utc.hotel_booking.modules.finance.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FolioRoomSummaryDto {

    Long bookingRoomId;
    Integer roomInstanceId;
    String roomNumber;
    String roomTypeName;
    String status;
    Short adultCount;
    Short childCount;
    @Builder.Default
    List<String> guestNames = new ArrayList<>();
}
