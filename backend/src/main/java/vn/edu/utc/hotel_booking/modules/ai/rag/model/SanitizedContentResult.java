package vn.edu.utc.hotel_booking.modules.ai.rag.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SanitizedContentResult {
    String sanitizedText;
    boolean injectionDetected;
    boolean priceClaimsStripped;

    @Builder.Default
    List<String> warnings = new ArrayList<>();
}
