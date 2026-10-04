package vn.edu.utc.hotel_booking.modules.identity.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.identity.entity.KeycloakSyncDeadLetter;
import vn.edu.utc.hotel_booking.modules.identity.repository.KeycloakSyncDeadLetterRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.KeycloakService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterSchedulerTest {

    @Mock
    private KeycloakSyncDeadLetterRepository deadLetterRepository;

    @Mock
    private KeycloakService keycloakService;

    @InjectMocks
    private DeadLetterScheduler deadLetterScheduler;

    @Test
    @DisplayName("Process dead letters successfully updates status to PROCESSED")
    void processDeadLetters_Success() {
        UUID id = UUID.randomUUID();
        KeycloakSyncDeadLetter letter = KeycloakSyncDeadLetter.builder()
                .id(1L)
                .keycloakId(id)
                .action("DISABLE_AND_LOGOUT")
                .status("PENDING")
                .retryCount(3)
                .build();

        when(deadLetterRepository.findByStatusOrderByCreatedAtDesc("PENDING"))
                .thenReturn(List.of(letter));

        deadLetterScheduler.processDeadLetters();

        verify(keycloakService).disableUser(id);
        verify(keycloakService).logoutUser(id);
        assertThat(letter.getStatus()).isEqualTo("PROCESSED");
        assertThat(letter.getErrorMessage()).isNull();
        verify(deadLetterRepository).save(letter);
    }

    @Test
    @DisplayName("Process dead letters handles failure by incrementing retry count")
    void processDeadLetters_Failure_IncrementsRetryCount() {
        UUID id = UUID.randomUUID();
        KeycloakSyncDeadLetter letter = KeycloakSyncDeadLetter.builder()
                .id(2L)
                .keycloakId(id)
                .action("DISABLE_AND_LOGOUT")
                .status("PENDING")
                .retryCount(3)
                .build();

        when(deadLetterRepository.findByStatusOrderByCreatedAtDesc("PENDING"))
                .thenReturn(List.of(letter));
        doThrow(new RuntimeException("Keycloak unavailable")).when(keycloakService).disableUser(id);

        deadLetterScheduler.processDeadLetters();

        assertThat(letter.getStatus()).isEqualTo("PENDING");
        assertThat(letter.getRetryCount()).isEqualTo(4);
        assertThat(letter.getErrorMessage()).contains("Scheduler retry error: Keycloak unavailable");
        verify(deadLetterRepository).save(letter);
    }
}
