package vn.edu.utc.hotel_booking.modules.operation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SchedulerTriggerResult {

    @Schema(description = "Mã tác vụ vừa thực thi", example = "ROOM_AVAILABILITY_ROLL")
    String taskKey;

    @Schema(description = "Tên tác vụ", example = "Cuộn chân trời tồn kho buồng phòng (Daily Rolling Horizon 365 Days)")
    String taskName;

    @Schema(description = "Thành công hay thất bại", example = "true")
    boolean success;

    @Schema(description = "Thông điệp phản hồi kết quả", example = "Đã kích hoạt và hoàn tất cuộn tồn kho thành công")
    String message;

    @Schema(description = "Số lượng bản ghi hoặc đối tượng được xử lý", example = "365")
    Integer processedCount;

    @Schema(description = "Thời điểm thực thi")
    @Builder.Default
    OffsetDateTime executedAt = OffsetDateTime.now();
}

