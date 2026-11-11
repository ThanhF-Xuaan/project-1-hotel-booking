package vn.edu.utc.hotel_booking.modules.ai.advisor.enums;

/**
 * Execution state of the RAG knowledge lookup within a unified conversational turn.
 */
public enum KnowledgeState {
    ANSWERED,
    SCOPE_REQUIRED,
    RATE_LIMITED,
    UNAVAILABLE,
    NOT_ASKED
}
