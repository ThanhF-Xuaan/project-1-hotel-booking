package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaEvidenceDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRoomAdvisorSessionStore implements RoomAdvisorSessionStore {

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final Map<UUID, RoomAdvisorSession> store = new ConcurrentHashMap<>();

    @Override
    public RoomAdvisorSession getOrCreateSession(UUID sessionId, String ownerKey) {
        purgeExpired();

        if (sessionId == null) {
            UUID newId = UUID.randomUUID();
            RoomAdvisorSession newSession = RoomAdvisorSession.builder()
                    .sessionId(newId)
                    .sessionVersion(1L)
                    .ownerKey(ownerKey)
                    .lastAccessedAt(Instant.now())
                    .build();
            store.put(newId, newSession);
            return newSession;
        }

        RoomAdvisorSession existing = store.get(sessionId);
        if (existing == null || isExpired(existing)) {
            RoomAdvisorSession newSession = RoomAdvisorSession.builder()
                    .sessionId(sessionId)
                    .sessionVersion(1L)
                    .ownerKey(ownerKey)
                    .lastAccessedAt(Instant.now())
                    .build();
            store.put(sessionId, newSession);
            return newSession;
        }

        validateOwner(existing, ownerKey);
        existing.setLastAccessedAt(Instant.now());
        return existing;
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
        existing.setLastAccessedAt(Instant.now());
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

        RoomAdvisorSession session = getSession(sessionId, ownerKey)
                .orElseThrow(() -> new SessionSecurityException("Session not found or expired"));

        if (expectedSessionVersion != null && expectedSessionVersion != session.getSessionVersion()) {
            throw new SessionVersionConflictException(
                    "Session version conflict: expected " + expectedSessionVersion + " but was " + session.getSessionVersion()
            );
        }

        session.setSessionVersion(session.getSessionVersion() + 1);
        if (criteria != null) {
            session.setCriteria(criteria);
        }
        if (provenance != null) {
            session.setCriteriaProvenance(new ArrayList<>(provenance));
        }
        if (lastResult != null) {
            session.setLastResult(lastResult);
        }
        session.setLastAccessedAt(Instant.now());

        return session;
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
