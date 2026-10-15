package vn.edu.utc.hotel_booking.modules.ai.rag.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagDeleteRequestDto {
    String sourceId;
    int version;
    RagScopeType scopeType;
    Integer hotelId;
}
