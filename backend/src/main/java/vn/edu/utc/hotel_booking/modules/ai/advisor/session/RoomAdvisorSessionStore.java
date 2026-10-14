package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaEvidenceDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomAdvisorSessionStore {

    /**
     * Retrieves an existing session or initializes a new one.
     * Verifies that the ownerKey matches the session owner.
     */
    RoomAdvisorSession getOrCreateSession(UUID sessionId, String ownerKey);

    /**
     * Retrieves an existing session by ID and ownerKey.
     */
    Optional<RoomAdvisorSession> getSession(UUID sessionId, String ownerKey);

    /**
     * Updates an existing session with new criteria, provenance, and latest recommendation result.
     * Checks optimistic locking against expectedSessionVersion if provided.
     */
    RoomAdvisorSession updateSession(
            UUID sessionId,
            String ownerKey,
            Long expectedSessionVersion,
            RoomSearchCriteriaDto criteria,
            List<CriteriaEvidenceDto> provenance,
            AdvisorResultDto lastResult
    );

    /**
     * Explicitly invalidates / removes a session.
     */
    void invalidate(UUID sessionId);
}
