package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationActionDto {
    @Builder.Default
    String type = "VIEW_ROOM_DETAILS";

    String path;
}
