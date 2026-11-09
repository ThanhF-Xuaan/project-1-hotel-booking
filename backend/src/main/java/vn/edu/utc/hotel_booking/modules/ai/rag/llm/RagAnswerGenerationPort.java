package vn.edu.utc.hotel_booking.modules.ai.rag.llm;

import java.util.List;
import java.util.Optional;

/** Bounded LLM boundary: evidence comes from the scoped RAG retrieval service. */
public interface RagAnswerGenerationPort {

    Optional<GeneratedAnswer> generate(String question, String language, List<Evidence> evidence);

    record Evidence(int id, String content) { }

    record GeneratedAnswer(String answer, List<Integer> evidenceIds) { }

    static RagAnswerGenerationPort disabled() {
        return (question, language, evidence) -> Optional.empty();
    }
}
