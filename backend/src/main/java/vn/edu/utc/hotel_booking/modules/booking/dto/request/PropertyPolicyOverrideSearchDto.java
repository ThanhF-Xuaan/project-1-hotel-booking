package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyPolicyOverrideSearchDto {

    Short hotelId;
    Integer hotelRoomTypeId;
    Boolean isActive;

    @Builder.Default
    Integer page = 0;

    @Builder.Default
    Integer pageSize = 10;

    @Builder.Default
    String sortBy = "id";

    @Builder.Default
    String sortDirection = "DESC";
}
