package vn.edu.utc.hotel_booking.modules.ai.rag.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagRollbackRequestDto {
    @NotBlank(message = "sourceId cannot be blank")
    String sourceId;

    @Min(value = 1, message = "targetVersion must be >= 1")
    Integer targetVersion;

    @NotNull(message = "scopeType cannot be null")
    RagScopeType scopeType;

    Integer hotelId;
}
