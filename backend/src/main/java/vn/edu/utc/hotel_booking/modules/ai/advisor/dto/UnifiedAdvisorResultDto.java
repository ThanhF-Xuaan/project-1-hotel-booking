package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.UnifiedIntent;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnifiedAdvisorResultDto {

    @Builder.Default
    String contractVersion = "2.1-proposed";

    UUID requestId;

    UnifiedIntent intent;

    UUID sessionId;

    Long sessionVersion;

    AdvisorResultDto advisor;

    KnowledgeAnswerDto knowledge;
}
