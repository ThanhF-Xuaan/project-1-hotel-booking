package vn.edu.utc.hotel_booking.modules.ai.rag.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagIngestRequestDto {
    String sourceId;
    RagScopeType scopeType;
    Integer hotelId;
    RagDocumentCategory category;
    String title;
    String content;
    LocalDate effectiveFrom;
    LocalDate effectiveTo;
}
