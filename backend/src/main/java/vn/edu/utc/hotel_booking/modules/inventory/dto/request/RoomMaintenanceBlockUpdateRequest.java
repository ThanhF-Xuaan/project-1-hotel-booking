package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomMaintenanceBlockUpdateRequest {

    @Pattern(regexp = "^(OOO|OOS)$", message = "Loại block phải là OOO hoặc OOS")
    @Schema(description = "Loại khóa phòng (OOO: Out of Order, OOS: Out of Service)", example = "OOO")
    String blockType;

    @Schema(description = "Ngày bắt đầu", example = "2026-10-20")
    LocalDate startDate;

    @Schema(description = "Ngày kết thúc", example = "2026-10-22")
    LocalDate endDate;

    @Schema(description = "Lý do khóa phòng / bảo trì", example = "Sửa đường ống nước và sơn tường")
    String reason;

    @Schema(description = "Ghi chú thêm", example = "Dự kiến thợ sửa chữa vào sáng 20")
    String notes;

    @Schema(description = "Mã phiếu bảo trì (nếu có)")
    Long maintenanceTicketId;
}
