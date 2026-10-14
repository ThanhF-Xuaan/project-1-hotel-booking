package vn.edu.utc.hotel_booking.modules.ai.advisor.adapter;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AmbiguityDto;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DestinationResolutionResult {
    Integer hotelId;
    Short regionId;
    String resolvedName;
    boolean ambiguous;

    @Builder.Default
    List<AmbiguityDto> ambiguities = new ArrayList<>();
}
