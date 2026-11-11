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
public class SystemSchedulerTaskResponse {

    @Schema(description = "Mã định danh tác vụ", example = "ROOM_AVAILABILITY_ROLL")
    String taskKey;

    @Schema(description = "Tên hiển thị tiếng Việt", example = "Cuộn chân trời tồn kho buồng phòng (Daily Rolling Horizon 365 Days)")
    String taskName;

    @Schema(description = "Phân hệ nghiệp vụ (INVENTORY, BOOKING, IDENTITY, OPERATION)", example = "INVENTORY")
    String category;

    @Schema(description = "Biểu thức Cron hoặc chu kỳ lặp", example = "00:05 mỗi ngày (0 5 0 * * ?)")
    String cronOrInterval;

    @Schema(description = "Tên khóa phân tán ShedLock", example = "roomAvailabilityRollingHorizonTask")
    String shedLockName;

    @Schema(description = "Đã được bảo vệ bằng Distributed Lock (ShedLock)", example = "true")
    @Builder.Default
    boolean hasDistributedLock = true;

    @Schema(description = "Cho phép kích hoạt thủ công từ giao diện", example = "true")
    boolean allowManualTrigger;

    @Schema(description = "Thông điệp pattern khi không cho phép kích hoạt thủ công", example = "Tác vụ được kích hoạt tự động theo sự kiện thay đổi dữ liệu nhân sự")
    String automaticPatternMessage;

    @Schema(description = "Mô tả chi tiết tác dụng nghiệp vụ")
    String description;

    @Schema(description = "Trạng thái khóa hiện tại (IDLE hoặc RUNNING_LOCKED)", example = "IDLE")
    @Builder.Default
    String lockStatus = "IDLE";

    @Schema(description = "Thời điểm khóa kết thúc nếu đang chạy")
    OffsetDateTime lockedUntil;
}

