package vn.edu.utc.hotel_booking.modules.ai.rag.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagIngestResultDto {
    UUID documentId;
    String sourceId;
    int version;
    RagVersionStatus status;
    int chunkCount;
    String contentHash;

    @Builder.Default
    List<String> warnings = new ArrayList<>();

    String errorMessage;
}
