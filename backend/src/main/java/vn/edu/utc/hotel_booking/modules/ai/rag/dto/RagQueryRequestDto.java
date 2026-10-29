package vn.edu.utc.hotel_booking.modules.ai.rag.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagQueryRequestDto {
    @Min(1)
    @Max(32767)
    Integer hotelId;
    String hotelCode;

    @NotBlank(message = "question cannot be blank")
    @Size(max = 2000, message = "question cannot exceed 2000 characters")
    String question;

    String language;
    LocalDate asOfDate;

    @Min(value = 1, message = "topK must be at least 1")
    @Builder.Default
    @Max(value = 5, message = "topK cannot exceed 5")
    Integer topK = 5;
}
