package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HoldReleaseReason;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancelSessionRequest {

    @NotBlank(message = "Mã session không được để trống")
    @Schema(description = "Mã draftSessionId hoặc paymentSessionId cần hủy", example = "550e8400-e29b-41d4-a716-446655440000")
    String sessionId;

    @Schema(description = "Lý do hủy (USER_CANCELLED, EXPIRED, SESSION_SUPERSEDED)", example = "USER_CANCELLED")
    @Builder.Default
    HoldReleaseReason reason = HoldReleaseReason.USER_CANCELLED;
}
