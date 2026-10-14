package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomSummaryDto {
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

    Integer standardAdults;
    Integer standardChildren;
    Integer maxInfants;
    Integer extraBeds;
}
