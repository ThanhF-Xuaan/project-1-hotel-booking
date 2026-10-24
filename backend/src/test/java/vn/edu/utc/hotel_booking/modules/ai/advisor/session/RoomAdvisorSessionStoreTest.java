package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaEvidenceDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CriteriaConfirmation;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CriteriaOrigin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomAdvisorSessionStoreTest {

    private InMemoryRoomAdvisorSessionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryRoomAdvisorSessionStore();
    }

    @Test
    @DisplayName("Should create new session when sessionId is null")
    void shouldCreateNewSession() {
        RoomAdvisorSession session = store.getOrCreateSession(null, "user-123");

        assertThat(session).isNotNull();
        assertThat(session.getSessionId()).isNotNull();
        assertThat(session.getSessionVersion()).isEqualTo(1L);
        assertThat(session.getOwnerKey()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("Should retrieve existing session for matching ownerKey")
    void shouldRetrieveExistingSession() {
        RoomAdvisorSession created = store.getOrCreateSession(null, "user-123");
        UUID sid = created.getSessionId();

        Optional<RoomAdvisorSession> retrieved = store.getSession(sid, "user-123");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getSessionId()).isEqualTo(sid);
    }

    @Test
    @DisplayName("Should reject cross-owner session access with SessionSecurityException")
    void shouldRejectCrossOwnerSessionAccess() {
        RoomAdvisorSession created = store.getOrCreateSession(null, "user-123");
        UUID sid = created.getSessionId();

        assertThatThrownBy(() -> store.getSession(sid, "attacker-999"))
                .isInstanceOf(SessionSecurityException.class)
                .hasMessageContaining("owner mismatch");

        assertThatThrownBy(() -> store.getOrCreateSession(sid, "attacker-999"))
                .isInstanceOf(SessionSecurityException.class)
                .hasMessageContaining("owner mismatch");
    }

    @Test
    @DisplayName("Should update session and increment version")
    void shouldUpdateSessionAndIncrementVersion() {
        RoomAdvisorSession created = store.getOrCreateSession(null, "user-123");
        UUID sid = created.getSessionId();

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Hà Nội")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .build();

        CriteriaEvidenceDto ev = CriteriaEvidenceDto.builder()
                .field("adults")
                .origin(CriteriaOrigin.USER_PROVIDED)
                .confirmation(CriteriaConfirmation.CONFIRMED)
                .build();

        RoomAdvisorSession updated = store.updateSession(
                sid,
                "user-123",
                1L, // matching expected version
                criteria,
                List.of(ev),
                AdvisorResultDto.builder().reply("Success").build()
        );

        assertThat(updated.getSessionVersion()).isEqualTo(2L);
        assertThat(updated.getCriteria().getLocationKeyword()).isEqualTo("Hà Nội");
        assertThat(updated.getCriteriaProvenance()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject update on version conflict (409)")
    void shouldRejectUpdateOnVersionConflict() {
        RoomAdvisorSession created = store.getOrCreateSession(null, "user-123");
        UUID sid = created.getSessionId();

        assertThatThrownBy(() -> store.updateSession(
                sid,
                "user-123",
                99L, // wrong expected version
                null,
                null,
                null
        )).isInstanceOf(SessionVersionConflictException.class)
                .hasMessageContaining("Session version conflict");
    }

    @Test
    @DisplayName("Should reject update with SessionNotFoundException when session does not exist")
    void shouldRejectUpdateWhenSessionNotFound() {
        assertThatThrownBy(() -> store.updateSession(
                UUID.randomUUID(),
                "user-123",
                null,
                null,
                null,
                null
        )).isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Session not found or expired");
    }

    @Test
    @DisplayName("Concurrent updateSession calls with same expectedVersion: exactly one succeeds and one receives 409")
    void concurrentUpdateWithSameExpectedVersionShouldConflict() throws Exception {
        RoomAdvisorSession created = store.getOrCreateSession(null, "user-123");
        UUID sid = created.getSessionId();
        long initialVersion = created.getSessionVersion(); // 1L

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedExceptions = new CopyOnWriteArrayList<>();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                    store.updateSession(
                            sid,
                            "user-123",
                            initialVersion,
                            RoomSearchCriteriaDto.builder().locationKeyword("City " + idx).build(),
                            null,
                            null
                    );
                    successCount.incrementAndGet();
                } catch (SessionVersionConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedExceptions.add(t);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(unexpectedExceptions).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        Optional<RoomAdvisorSession> finalSession = store.getSession(sid, "user-123");
        assertThat(finalSession).isPresent();
        assertThat(finalSession.get().getSessionVersion()).isEqualTo(initialVersion + 1);
    }

    @Test
    @DisplayName("Atomic computeIfPresent in getSession should not remove newly recreated session")
    void expiredSessionRemovalRaceShouldNotDeleteNewSession() {
        RoomAdvisorSession session = store.getOrCreateSession(null, "user-123");
        UUID sid = session.getSessionId();

        // Expire the session in-place
        session.setLastAccessedAt(java.time.Instant.now().minus(java.time.Duration.ofHours(1)));

        // Simulate concurrent thread recreating a fresh active session for sid
        RoomAdvisorSession newSession = store.getOrCreateSession(sid, "user-123");
        assertThat(newSession.getSessionVersion()).isEqualTo(1L);

        // getSession on sid finds the new active session, leaving it intact
        Optional<RoomAdvisorSession> retrieved = store.getSession(sid, "user-123");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getSessionId()).isEqualTo(sid);
        assertThat(retrieved.get()).isSameAs(newSession);
    }

    @Test
    @DisplayName("Concurrent expiry, refresh, and replacement preserves consistency without swallowing exceptions")
    void concurrentExpiryRefreshAndReplacement() throws Exception {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Throwable> unexpectedExceptions = new CopyOnWriteArrayList<>();

        UUID sid = store.getOrCreateSession(null, "user-123").getSessionId();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                    if (idx % 2 == 0) {
                        store.getOrCreateSession(sid, "user-123");
                    } else {
                        store.getSession(sid, "user-123");
                    }
                } catch (Throwable t) {
                    unexpectedExceptions.add(t);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(unexpectedExceptions).isEmpty();
        assertThat(store.getSession(sid, "user-123")).isPresent();
    }
}
