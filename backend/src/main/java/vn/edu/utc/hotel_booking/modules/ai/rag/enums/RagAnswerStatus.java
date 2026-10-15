package vn.edu.utc.hotel_booking.modules.ai.rag.enums;

public enum RagAnswerStatus {
    GROUNDED_ANSWER,
    INSUFFICIENT_EVIDENCE,
    SCOPE_REQUIRED,
    CONFLICTING_SOURCES,
    RAG_TEMP_UNAVAILABLE,
    OUT_OF_SCOPE
}
