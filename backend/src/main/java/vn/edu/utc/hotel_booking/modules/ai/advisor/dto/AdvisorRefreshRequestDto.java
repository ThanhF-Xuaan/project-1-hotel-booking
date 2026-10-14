package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdvisorRefreshRequestDto {
    @Builder.Default
    String contractVersion = "2.0";

    UUID sessionId;
    Long expectedSessionVersion;
}
