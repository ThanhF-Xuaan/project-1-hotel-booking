package vn.edu.utc.hotel_booking.modules.ai.rag.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagQueryResultDto {
    RagAnswerStatus status;
    String answer;

    @Builder.Default
    List<RagCitationDto> citations = new ArrayList<>();

    @Builder.Default
    List<String> warnings = new ArrayList<>();

    @Builder.Default
    List<String> disclaimers = new ArrayList<>();
}
