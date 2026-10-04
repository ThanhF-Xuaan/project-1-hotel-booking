package vn.edu.utc.hotel_booking.modules.identity.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.identity.entity.KeycloakSyncDeadLetter;
import vn.edu.utc.hotel_booking.modules.identity.repository.KeycloakSyncDeadLetterRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.KeycloakService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterScheduler {

    private final KeycloakSyncDeadLetterRepository deadLetterRepository;
    private final KeycloakService keycloakService;

    /**
     * Chạy định kỳ mỗi 15 phút (900.000 ms) để xử lý lại các bản ghi đồng bộ Keycloak bị lỗi.
     * Sử dụng ShedLock để phân tán khóa trên môi trường đa Node/Pod, chống trùng lặp execution.
     */
    @Scheduled(fixedDelay = 900000)
    @SchedulerLock(name = "deadLetterTask", lockAtLeastFor = "30s", lockAtMostFor = "14m")
    @Transactional
    public void processDeadLetters() {
        List<KeycloakSyncDeadLetter> deadLetters = deadLetterRepository.findByStatusOrderByCreatedAtDesc("PENDING");

        if (deadLetters.isEmpty()) {
            return;
        }

        log.info("Bắt đầu xử lý {} bản ghi Keycloak Dead Letter đang ở trạng thái PENDING", deadLetters.size());

        for (KeycloakSyncDeadLetter letter : deadLetters) {
            try {
                if ("DISABLE_AND_LOGOUT".equals(letter.getAction())) {
                    keycloakService.disableUser(letter.getKeycloakId());
                    keycloakService.logoutUser(letter.getKeycloakId());
                }
                letter.setStatus("PROCESSED");
                letter.setErrorMessage(null);
                deadLetterRepository.save(letter);
                log.info("Xử lý thành công Dead Letter cho keycloakId: {}", letter.getKeycloakId());
            } catch (Exception ex) {
                letter.setRetryCount(letter.getRetryCount() + 1);
                letter.setErrorMessage("Scheduler retry error: " + ex.getMessage());
                deadLetterRepository.save(letter);
                log.error("Xử lý thất bại Dead Letter cho keycloakId: {}, retryCount={}", letter.getKeycloakId(), letter.getRetryCount(), ex);
            }
        }
    }
}
