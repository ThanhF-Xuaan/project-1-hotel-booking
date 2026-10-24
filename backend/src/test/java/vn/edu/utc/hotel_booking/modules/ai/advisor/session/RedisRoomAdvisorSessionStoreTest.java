package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaEvidenceDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CriteriaConfirmation;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CriteriaOrigin;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RedisRoomAdvisorSessionStore} (AC-4.3, AC-4.4, AC-4.5).
 *
 * <p>Validates the optimistic locking command sequence (WATCH -> READ/VALIDATE -> MULTI -> SET -> EXEC),
 * keyspace restrictions (advisor:session:*), bounded retries, and error mapping to
 * {@link SessionStorageUnavailableException}.</p>
 */
@ExtendWith(MockitoExtension.class)
class RedisRoomAdvisorSessionStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RedisRoomAdvisorSessionStore store;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        store = new RedisRoomAdvisorSessionStore(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("AC-4.3: Session keyspace must be exactly advisor:session:{sessionId} with TTL 1800s")
    void shouldEnforceApprovedKeyspaceAndTtlOnCreate() {
        UUID sid = UUID.randomUUID();
        String expectedKey = "advisor:session:" + sid;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);

        RoomAdvisorSession created = store.getOrCreateSession(sid, "owner:user-1");

        assertThat(created).isNotNull();
        assertThat(created.getSessionId()).isEqualTo(sid);
        assertThat(created.getSessionVersion()).isEqualTo(1L);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), anyString(), ttlCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo(expectedKey);
        assertThat(keyCaptor.getValue()).startsWith("advisor:session:");
        assertThat(keyCaptor.getValue()).doesNotStartWith("session:");
        assertThat(keyCaptor.getValue()).doesNotStartWith("t12:advisor:");
        assertThat(keyCaptor.getValue()).doesNotStartWith("booking:");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(1800));
    }

    @Test
    @DisplayName("AC-4.4: updateSession follows exact WATCH -> get -> MULTI -> SET -> EXEC sequence")
    void shouldExecuteAtomicWatchMultiExecSequenceOnUpdate() throws Exception {
        UUID sid = UUID.randomUUID();
        String key = "advisor:session:" + sid;
        RoomAdvisorSession existing = RoomAdvisorSession.builder()
                .sessionId(sid)
                .sessionVersion(1L)
                .ownerKey("owner:user-1")
                .lastAccessedAt(Instant.now())
                .build();
        String existingJson = objectMapper.writeValueAsString(existing);

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(existingJson);
            when(redisTemplate.exec()).thenReturn(List.of("OK"));
            return callback.execute(redisTemplate);
        });

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Đà Nẵng")
                .checkInDate(LocalDate.of(2026, 12, 1))
                .checkOutDate(LocalDate.of(2026, 12, 5))
                .adults(2)
                .build();

        CriteriaEvidenceDto ev = CriteriaEvidenceDto.builder()
                .field("locationKeyword")
                .origin(CriteriaOrigin.USER_PROVIDED)
                .confirmation(CriteriaConfirmation.CONFIRMED)
                .build();

        RoomAdvisorSession updated = store.updateSession(
                sid,
                "owner:user-1",
                1L,
                criteria,
                List.of(ev),
                AdvisorResultDto.builder().reply("Recommendation ready").build()
        );

        assertThat(updated.getSessionVersion()).isEqualTo(2L);
        assertThat(updated.getCriteria().getLocationKeyword()).isEqualTo("Đà Nẵng");

        InOrder inOrder = inOrder(redisTemplate, valueOperations);
        inOrder.verify(redisTemplate).watch(key);
        inOrder.verify(valueOperations).get(key);
        inOrder.verify(redisTemplate).multi();
        inOrder.verify(valueOperations).set(eq(key), anyString(), eq(Duration.ofSeconds(1800)));
        inOrder.verify(redisTemplate).exec();
    }

    @Test
    @DisplayName("AC-4.4: Version mismatch calls unwatch() and throws SessionVersionConflictException without multi/exec")
    void shouldRejectVersionMismatchWithConflictException() throws Exception {
        UUID sid = UUID.randomUUID();
        String key = "advisor:session:" + sid;
        RoomAdvisorSession existing = RoomAdvisorSession.builder()
                .sessionId(sid)
                .sessionVersion(2L)
                .ownerKey("owner:user-1")
                .lastAccessedAt(Instant.now())
                .build();
        String existingJson = objectMapper.writeValueAsString(existing);

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(existingJson);
            return callback.execute(redisTemplate);
        });

        assertThatThrownBy(() -> store.updateSession(
                sid,
                "owner:user-1",
                1L, // expected 1L, but in redis it is 2L
                null,
                null,
                null
        )).isInstanceOf(SessionVersionConflictException.class)
                .hasMessageContaining("Session version conflict: expected 1 but was 2");

        verify(redisTemplate).watch(key);
        verify(redisTemplate).unwatch();
        verify(redisTemplate, never()).multi();
        verify(redisTemplate, never()).exec();
    }

    @Test
    @DisplayName("AC-4.4: Owner mismatch calls unwatch() and throws SessionSecurityException without multi/exec")
    void shouldRejectOwnerMismatchWithSecurityException() throws Exception {
        UUID sid = UUID.randomUUID();
        String key = "advisor:session:" + sid;
        RoomAdvisorSession existing = RoomAdvisorSession.builder()
                .sessionId(sid)
                .sessionVersion(1L)
                .ownerKey("owner:legitimate-user")
                .lastAccessedAt(Instant.now())
                .build();
        String existingJson = objectMapper.writeValueAsString(existing);

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(existingJson);
            return callback.execute(redisTemplate);
        });

        assertThatThrownBy(() -> store.updateSession(
                sid,
                "owner:attacker-user",
                1L,
                null,
                null,
                null
        )).isInstanceOf(SessionSecurityException.class)
                .hasMessageContaining("owner mismatch");

        verify(redisTemplate).watch(key);
        verify(redisTemplate).unwatch();
        verify(redisTemplate, never()).multi();
        verify(redisTemplate, never()).exec();
    }

    @Test
    @DisplayName("AC-4.4: Session missing on update calls unwatch() and throws SessionNotFoundException")
    void shouldRejectMissingSessionOnUpdate() {
        UUID sid = UUID.randomUUID();
        String key = "advisor:session:" + sid;

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(null);
            return callback.execute(redisTemplate);
        });

        assertThatThrownBy(() -> store.updateSession(
                sid,
                "owner:user-1",
                1L,
                null,
                null,
                null
        )).isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Session not found or expired");

        verify(redisTemplate).watch(key);
        verify(redisTemplate).unwatch();
        verify(redisTemplate, never()).multi();
        verify(redisTemplate, never()).exec();
    }

    @Test
    @DisplayName("AC-4.4: Bounded retry: WATCH abort retries and succeeds on subsequent attempt")
    void shouldRetryOnWatchAbortAndSucceed() throws Exception {
        UUID sid = UUID.randomUUID();
        String key = "advisor:session:" + sid;
        RoomAdvisorSession existing = RoomAdvisorSession.builder()
                .sessionId(sid)
                .sessionVersion(1L)
                .ownerKey("owner:user-1")
                .lastAccessedAt(Instant.now())
                .build();
        String existingJson = objectMapper.writeValueAsString(existing);

        AtomicInteger attemptCounter = new AtomicInteger(0);

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(existingJson);

            int current = attemptCounter.incrementAndGet();
            if (current == 1) {
                // First attempt aborts (exec returns null / empty)
                when(redisTemplate.exec()).thenReturn(Collections.emptyList());
            } else {
                // Second attempt succeeds
                when(redisTemplate.exec()).thenReturn(List.of("OK"));
            }
            return callback.execute(redisTemplate);
        });

        RoomAdvisorSession updated = store.updateSession(
                sid,
                "owner:user-1",
                1L,
                null,
                null,
                null
        );

        assertThat(updated.getSessionVersion()).isEqualTo(2L);
        assertThat(attemptCounter.get()).isEqualTo(2);
        verify(redisTemplate, times(2)).watch(key);
        verify(redisTemplate, times(2)).multi();
    }

    @Test
    @DisplayName("AC-4.4: Bounded retry: Exhausted retries throw SessionVersionConflictException")
    void shouldThrowConflictWhenAllRetriesExhausted() throws Exception {
        UUID sid = UUID.randomUUID();
        String key = "advisor:session:" + sid;
        RoomAdvisorSession existing = RoomAdvisorSession.builder()
                .sessionId(sid)
                .sessionVersion(1L)
                .ownerKey("owner:user-1")
                .lastAccessedAt(Instant.now())
                .build();
        String existingJson = objectMapper.writeValueAsString(existing);

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(existingJson);
            when(redisTemplate.exec()).thenReturn(Collections.emptyList());
            return callback.execute(redisTemplate);
        });

        assertThatThrownBy(() -> store.updateSession(
                sid,
                "owner:user-1",
                1L,
                null,
                null,
                null
        )).isInstanceOf(SessionVersionConflictException.class)
                .hasMessageContaining("maximum transaction retries exceeded");

        verify(redisTemplate, times(RedisRoomAdvisorSessionStore.MAX_UPDATE_RETRIES)).watch(key);
    }

    @Test
    @DisplayName("AC-4.5: Redis outage throws SessionStorageUnavailableException (mapped to 503)")
    void shouldThrowSessionStorageUnavailableExceptionOnRedisError() {
        UUID sid = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new QueryTimeoutException("Redis connection timed out"));

        assertThatThrownBy(() -> store.getOrCreateSession(sid, "owner:user-1"))
                .isInstanceOf(SessionStorageUnavailableException.class)
                .hasMessageContaining("Redis storage unavailable");

        assertThatThrownBy(() -> store.getSession(sid, "owner:user-1"))
                .isInstanceOf(SessionStorageUnavailableException.class)
                .hasMessageContaining("Redis storage unavailable");
    }

    @Test
    @DisplayName("getSession: returns empty when key absent and refreshes TTL when present")
    void shouldRetrieveSessionAndRefreshTtl() throws Exception {
        UUID sid = UUID.randomUUID();
        String key = "advisor:session:" + sid;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Case 1: absent
        when(valueOperations.get(key)).thenReturn(null);
        Optional<RoomAdvisorSession> absent = store.getSession(sid, "owner:user-1");
        assertThat(absent).isEmpty();

        // Case 2: present
        RoomAdvisorSession session = RoomAdvisorSession.builder()
                .sessionId(sid)
                .sessionVersion(1L)
                .ownerKey("owner:user-1")
                .lastAccessedAt(Instant.now())
                .build();
        when(valueOperations.get(key)).thenReturn(objectMapper.writeValueAsString(session));

        Optional<RoomAdvisorSession> present = store.getSession(sid, "owner:user-1");
        assertThat(present).isPresent();
        assertThat(present.get().getSessionId()).isEqualTo(sid);

        // Verify TTL refreshed on touch
        verify(redisTemplate).expire(key, Duration.ofSeconds(1800));
    }

    @Test
    @DisplayName("invalidate: deletes session key from Redis")
    void shouldDeleteSessionKeyOnInvalidate() {
        UUID sid = UUID.randomUUID();
        String key = "advisor:session:" + sid;

        store.invalidate(sid);

        verify(redisTemplate).delete(key);
    }
}
