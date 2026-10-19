package vn.edu.utc.hotel_booking.modules.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomMaintenanceBlockResponse {

    @Schema(description = "ID bản ghi khóa phòng", example = "1")
    Long id;

    @Schema(description = "ID phòng vật lý", example = "1")
    Integer roomInstanceId;

    @Schema(description = "Số phòng", example = "P301")
    String roomNumber;

    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "Tên khách sạn", example = "Grand Hotel Da Nang")
    String hotelName;

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "Tên loại phòng", example = "Phòng Deluxe Hướng Biển")
    String roomTypeName;

    @Schema(description = "Loại block (OOO/OOS)", example = "OOO")
    String blockType;

    @Schema(description = "Ngày bắt đầu", example = "2026-10-20")
    LocalDate startDate;

    @Schema(description = "Ngày kết thúc", example = "2026-10-22")
    LocalDate endDate;

    @Schema(description = "Lý do bảo trì", example = "Sửa đường ống nước")
    String reason;

    @Schema(description = "Ghi chú thêm", example = "Thợ bảo trì vào sáng 20")
    String notes;

    @Schema(description = "Trạng thái (ACTIVE, COMPLETED, CANCELLED)", example = "ACTIVE")
    String status;

    @Schema(description = "ID phiếu bảo trì (nếu có)")
    Long maintenanceTicketId;

    @Schema(description = "ID nhân viên tạo")
    Integer createdByStaffId;

    @Schema(description = "ID nhân viên cập nhật/kết thúc")
    Integer updatedByStaffId;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
