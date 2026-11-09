package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupRoomBlockService;
import vn.edu.utc.hotel_booking.modules.identity.scheduler.DeadLetterScheduler;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.SystemSchedulerSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.SchedulerTriggerResult;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.SystemSchedulerTaskResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.SystemSchedulerService;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemSchedulerServiceImpl implements SystemSchedulerService {

    RoomAvailabilityGenerationService roomAvailabilityGenerationService;
    RoomAvailabilityService roomAvailabilityService;
    GroupRoomBlockService groupRoomBlockService;
    DeadLetterScheduler deadLetterScheduler;
    JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<SystemSchedulerTaskResponse> getSchedulerTasks(SystemSchedulerSearchDto searchDto) {
        Map<String, OffsetDateTime> activeLocks = getActiveShedLocks();
        List<SystemSchedulerTaskResponse> allTasks = buildRegistry();

        return allTasks.stream()
                .peek(task -> {
                    if (task.getShedLockName() != null && activeLocks.containsKey(task.getShedLockName())) {
                        task.setLockStatus("RUNNING_LOCKED");
                        task.setLockedUntil(activeLocks.get(task.getShedLockName()));
                    } else {
                        task.setLockStatus("IDLE");
                    }
                })
                .filter(task -> {
                    if (searchDto == null) return true;
                    if (searchDto.getKeyword() != null && !searchDto.getKeyword().isBlank()) {
                        String kw = searchDto.getKeyword().trim().toLowerCase();
                        boolean matchName = task.getTaskName().toLowerCase().contains(kw);
                        boolean matchKey = task.getTaskKey().toLowerCase().contains(kw);
                        boolean matchDesc = task.getDescription() != null && task.getDescription().toLowerCase().contains(kw);
                        if (!matchName && !matchKey && !matchDesc) return false;
                    }
                    if (searchDto.getCategory() != null && !searchDto.getCategory().isBlank()) {
                        if (!task.getCategory().equalsIgnoreCase(searchDto.getCategory().trim())) return false;
                    }
                    if (searchDto.getAllowManualTrigger() != null) {
                        if (task.isAllowManualTrigger() != searchDto.getAllowManualTrigger()) return false;
                    }
                    return true;
                })
                .toList();
    }

    @Override
    @Transactional
    public SchedulerTriggerResult triggerTask(String taskKey) {
        if (taskKey == null || taskKey.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mã tác vụ không được để trống");
        }

        SystemSchedulerTaskResponse task = buildRegistry().stream()
                .filter(t -> t.getTaskKey().equalsIgnoreCase(taskKey.trim()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST_DATA, "Không tìm thấy tác vụ có mã: " + taskKey));

        if (!task.isAllowManualTrigger()) {
            String msg = (task.getAutomaticPatternMessage() != null && !task.getAutomaticPatternMessage().isBlank())
                    ? task.getAutomaticPatternMessage()
                    : task.getTaskName() + " được kích hoạt tự động";
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Tác vụ này không cho phép kích hoạt thủ công! " + msg);
        }

        log.info("[Scheduler Manual Trigger] Người dùng kích hoạt thủ công tác vụ: {} ({})", task.getTaskName(), task.getTaskKey());

        int processed = 0;
        String resultMessage;

        switch (task.getTaskKey().toUpperCase()) {
            case "ROOM_AVAILABILITY_ROLL" -> {
                processed = roomAvailabilityGenerationService.rollHorizonForAllActiveRoomTypes(365);
                resultMessage = String.format("Đã hoàn tất cuộn tồn kho cho toàn bộ hạng phòng ACTIVE. Tổng số bản ghi sinh mới: %d ngày.", processed);
            }
            case "RELEASE_EXPIRED_HOLDS" -> {
                processed = roomAvailabilityService.releaseExpiredHolds();
                resultMessage = String.format("Đã tự động thu hồi và giải phóng %d lượt giữ phòng tạm thời quá hạn về quỹ phòng.", processed);
            }
            case "GROUP_CUTOFF_AUTO_RELEASE" -> {
                processed = groupRoomBlockService.processCutoffAutoRelease();
                resultMessage = String.format("Đã quét và giải phóng thành công %d phòng đoàn quá hạn Cut-off Date về Public Inventory.", processed);
            }
            case "GROUP_DEPOSIT_EXPIRED_CANCEL" -> {
                processed = groupRoomBlockService.processDepositExpiredAutoCancel();
                resultMessage = String.format("Đã quét và hủy %d hợp đồng đoàn TENTATIVE quá hạn đặt cọc 3 ngày.", processed);
            }
            case "GROUP_CUTOFF_REMINDERS" -> {
                processed = groupRoomBlockService.processCutoffReminders();
                resultMessage = String.format("Đã gửi cảnh báo nhắc nhở hạn chót Cut-off Date cho %d hợp đồng đoàn sắp đến hạn.", processed);
            }
            case "KEYCLOAK_DEAD_LETTER_RETRY" -> {
                deadLetterScheduler.processDeadLetters();
                resultMessage = "Đã thực thi tiến trình xử lý lại các bản ghi Keycloak Dead Letter đang ở trạng thái PENDING.";
            }
            default -> throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Chưa hỗ trợ thực thi cho mã tác vụ: " + taskKey);
        }

        return SchedulerTriggerResult.builder()
                .taskKey(task.getTaskKey())
                .taskName(task.getTaskName())
                .success(true)
                .message(resultMessage)
                .processedCount(processed)
                .executedAt(OffsetDateTime.now())
                .build();
    }

    private Map<String, OffsetDateTime> getActiveShedLocks() {
        Map<String, OffsetDateTime> map = new HashMap<>();
        try {
            jdbcTemplate.query("SELECT name, lock_until FROM shedlock WHERE lock_until > CURRENT_TIMESTAMP", (rs) -> {
                String name = rs.getString("name");
                Timestamp ts = rs.getTimestamp("lock_until");
                if (name != null && ts != null) {
                    map.put(name, ts.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
                }
            });
        } catch (Exception ex) {
            log.warn("Không thể tra cứu trạng thái shedlock thời gian thực: {}", ex.getMessage());
        }
        return map;
    }

    private List<SystemSchedulerTaskResponse> buildRegistry() {
        return List.of(
                SystemSchedulerTaskResponse.builder()
                        .taskKey("ROOM_AVAILABILITY_ROLL")
                        .taskName("Cuộn chân trời tồn kho buồng phòng (Daily Rolling Horizon 365 Days)")
                        .category("INVENTORY")
                        .cronOrInterval("00:05 mỗi ngày (0 5 0 * * ?)")
                        .shedLockName("roomAvailabilityRollingHorizonTask")
                        .hasDistributedLock(true)
                        .allowManualTrigger(true)
                        .automaticPatternMessage(null)
                        .description("Tự động quét tất cả hạng phòng ACTIVE trong chuỗi và cuộn thêm 1 ngày ở tương lai, bảo đảm chân trời mở bán liên tục 365 ngày.")
                        .build(),

                SystemSchedulerTaskResponse.builder()
                        .taskKey("RELEASE_EXPIRED_HOLDS")
                        .taskName("Thu hồi lượt giữ phòng tạm thời quá hạn (Release Expired Holds)")
                        .category("INVENTORY")
                        .cronOrInterval("Mỗi 60 giây (fixedRate = 60s)")
                        .shedLockName("roomHoldReleaseTask")
                        .hasDistributedLock(true)
                        .allowManualTrigger(true)
                        .automaticPatternMessage(null)
                        .description("Quét và hoàn trả lại các phòng đang ở trạng thái giữ chỗ tạm thời (Hold) quá thời gian chờ thanh toán (15 phút) vào quỹ phòng mở bán.")
                        .build(),

                SystemSchedulerTaskResponse.builder()
                        .taskKey("GROUP_CUTOFF_AUTO_RELEASE")
                        .taskName("Thu hồi phòng đoàn dôi dư quá hạn Cut-off (Group Cut-off Auto Release)")
                        .category("BOOKING")
                        .cronOrInterval("Mỗi giờ tại phút 00 (0 0 * * * ?)")
                        .shedLockName("groupCutoffAutoReleaseTask")
                        .hasDistributedLock(true)
                        .allowManualTrigger(true)
                        .automaticPatternMessage(null)
                        .description("Tự động thu hồi các phòng khách đoàn chưa nộp danh sách khách khi đến hoặc quá ngày Cut-off Date, hoàn trả về Public Inventory bán lẻ theo hợp đồng.")
                        .build(),

                SystemSchedulerTaskResponse.builder()
                        .taskKey("GROUP_DEPOSIT_EXPIRED_CANCEL")
                        .taskName("Hủy hợp đồng đoàn quá hạn đặt cọc (Group Deposit Expiry Cancel)")
                        .category("BOOKING")
                        .cronOrInterval("Mỗi giờ tại phút 15 (0 15 * * * ?)")
                        .shedLockName("groupDepositExpiryTask")
                        .hasDistributedLock(true)
                        .allowManualTrigger(true)
                        .automaticPatternMessage(null)
                        .description("Tự động hủy các hợp đồng đoàn ở trạng thái TENTATIVE quá hạn đặt cọc 3 ngày giữ chỗ theo quy định hợp đồng và Bộ Luật Dân sự 2015.")
                        .build(),

                SystemSchedulerTaskResponse.builder()
                        .taskKey("GROUP_CUTOFF_REMINDERS")
                        .taskName("Gửi cảnh báo nhắc nhở hạn chót Cut-off Đoàn (Group Cut-off Reminder)")
                        .category("BOOKING")
                        .cronOrInterval("08:30 sáng mỗi ngày (0 30 8 * * ?)")
                        .shedLockName("groupCutoffReminderTask")
                        .hasDistributedLock(true)
                        .allowManualTrigger(true)
                        .automaticPatternMessage(null)
                        .description("Quét và gửi cảnh báo nhắc nhở trước 24h - 48h cho Sales Staff phụ trách đoàn để giục khách nộp Rooming List hoặc hoàn tất thanh toán.")
                        .build(),

                SystemSchedulerTaskResponse.builder()
                        .taskKey("KEYCLOAK_DEAD_LETTER_RETRY")
                        .taskName("Xử lý lại lỗi đồng bộ tài khoản Keycloak (Dead Letter Retry)")
                        .category("IDENTITY")
                        .cronOrInterval("Mỗi 15 phút (fixedDelay = 15m)")
                        .shedLockName("deadLetterTask")
                        .hasDistributedLock(true)
                        .allowManualTrigger(true)
                        .automaticPatternMessage(null)
                        .description("Tự động xử lý lại các bản ghi đồng bộ Keycloak bị lỗi (vô hiệu hóa, đăng xuất phiên làm việc) khi khôi phục kết nối IAM theo Nghị định 13/2023/NĐ-CP.")
                        .build(),

                SystemSchedulerTaskResponse.builder()
                        .taskKey("ASYNC_IAM_WORKER")
                        .taskName("Luồng nền xử lý sự kiện tài khoản IAM (Async IAM Worker)")
                        .category("IDENTITY")
                        .cronOrInterval("Khi phát sinh sự kiện nhân sự (Event-driven)")
                        .shedLockName("iamTaskExecutorPool")
                        .hasDistributedLock(true)
                        .allowManualTrigger(false)
                        .automaticPatternMessage("Tác vụ được kích hoạt tự động theo sự kiện thay đổi dữ liệu nhân sự")
                        .description("Tiến trình bất đồng bộ thực hiện khóa và đăng xuất phiên tài khoản trên Keycloak khi có sự kiện thay đổi hoặc xóa nhân sự.")
                        .build(),

                SystemSchedulerTaskResponse.builder()
                        .taskKey("NIGHT_AUDIT_CLOSING")
                        .taskName("Tiến trình đóng ngày kế toán khách sạn (Night Audit Routine)")
                        .category("OPERATION")
                        .cronOrInterval("Định kỳ ca đêm (03:00 - 05:00 sáng)")
                        .shedLockName("nightAuditSystemLock")
                        .hasDistributedLock(true)
                        .allowManualTrigger(false)
                        .automaticPatternMessage("Tác vụ được kích hoạt tự động vào cuối ngày hoặc thực hiện qua phân hệ Đóng ngày chuyên biệt")
                        .description("Chốt sổ kế toán ngày, chuyển trạng thái No-Show, tính phí đêm tự động vào Folio. Tuân thủ Luật Kế toán 2015, tác vụ chỉ được thực hiện qua màn hình Đóng ngày chuyên biệt với quyền hạn được cấp phép.")
                        .build()
        );
    }
}

