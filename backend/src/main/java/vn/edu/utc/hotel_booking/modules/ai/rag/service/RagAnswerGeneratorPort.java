package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import java.util.List;
import java.util.UUID;

/** No session, business services, URLs or model-controlled source metadata cross this boundary. */
public interface RagAnswerGeneratorPort {
    int MAX_CHUNKS = 5;
    int MAX_EVIDENCE_CHARACTERS = 10_000;

    record Evidence(UUID chunkId, String content) { }

    record Request(String question, String language, List<Evidence> evidence) {
        public Request { evidence = List.copyOf(evidence); }
    }

    record RawGeneration(String json, String finishReason) { }

    RawGeneration generate(Request request);

    default boolean isEnabled() { return true; }

    class GenerationUnavailableException extends RuntimeException {
        public GenerationUnavailableException() { super("RAG answer generation temporarily unavailable"); }
    }
}
