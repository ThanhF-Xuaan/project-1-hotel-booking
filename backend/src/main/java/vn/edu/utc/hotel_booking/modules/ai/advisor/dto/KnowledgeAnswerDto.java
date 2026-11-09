package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.KnowledgeState;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeAnswerDto {
    KnowledgeState state;
    Integer hotelId;
    String hotelName;
    String policyQuestion;
    RagQueryResultDto result;
    Long retryAfterSeconds;
    List<HotelChoiceDto> hotelChoices;
}
