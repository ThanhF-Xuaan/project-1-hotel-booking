package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CriteriaConfirmation;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CriteriaOrigin;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CriteriaEvidenceDto {
    String field;
    CriteriaOrigin origin;
    CriteriaConfirmation confirmation;
}
