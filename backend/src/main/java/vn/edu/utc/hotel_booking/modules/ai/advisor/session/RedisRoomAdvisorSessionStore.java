package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaEvidenceDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed session store for Room Advisor sessions (ADR-05, ADR-08).
 *
 * <p>Enforces optimistic concurrency via atomic {@code WATCH} -> read/validate -> {@code MULTI} ->
 * {@code SET} -> {@code EXEC} transactions with bounded retry on concurrent conflict. Redis storage
 * failures throw {@link SessionStorageUnavailableException} which maps to HTTP 503 at the controller
 * boundary.</p>
 *
 * <p>Constructor is inject-only and performs zero I/O so context bean wiring checks never trigger
 * network operations.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hotel.ai.advisor.session", name = "store-type", havingValue = "redis")
public class RedisRoomAdvisorSessionStore implements RoomAdvisorSessionStore {

    public static final String KEY_PREFIX = "advisor:session:";
    public static final Duration SESSION_TTL = Duration.ofSeconds(1800);
    public static final int MAX_UPDATE_RETRIES = 3;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public RedisRoomAdvisorSessionStore(
            StringRedisTemplate redisTemplate,
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
    }

    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Override
    public RoomAdvisorSession getOrCreateSession(UUID sessionId, String ownerKey) {
        try {
            UUID targetId = (sessionId != null) ? sessionId : UUID.randomUUID();
            String key = keyFor(targetId);

            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                RoomAdvisorSession newSession = RoomAdvisorSession.builder()
                        .sessionId(targetId)
                        .sessionVersion(1L)
                        .ownerKey(ownerKey)
                        .lastAccessedAt(Instant.now())
                        .build();
                redisTemplate.opsForValue().set(key, serialize(newSession), SESSION_TTL);
                return newSession;
            }

            RoomAdvisorSession existing = deserialize(raw);
            validateOwner(existing, ownerKey);

            RoomAdvisorSession refreshed = RoomAdvisorSession.builder()
                    .sessionId(existing.getSessionId())
                    .sessionVersion(existing.getSessionVersion())
                    .ownerKey(existing.getOwnerKey())
                    .criteria(existing.getCriteria())
                    .criteriaProvenance(existing.getCriteriaProvenance() != null
                            ? new ArrayList<>(existing.getCriteriaProvenance()) : new ArrayList<>())
                    .lastResult(existing.getLastResult())
                    .lastAccessedAt(Instant.now())
                    .build();

            redisTemplate.opsForValue().set(key, serialize(refreshed), SESSION_TTL);
            return refreshed;
        } catch (SessionSecurityException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new SessionStorageUnavailableException("Redis storage unavailable during getOrCreateSession", e);
        }
    }

    @Override
    public Optional<RoomAdvisorSession> getSession(UUID sessionId, String ownerKey) {
        if (sessionId == null) {
            return Optional.empty();
        }
        try {
            String key = keyFor(sessionId);
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                return Optional.empty();
            }

            RoomAdvisorSession existing = deserialize(raw);
            validateOwner(existing, ownerKey);

            // Refresh TTL on touch
            redisTemplate.expire(key, SESSION_TTL);
            return Optional.of(existing);
        } catch (SessionSecurityException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new SessionStorageUnavailableException("Redis storage unavailable during getSession", e);
        }
    }

    @Override
    public RoomAdvisorSession updateSession(
            UUID sessionId,
            String ownerKey,
            Long expectedSessionVersion,
            RoomSearchCriteriaDto criteria,
            List<CriteriaEvidenceDto> provenance,
            AdvisorResultDto lastResult
    ) {
        if (sessionId == null) {
            throw new SessionNotFoundException("Session not found or expired");
        }

        String key = keyFor(sessionId);

        for (int attempt = 1; attempt <= MAX_UPDATE_RETRIES; attempt++) {
            try {
                UpdateAttemptResult result = executeUpdateTransaction(
                        key, ownerKey, expectedSessionVersion, criteria, provenance, lastResult
                );

                if (result.success()) {
                    return result.session();
                }

                // If transaction aborted due to WATCH (concurrent modification), back off slightly and retry
                if (attempt < MAX_UPDATE_RETRIES) {
                    try {
                        Thread.sleep(10L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SessionVersionConflictException("Session version conflict: update interrupted");
                    }
                }
            } catch (SessionSecurityException | SessionNotFoundException | SessionVersionConflictException e) {
                throw e;
            } catch (DataAccessException e) {
                throw new SessionStorageUnavailableException("Redis storage unavailable during updateSession", e);
            }
        }

        throw new SessionVersionConflictException(
                "Session version conflict: maximum transaction retries exceeded for session " + sessionId
        );
    }

    @Override
    public void invalidate(UUID sessionId) {
        if (sessionId != null) {
            try {
                redisTemplate.delete(keyFor(sessionId));
            } catch (DataAccessException e) {
                throw new SessionStorageUnavailableException("Redis storage unavailable during invalidate", e);
            }
        }
    }

    private UpdateAttemptResult executeUpdateTransaction(
            String key,
            String ownerKey,
            Long expectedSessionVersion,
            RoomSearchCriteriaDto criteria,
            List<CriteriaEvidenceDto> provenance,
            AdvisorResultDto lastResult
    ) {
        return redisTemplate.execute(new SessionCallback<UpdateAttemptResult>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> UpdateAttemptResult execute(RedisOperations<K, V> operations) throws DataAccessException {
                StringRedisTemplate stringOps = (StringRedisTemplate) operations;

                stringOps.watch(key);

                String raw = stringOps.opsForValue().get(key);
                if (raw == null) {
                    stringOps.unwatch();
                    throw new SessionNotFoundException("Session not found or expired");
                }

                RoomAdvisorSession existing = deserialize(raw);

                if (existing.getOwnerKey() != null && ownerKey != null && !existing.getOwnerKey().equals(ownerKey)) {
                    stringOps.unwatch();
                    throw new SessionSecurityException("Access denied: session owner mismatch");
                }

                if (expectedSessionVersion != null && !expectedSessionVersion.equals(existing.getSessionVersion())) {
                    stringOps.unwatch();
                    throw new SessionVersionConflictException(
                            "Session version conflict: expected " + expectedSessionVersion
                                    + " but was " + existing.getSessionVersion()
                    );
                }

                long nextVersion = existing.getSessionVersion() + 1;
                RoomAdvisorSession updated = RoomAdvisorSession.builder()
                        .sessionId(existing.getSessionId())
                        .sessionVersion(nextVersion)
                        .ownerKey(existing.getOwnerKey())
                        .criteria(criteria != null ? criteria : existing.getCriteria())
                        .criteriaProvenance(provenance != null ? new ArrayList<>(provenance) :
                                (existing.getCriteriaProvenance() != null
                                        ? new ArrayList<>(existing.getCriteriaProvenance()) : new ArrayList<>()))
                        .lastResult(lastResult != null ? lastResult : existing.getLastResult())
                        .lastAccessedAt(Instant.now())
                        .build();

                stringOps.multi();
                stringOps.opsForValue().set(key, serialize(updated), SESSION_TTL);
                List<Object> execResult = stringOps.exec();

                if (execResult == null || execResult.isEmpty()) {
                    // WATCH abort: another writer modified the session key concurrently
                    return UpdateAttemptResult.aborted();
                }

                return UpdateAttemptResult.success(updated);
            }
        });
    }

    private void validateOwner(RoomAdvisorSession session, String ownerKey) {
        if (session.getOwnerKey() != null && ownerKey != null) {
            if (!session.getOwnerKey().equals(ownerKey)) {
                throw new SessionSecurityException("Access denied: session owner mismatch");
            }
        }
    }

    private String keyFor(UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return KEY_PREFIX + sessionId;
    }

    private String serialize(RoomAdvisorSession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new SessionStorageUnavailableException("Failed to serialize session", e);
        }
    }

    private RoomAdvisorSession deserialize(String json) {
        try {
            return objectMapper.readValue(json, RoomAdvisorSession.class);
        } catch (JsonProcessingException e) {
            throw new SessionStorageUnavailableException("Failed to deserialize session", e);
        }
    }

    private record UpdateAttemptResult(boolean success, RoomAdvisorSession session) {
        static UpdateAttemptResult success(RoomAdvisorSession session) {
            return new UpdateAttemptResult(true, session);
        }

        static UpdateAttemptResult aborted() {
            return new UpdateAttemptResult(false, null);
        }
    }
}
