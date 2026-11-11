package vn.edu.utc.hotel_booking.modules.inventory.scheduler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomMaintenanceBlockService;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(name = "hotel.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class RoomAvailabilityScheduler {

    RoomAvailabilityGenerationService generationService;
    RoomMaintenanceBlockService roomMaintenanceBlockService;

    /**
     * Chạy định kỳ mỗi phút:
     * Tự động quét và giải phóng các lệnh khóa phòng bảo trì (OOO/OOS) đã quá hạn (endDate < hôm nay),
     * hoàn tất lệnh khóa và chuyển trạng thái buồng phòng về READY.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    public void scheduleAutoExpireMaintenanceBlocks() {
        try {
            int expiredCount = roomMaintenanceBlockService.autoExpireOverdueBlocks();
            if (expiredCount > 0) {
                log.info("Scheduler: Đã tự động giải phóng {} lệnh khóa phòng quá hạn.", expiredCount);
            }
        } catch (Exception ex) {
            log.error("Scheduler: Lỗi tự động giải phóng lệnh khóa phòng quá hạn: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Chạy định kỳ lúc 00:05 mỗi ngày theo múi giờ Việt Nam (Asia/Ho_Chi_Minh):
     * Tự động cuộn chân trời tồn kho lên 365 ngày tiếp theo
     * cho toàn bộ các hạng phòng ACTIVE trong toàn bộ chuỗi khách sạn.
     * Sử dụng ShedLock để phân tán khóa trên môi trường đa Node/Pod, chống chạy trùng lặp.
     */
    @Scheduled(
            cron = "${app.scheduler.room-availability-roll.cron:0 5 0 * * ?}",
            zone = "Asia/Ho_Chi_Minh"
    )
    @SchedulerLock(
            name = "roomAvailabilityRollingHorizonTask",
            lockAtLeastFor = "30s",
            lockAtMostFor = "15m"
    )
    public void scheduleDailyRollingHorizon() {
        log.info("Bắt đầu tiến trình tự động cuộn chân trời tồn kho buồng phòng (Daily Rolling Horizon 365 Days)...");
        try {
            int generated = generationService.rollHorizonForAllActiveRoomTypes(365);
            log.info("Tiến trình cuộn tồn kho hoàn tất thành công. Số ngày bổ sung: {}", generated);
        } catch (Exception ex) {
            log.error("Tiến trình cuộn tồn kho gặp lỗi: {}", ex.getMessage(), ex);
        }
    }
}
