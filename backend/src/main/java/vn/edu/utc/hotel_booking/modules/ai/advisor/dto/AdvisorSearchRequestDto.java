package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdvisorSearchRequestDto {
    @Builder.Default
    String contractVersion = "2.0";

    RoomSearchCriteriaDto criteria;
    UUID sessionId;
    Long expectedSessionVersion;
}
