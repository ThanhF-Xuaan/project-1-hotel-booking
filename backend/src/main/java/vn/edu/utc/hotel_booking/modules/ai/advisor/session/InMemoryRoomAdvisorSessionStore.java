package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaEvidenceDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryRoomAdvisorSessionStore implements RoomAdvisorSessionStore {

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final Map<UUID, RoomAdvisorSession> store = new ConcurrentHashMap<>();

    @Override
    public RoomAdvisorSession getOrCreateSession(UUID sessionId, String ownerKey) {
        purgeExpired();

        UUID targetId = (sessionId != null) ? sessionId : UUID.randomUUID();
        AtomicReference<SessionSecurityException> securityEx = new AtomicReference<>();

        RoomAdvisorSession session = store.compute(targetId, (key, existing) -> {
            if (existing == null || isExpired(existing)) {
                return RoomAdvisorSession.builder()
                        .sessionId(targetId)
                        .sessionVersion(1L)
                        .ownerKey(ownerKey)
                        .lastAccessedAt(Instant.now())
                        .build();
            }

            if (existing.getOwnerKey() != null && ownerKey != null && !existing.getOwnerKey().equals(ownerKey)) {
                securityEx.set(new SessionSecurityException("Access denied: session owner mismatch"));
                return existing;
            }

            return RoomAdvisorSession.builder()
                    .sessionId(existing.getSessionId())
                    .sessionVersion(existing.getSessionVersion())
                    .ownerKey(existing.getOwnerKey())
                    .criteria(existing.getCriteria())
                    .criteriaProvenance(existing.getCriteriaProvenance() != null ? new ArrayList<>(existing.getCriteriaProvenance()) : new ArrayList<>())
                    .lastResult(existing.getLastResult())
                    .lastAccessedAt(Instant.now())
                    .build();
        });

        if (securityEx.get() != null) {
            throw securityEx.get();
        }

        return session;
    }

    @Override
    public Optional<RoomAdvisorSession> getSession(UUID sessionId, String ownerKey) {
        purgeExpired();

        if (sessionId == null) {
            return Optional.empty();
        }

        RoomAdvisorSession existing = store.get(sessionId);
        if (existing == null) {
            return Optional.empty();
        }

        if (isExpired(existing)) {
            store.remove(sessionId);
            return Optional.empty();
        }

        validateOwner(existing, ownerKey);
        return Optional.of(existing);
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
        purgeExpired();

        if (sessionId == null) {
            throw new SessionNotFoundException("Session not found or expired");
        }

        AtomicReference<SessionSecurityException> securityEx = new AtomicReference<>();
        AtomicReference<SessionVersionConflictException> conflictEx = new AtomicReference<>();

        RoomAdvisorSession updated = store.compute(sessionId, (key, existing) -> {
            if (existing == null || isExpired(existing)) {
                return null;
            }

            if (existing.getOwnerKey() != null && ownerKey != null && !existing.getOwnerKey().equals(ownerKey)) {
                securityEx.set(new SessionSecurityException("Access denied: session owner mismatch"));
                return existing;
            }

            if (expectedSessionVersion != null && !expectedSessionVersion.equals(existing.getSessionVersion())) {
                conflictEx.set(new SessionVersionConflictException(
                        "Session version conflict: expected " + expectedSessionVersion + " but was " + existing.getSessionVersion()
                ));
                return existing;
            }

            long nextVersion = existing.getSessionVersion() + 1;
            return RoomAdvisorSession.builder()
                    .sessionId(existing.getSessionId())
                    .sessionVersion(nextVersion)
                    .ownerKey(existing.getOwnerKey())
                    .criteria(criteria != null ? criteria : existing.getCriteria())
                    .criteriaProvenance(provenance != null ? new ArrayList<>(provenance) :
                            (existing.getCriteriaProvenance() != null ? new ArrayList<>(existing.getCriteriaProvenance()) : new ArrayList<>()))
                    .lastResult(lastResult != null ? lastResult : existing.getLastResult())
                    .lastAccessedAt(Instant.now())
                    .build();
        });

        if (securityEx.get() != null) {
            throw securityEx.get();
        }
        if (conflictEx.get() != null) {
            throw conflictEx.get();
        }
        if (updated == null) {
            throw new SessionNotFoundException("Session not found or expired");
        }

        return updated;
    }

    @Override
    public void invalidate(UUID sessionId) {
        if (sessionId != null) {
            store.remove(sessionId);
        }
    }

    private void validateOwner(RoomAdvisorSession session, String ownerKey) {
        if (session.getOwnerKey() != null && ownerKey != null) {
            if (!session.getOwnerKey().equals(ownerKey)) {
                throw new SessionSecurityException("Access denied: session owner mismatch");
            }
        }
    }

    private boolean isExpired(RoomAdvisorSession session) {
        if (session == null || session.getLastAccessedAt() == null) {
            return true;
        }
        return Instant.now().isAfter(session.getLastAccessedAt().plus(SESSION_TTL));
    }

    private void purgeExpired() {
        Instant threshold = Instant.now().minus(SESSION_TTL);
        store.entrySet().removeIf(entry -> entry.getValue().getLastAccessedAt() == null
                || entry.getValue().getLastAccessedAt().isBefore(threshold));
    }
}
