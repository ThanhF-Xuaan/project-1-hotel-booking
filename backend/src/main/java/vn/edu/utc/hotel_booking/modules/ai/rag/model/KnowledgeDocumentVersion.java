package vn.edu.utc.hotel_booking.modules.ai.rag.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KnowledgeDocumentVersion {
    UUID documentId;
    String sourceId;
    int version;
    RagScopeType scopeType;
    Integer hotelId;
    RagDocumentCategory category;
    String title;
    String rawContent;
    String contentHash;
    RagVersionStatus status;
    LocalDate effectiveFrom;
    LocalDate effectiveTo;
    String embeddingModelId;
    int embeddingDimension;
    String chunkingVersion;
    Instant createdAt;
    Instant activatedAt;
}
