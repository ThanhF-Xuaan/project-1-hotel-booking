package vn.edu.utc.hotel_booking.modules.ai.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "sourceId cannot be blank")
    String sourceId;

    @NotNull(message = "scopeType cannot be null")
    RagScopeType scopeType;

    Integer hotelId;

    @NotNull(message = "category cannot be null")
    RagDocumentCategory category;

    @NotBlank(message = "title cannot be blank")
    String title;

    @NotBlank(message = "content cannot be blank")
    String content;

    LocalDate effectiveFrom;
    LocalDate effectiveTo;
}
