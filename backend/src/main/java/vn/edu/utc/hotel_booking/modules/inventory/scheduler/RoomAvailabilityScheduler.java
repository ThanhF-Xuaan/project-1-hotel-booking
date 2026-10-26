package vn.edu.utc.hotel_booking.modules.inventory.scheduler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityGenerationService;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomAvailabilityScheduler {

    RoomAvailabilityGenerationService generationService;

    /**
     * Chạy định kỳ lúc 00:05 mỗi ngày: Tự động cuộn chân trời tồn kho lên 365 ngày tiếp theo
     * cho toàn bộ các hạng phòng ACTIVE trong toàn bộ chuỗi khách sạn.
     */
    @Scheduled(cron = "${app.scheduler.room-availability-roll.cron:0 5 0 * * ?}")
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
