package vn.edu.utc.hotel_booking.modules.ai.rag.llm;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.ScopedRagRetrievalService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagAiGroundedGenerationTest {

    @Test
    void modelAnswerKeepsOnlyCitationsDeclaredByTheModel() {
        ScopedRagRetrievalService retrieval = mock(ScopedRagRetrievalService.class);
        KnowledgeChunk checkIn = chunk("POLICY-CHECKIN", "Chính sách nhận phòng: nhận phòng từ 14:00.");
        KnowledgeChunk luggage = chunk("SERVICE-LUGGAGE", "Khách sạn hỗ trợ giữ hành lý tại quầy lễ tân.");
        when(retrieval.retrieve(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                new ScopedRagRetrievalService.ScoredChunk(checkIn, 0.91),
                new ScopedRagRetrievalService.ScoredChunk(luggage, 0.75)));
        RagAnswerGenerationPort generator = (question, language, evidence) -> Optional.of(
                new RagAnswerGenerationPort.GeneratedAnswer(
                        "Quý khách có thể nhận phòng từ 14:00.", List.of(1)));
        RagGroundedAnswerService service = new RagGroundedAnswerService(
                retrieval, new RagCorpusGovernanceValidator(), generator);

        var result = service.answerQuestion(RagQueryRequestDto.builder()
                .question("Mấy giờ nhận phòng?")
                .hotelId(4)
                .asOfDate(LocalDate.of(2026, 11, 8))
                .build());

        assertThat(result.getAnswer()).isEqualTo("Quý khách có thể nhận phòng từ 14:00.");
        assertThat(result.getCitations()).extracting(citation -> citation.getSourceId())
                .containsExactly("POLICY-CHECKIN");
        assertThat(result.getDisclaimers()).anyMatch(text -> text.contains("Gemini"));
    }

    private KnowledgeChunk chunk(String sourceId, String content) {
        return KnowledgeChunk.builder()
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .sourceId(sourceId)
                .version(1)
                .chunkIndex(0)
                .scopeType(RagScopeType.HOTEL_SCOPED)
                .hotelId(4)
                .category(RagDocumentCategory.POLICY)
                .content(content)
                .sanitizedContent(content)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .status(RagVersionStatus.ACTIVE)
                .build();
    }
}
