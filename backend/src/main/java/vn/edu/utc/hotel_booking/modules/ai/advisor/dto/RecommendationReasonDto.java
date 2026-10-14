package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ReasonCode;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationReasonDto {
    ReasonCode code;
    String text;

    @Builder.Default
    List<String> evidenceFields = new ArrayList<>();
}
