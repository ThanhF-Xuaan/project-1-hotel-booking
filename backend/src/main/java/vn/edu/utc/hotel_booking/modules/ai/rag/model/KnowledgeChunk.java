package vn.edu.utc.hotel_booking.modules.ai.rag.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KnowledgeChunk {
    UUID chunkId;
    UUID documentId;
    String sourceId;
    int version;
    int chunkIndex;
    RagScopeType scopeType;
    Integer hotelId;
    RagDocumentCategory category;
    String content;
    String sanitizedContent;
    float[] embedding;
    LocalDate effectiveFrom;
    LocalDate effectiveTo;
    RagVersionStatus status;

    @Builder.Default
    List<String> warnings = new ArrayList<>();
}
