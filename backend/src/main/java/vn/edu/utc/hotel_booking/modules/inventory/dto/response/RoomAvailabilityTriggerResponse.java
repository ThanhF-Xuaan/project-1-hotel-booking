package vn.edu.utc.hotel_booking.modules.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Kết quả kích hoạt thủ công sinh tồn kho buồng phòng")
public class RoomAvailabilityTriggerResponse {

    @Schema(description = "Phạm vi kích hoạt (ROOM_TYPE, HOTEL, CHAIN)", example = "ROOM_TYPE")
    String scope;

    @Schema(description = "ID đối tượng mục tiêu (hotelRoomTypeId hoặc hotelId, null nếu là CHAIN)", example = "10")
    Integer targetId;

    @Schema(description = "Ngày bắt đầu chân trời tồn kho", example = "2026-11-01")
    LocalDate startDate;

    @Schema(description = "Số ngày của chân trời tồn kho", example = "365")
    int horizonDays;

    @Schema(description = "Số lượng bản ghi ngày được tạo mới thực tế", example = "365")
    int recordsGenerated;

    @Schema(description = "Trạng thái thực thi", example = "SUCCESS")
    String status;

    @Schema(description = "Thông báo chi tiết", example = "Đã sinh sẵn thành công 365 bản ghi tồn kho cho hạng phòng ID: 10")
    String message;

    @Schema(description = "Thời điểm thực thi")
    OffsetDateTime triggeredAt;
}
