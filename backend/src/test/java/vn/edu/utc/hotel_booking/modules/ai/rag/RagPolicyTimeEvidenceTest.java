package vn.edu.utc.hotel_booking.modules.ai.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.modules.ai.rag.corpus.DefaultPublicHotelCorpusInitializer;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.HybridRagEmbeddingService;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.InMemoryScopedVectorStore;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class RagPolicyTimeEvidenceTest {
    private RagGroundedAnswerService answer;

    @BeforeEach void seedOriginalDemoCorpus() {
        var governance = new RagCorpusGovernanceValidator();
        var embedding = new HybridRagEmbeddingService();
        var store = new InMemoryScopedVectorStore();
        var lifecycle = new RagKnowledgeLifecycleService(governance, embedding, store);
        new DefaultPublicHotelCorpusInitializer(lifecycle, store, true).seedDefaultCorpus();
        answer = new RagGroundedAnswerService(new ScopedRagRetrievalService(store, embedding), governance);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Khách sạn 1 mấy giờ nhận phòng?", "Mấy giờ trả phòng?", "What time is check-in?"})
    void breakfastOrCancellationCannotAnswerMissingCheckInOrOutTime(String question) {
        var result = answer.answerQuestion(query(2, question));
        assertThat(result.getStatus()).isEqualTo(RagAnswerStatus.INSUFFICIENT_EVIDENCE);
        assertThat(result.getCitations()).isEmpty();
    }

    @Test void ownHotelCheckInTimeStillAnswersWithOwnCitation() {
        var result = answer.answerQuestion(query(1, "Mấy giờ nhận phòng?"));
        assertThat(result.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(result.getAnswer()).contains("14:00");
        assertThat(result.getCitations()).isNotEmpty().allSatisfy(c -> assertThat(c.getHotelId()).isEqualTo(1));
    }

    @Test void ownHotelBreakfastQuestionStillUsesBreakfastEvidence() {
        var result = answer.answerQuestion(query(2, "Bữa sáng phục vụ mấy giờ?"));
        assertThat(result.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(result.getAnswer()).contains("06:00");
        assertThat(result.getCitations()).isNotEmpty().allSatisfy(c -> assertThat(c.getHotelId()).isEqualTo(2));
    }

    private RagQueryRequestDto query(int hotel, String question) {
        return RagQueryRequestDto.builder().hotelId(hotel).question(question).topK(5)
                .asOfDate(LocalDate.of(2026, 10, 31)).build();
    }
}
