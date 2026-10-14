package vn.edu.utc.hotel_booking.modules.ai.advisor.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomFeatureDto;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomCandidate {
    Integer hotelId;
    String hotelName;
    Integer hotelRoomTypeId;
    Integer roomTypeId;
    String roomTypeCode;
    String roomTypeName;
    Integer maxAdults;
    Integer maxChildren;
    Integer maxTotalGuests;
    Integer extraBedsAllowed;

    @Builder.Default
    List<RoomFeatureDto> features = new ArrayList<>();

    String status;
    Integer standardAdults;
    Integer standardChildren;
    Integer maxInfants;
    Integer extraBeds;
}
