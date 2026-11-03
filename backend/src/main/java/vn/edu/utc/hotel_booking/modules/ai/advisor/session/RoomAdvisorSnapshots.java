package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

/** Detached snapshots at session boundaries; never expose mutable objects held by a store. */
public final class RoomAdvisorSnapshots {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private RoomAdvisorSnapshots() { }
    public static RoomAdvisorSession copy(RoomAdvisorSession value) { return copy(value, RoomAdvisorSession.class); }
    public static RoomSearchCriteriaDto copy(RoomSearchCriteriaDto value) { return copy(value, RoomSearchCriteriaDto.class); }
    private static <T> T copy(T value, Class<T> type) {
        if (value == null) return null;
        try { return MAPPER.convertValue(value, type); }
        catch (IllegalArgumentException failure) { throw new SessionStorageUnavailableException("Session snapshot unavailable"); }
    }
    public static RoomAdvisorSession committed(RoomAdvisorSession value, boolean newResult) {
        var snapshot = copy(value);
        if (newResult && snapshot.getLastResult() != null) {
            snapshot.getLastResult().setSessionId(snapshot.getSessionId());
            snapshot.getLastResult().setSessionVersion(snapshot.getSessionVersion());
        }
        return snapshot;
    }
}
