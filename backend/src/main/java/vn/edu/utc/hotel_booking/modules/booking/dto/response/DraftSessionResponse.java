package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.LeadGuestRequest;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DraftSessionResponse {

    @Schema(description = "Mã session đặt phòng nháp (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    String sessionId;

    @Schema(description = "Thời điểm hết hạn giữ phòng (TTL 30 phút)")
    OffsetDateTime expiresAt;

    @Schema(description = "Số giây còn lại của phiên", example = "1800")
    Long remainingSeconds;

    @Schema(description = "Chi tiết báo giá mới nhất của giỏ hàng")
    CartPriceQuoteResponse quote;

    @Schema(description = "Thông tin khách đại diện đã lưu")
    LeadGuestRequest leadGuest;
}
