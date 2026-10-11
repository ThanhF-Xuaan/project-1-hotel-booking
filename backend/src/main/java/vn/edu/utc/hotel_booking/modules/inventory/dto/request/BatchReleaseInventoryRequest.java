package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HoldReleaseReason;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchReleaseInventoryRequest {

    @Schema(description = "Mã phiên giữ phòng (UUID) cần giải phóng", example = "550e8400-e29b-41d4-a716-446655440000")
    String sessionId;

    @Schema(description = "Lý do giải phóng phòng (USER_CANCELLED, EXPIRED, SESSION_SUPERSEDED)", example = "USER_CANCELLED")
    HoldReleaseReason releaseReason;

    @Schema(description = "ID khách sạn (tùy chọn nếu có sessionId)", example = "1")
    Short hotelId;

    @Schema(description = "Ngày check-in (tùy chọn nếu có sessionId)", example = "2026-10-15")
    LocalDate checkInDate;

    @Schema(description = "Ngày check-out (tùy chọn nếu có sessionId)", example = "2026-10-17")
    LocalDate checkOutDate;

    @Valid
    @Builder.Default
    @Schema(description = "Danh sách các hạng phòng và số lượng phòng cần mở khóa (tùy chọn nếu có sessionId)")
    List<BatchHoldInventoryRequest.RoomTypeHoldItem> items = new ArrayList<>();
}
