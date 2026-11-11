package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import lombok.Builder;
import lombok.Value;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;

@Value
@Builder
public class RagPublicQueryOutcome {
    public enum State { ANSWERED, SCOPE_REQUIRED, RATE_LIMITED, UNAVAILABLE }

    State state;
    RagQueryResultDto result;
    Long retryAfterSeconds;

    public static RagPublicQueryOutcome answer(RagQueryResultDto result) {
        return RagPublicQueryOutcome.builder()
                .state(State.ANSWERED)
                .result(result)
                .build();
    }

    public static RagPublicQueryOutcome scopeRequired(RagQueryResultDto result) {
        return RagPublicQueryOutcome.builder()
                .state(State.SCOPE_REQUIRED)
                .result(result)
                .build();
    }

    public static RagPublicQueryOutcome rateLimited(long retryAfterSeconds) {
        return RagPublicQueryOutcome.builder()
                .state(State.RATE_LIMITED)
                .retryAfterSeconds(retryAfterSeconds)
                .build();
    }

    public static RagPublicQueryOutcome unavailable() {
        return RagPublicQueryOutcome.builder()
                .state(State.UNAVAILABLE)
                .build();
    }
}
