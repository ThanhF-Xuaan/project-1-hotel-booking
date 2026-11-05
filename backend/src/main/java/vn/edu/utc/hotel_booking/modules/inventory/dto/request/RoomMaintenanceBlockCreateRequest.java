package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomMaintenanceBlockCreateRequest {

    @NotNull(message = "ID phòng vật lý không được để trống")
    @Schema(description = "ID phòng vật lý", example = "1")
    Integer roomInstanceId;

    @NotBlank(message = "Loại block không được để trống")
    @Pattern(regexp = "^(OOO|OOS)$", message = "Loại block phải là OOO hoặc OOS")
    @Schema(description = "Loại khóa phòng (OOO: Out of Order, OOS: Out of Service)", example = "OOO")
    String blockType;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Schema(description = "Ngày bắt đầu", example = "2026-10-20")
    LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Schema(description = "Ngày kết thúc", example = "2026-10-22")
    LocalDate endDate;

    @NotBlank(message = "Lý do bảo trì không được để trống")
    @Schema(description = "Lý do khóa phòng / bảo trì", example = "Sửa đường ống nước và sơn tường")
    String reason;

    @Schema(description = "Ghi chú thêm", example = "Dự kiến thợ sửa chữa vào sáng 20")
    String notes;

    @Schema(description = "Mã phiếu bảo trì (nếu có)")
    Long maintenanceTicketId;
}
