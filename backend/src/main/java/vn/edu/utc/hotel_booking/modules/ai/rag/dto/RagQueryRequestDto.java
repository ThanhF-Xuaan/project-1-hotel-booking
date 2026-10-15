package vn.edu.utc.hotel_booking.modules.ai.rag.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagQueryRequestDto {
    Integer hotelId;
    String hotelCode;
    String question;
    String language;
    LocalDate asOfDate;

    @Builder.Default
    Integer topK = 5;
}
