package vn.edu.utc.hotel_booking.modules.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomAssignmentCandidateResponse {

    @Schema(description = "ID phòng vật lý", example = "101")
    Integer id;

    @Schema(description = "Số phòng / Tên phòng", example = "101A")
    String roomNumber;

    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "Tên khách sạn", example = "Viettel Boutique Sapa")
    String hotelName;

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "Mã loại phòng", example = "STD")
    String roomTypeCode;

    @Schema(description = "Tên loại phòng", example = "Standard Room (Phòng Tiêu chuẩn)")
    String roomTypeName;

    @Schema(description = "Trạng thái buồng phòng hiện tại (hôm nay)", example = "READY")
    String currentStatus;

    @Schema(description = "Cờ xác định phòng có hoàn toàn trống trong dải ngày lưu trú không (Sparse Data)", example = "true")
    boolean isAvailable;

    @Schema(description = "Cờ xác định phòng có cùng hạng với hạng phòng khách đặt không", example = "true")
    boolean isSameRoomType;

    @Schema(description = "Lý do xung đột nếu phòng bận (null nếu trống)", example = "Trùng lịch đặt phòng")
    String conflictReason;
}
