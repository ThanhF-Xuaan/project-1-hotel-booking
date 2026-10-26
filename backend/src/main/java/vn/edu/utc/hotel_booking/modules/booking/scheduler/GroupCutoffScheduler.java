package vn.edu.utc.hotel_booking.modules.booking.scheduler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupRoomBlockService;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(name = "hotel.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class GroupCutoffScheduler {

    GroupRoomBlockService groupRoomBlockService;

    /**
     * Tự động giải phóng các phòng chưa gán tên khách khi đến hoặc quá Cut-off Date.
     * Sử dụng ShedLock để phân tán khóa trên môi trường đa Node/Pod (Kubernetes),
     * chống race condition và trùng lặp giải phóng.
     */
    @Scheduled(cron = "${app.scheduler.group-cutoff.cron:0 0 * * * ?}")
    @SchedulerLock(name = "groupCutoffAutoReleaseTask", lockAtLeastFor = "15s", lockAtMostFor = "10m")
    public void executeCutoffAutoRelease() {
        log.info("[Scheduler] Bắt đầu quét và giải phóng phòng khách đoàn quá hạn Cut-off Date...");
        try {
            int releasedCount = groupRoomBlockService.processCutoffAutoRelease();
            if (releasedCount > 0) {
                log.info("[Scheduler] Đã giải phóng thành công {} phòng đoàn dôi dư về Public Inventory", releasedCount);
            } else {
                log.info("[Scheduler] Không có phòng đoàn nào cần giải phóng tại chu kỳ này");
            }
        } catch (Exception ex) {
            log.error("[Scheduler] Lỗi khi thực hiện giải phóng phòng đoàn Cut-off: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Tự động hủy các hợp đồng TENTATIVE quá hạn đặt cọc 3 ngày giữ chỗ.
     */
    @Scheduled(cron = "${app.scheduler.group-deposit-expiry.cron:0 15 * * * ?}")
    @SchedulerLock(name = "groupDepositExpiryTask", lockAtLeastFor = "15s", lockAtMostFor = "10m")
    public void executeDepositExpiredAutoCancel() {
        log.info("[Scheduler] Bắt đầu quét các hợp đồng đoàn TENTATIVE quá hạn đặt cọc 3 ngày...");
        try {
            int cancelledCount = groupRoomBlockService.processDepositExpiredAutoCancel();
            if (cancelledCount > 0) {
                log.info("[Scheduler] Đã tự động hủy {} hợp đồng đoàn do quá hạn đặt cọc 50%", cancelledCount);
            }
        } catch (Exception ex) {
            log.error("[Scheduler] Lỗi khi xử lý hủy hợp đồng quá hạn đặt cọc: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Quét và gửi cảnh báo nhắc nhở trước 24h - 48h cho Sales Staff phụ trách đoàn.
     */
    @Scheduled(cron = "${app.scheduler.group-cutoff-reminder.cron:0 30 8 * * ?}") // 08:30 sáng mỗi ngày
    @SchedulerLock(name = "groupCutoffReminderTask", lockAtLeastFor = "15s", lockAtMostFor = "10m")
    public void executeCutoffReminders() {
        log.info("[Scheduler] Bắt đầu quét cảnh báo hạn chót Cut-off Date cho Sales Staff...");
        try {
            int remindedCount = groupRoomBlockService.processCutoffReminders();
            log.info("[Scheduler] Đã gửi cảnh báo hạn chót cho {} hợp đồng sắp đến ngày Cut-off", remindedCount);
        } catch (Exception ex) {
            log.error("[Scheduler] Lỗi khi gửi cảnh báo hạn chót Cut-off Date: {}", ex.getMessage(), ex);
        }
    }
}
