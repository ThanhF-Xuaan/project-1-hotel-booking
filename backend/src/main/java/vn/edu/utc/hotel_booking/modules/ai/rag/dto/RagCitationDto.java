package vn.edu.utc.hotel_booking.modules.ai.rag.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagCitationDto {
    String sourceId;
    int version;
    UUID chunkId;
    RagDocumentCategory category;
    Integer hotelId;
    LocalDate effectiveFrom;
    String snippet;
    double similarityScore;
}
