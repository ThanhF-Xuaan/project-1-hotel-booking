package vn.edu.utc.hotel_booking.modules.ai.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.*;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RagKnowledgeReviewRegressionTest {
    @ParameterizedTest
    @ValueSource(strings={
        "Giá phòng Deluxe?", "Giá phòng Deluxe hôm nay?", "Giá phòng?",
        "Gia phong Deluxe?", "Gia phong Deluxe hom nay?", "Giá Suite?",
        "Phong Deluxe co trong ngay mai khong?",
        "Phòng Deluxe co trong ngay mai khong?", "Phong Deluxe có trống ngày mai không?",
        "Phòng Deluxe co trống ngay mai khong?", "Phòng này có trống không?",
        "Phòng trống?", "Phong co trong?",
        "Phòng Deluxe hướng biển có trống ngày mai không?",
        "Phòng Deluxe có trống vào ngày mai không?",
        "Phòng Deluxe hướng biển có trống vào ngày mai không?",
        "Phòng Deluxe Hướng Biển Giường Đôi có trống vào ngày mai không?",
        "Phong Deluxe huong bien co trong vao ngay mai khong?",
        "Phòng Deluxe huong bien co trong vao ngay mai khong?",
        "Phòng Suite hướng vườn có trống tối nay không?",
        "Phòng Deluxe hướng biển giường đôi có còn trống ngày mai không?",
        "Phòng Deluxe có trống vào cuối tuần không?",
        "Phòng Deluxe có trống vào ngày 08/11 không?",
        "Đêm mai Deluxe giá bao nhiêu?", "Còn 2 phòng tối nay không?",
        "Ngày mai còn phòng không?", "Còn bao nhiêu phòng?",
        "Phòng Deluxe có trống ngày mai không?", "Tình trạng phòng hôm nay thế nào?",
        "Giá hạng Deluxe là bao nhiêu cho 2 đêm?", "Phòng Suite bao nhiêu tiền?",
        "Báo giá phòng cho tôi", "Giá phòng hiện tại là bao nhiêu?",
        "DEM MAI DELUXE GIA BAO NHIEU?", "Con 2 phong toi nay khong?",
        "Còn\u00a02\u00a0phòng tối nay không?", "Cò\u200bn 2 phòng tối nay không?",
        "How much is a Deluxe room tonight?", "How much does a room cost?",
        "What is the room price?", "What are the current room rates?",
        "Are there rooms available tomorrow?", "Is a Deluxe room available?",
        "What is the room availability tonight?", "Room rates for tomorrow?"
    })
    void defersBeforeRetrievalOrGeneration(String question) {
        var retrieval=mock(ScopedRagRetrievalService.class);
        var generator=mock(RagAnswerGeneratorPort.class);
        var service=new RagGroundedAnswerService(retrieval,new RagCorpusGovernanceValidator(),generator);
        for (String q:List.of(question,Normalizer.normalize(question,Normalizer.Form.NFD))) {
            var response=service.answerQuestion(RagQueryRequestDto.builder().hotelId(4).question(q).build());
            assertThat(response.getStatus()).isEqualTo(RagAnswerStatus.OUT_OF_SCOPE);
            assertThat(response.getAnswer()).contains("Core Booking");
            assertThat(response.getCitations()).isEmpty();
        }
        verifyNoInteractions(retrieval,generator);
    }

    @ParameterizedTest
    @ValueSource(strings={
        "Deluxe Hướng Biển Giường Đôi rộng bao nhiêu?", "Phòng Deluxe có diện tích bao nhiêu mét vuông?",
        "Phòng có gì bên trong?", "Phòng có bàn làm việc trong phòng không?",
        "Phong co gi ben trong?", "Phong co ban lam viec trong phong khong?",
        "Trong phòng có gì?", "Phòng trong có gì?",
        "Phòng có chỗ trống để đặt nôi không?", "Phòng có khoảng trống để đặt nôi không?",
        "Phong co cho trong de dat noi khong?", "Phòng co cho trong de dat noi khong?",
        "Phòng Deluxe hướng biển có chỗ trống để đặt nôi không?",
        "Phòng Deluxe Hướng Biển Giường Đôi có khoảng trống để đặt nôi không?",
        "Phòng Deluxe hướng biển có gì bên trong?",
        "Phòng Suite hướng vườn có bàn làm việc trong phòng không?",
        "Phòng có ban công không?", "Khách sạn nằm trên đảo nào?",
        "Buffet sáng bắt đầu mấy giờ?", "Chính sách hủy phòng là gì?",
        "Giá phòng đã bao gồm bữa sáng chưa?", "Giờ nhận phòng là mấy giờ?",
        "Does the Deluxe room have a balcony?", "How much space does the Deluxe room have?",
        "What does the room rate include?", "What time does the pool close?"
    })
    void keepsStaticKnowledgeOnRetrievalPath(String question) {
        var retrieval=mock(ScopedRagRetrievalService.class);
        var generator=mock(RagAnswerGeneratorPort.class);
        when(retrieval.retrieve(any())).thenReturn(List.of());
        var service=new RagGroundedAnswerService(retrieval,new RagCorpusGovernanceValidator(),generator);
        var request=RagQueryRequestDto.builder().hotelId(4).question(question).build();
        var response=service.answerQuestion(request);
        assertThat(response.getStatus()).isEqualTo(RagAnswerStatus.INSUFFICIENT_EVIDENCE);
        verify(retrieval).retrieve(request);
        verifyNoInteractions(generator);
    }

    @ParameterizedTest
    @ValueSource(strings={
        "Vinpearl Resort & Spa Hạ Long có cho mang thú cưng không?",
        "Khách sạn có nhận vật nuôi không?", "Có được mang chó hoặc mèo không?",
        "Are pets allowed at the hotel?", "Can I bring my dog?", "Are cats allowed?",
        "Khach san co cho mang thu cung khong?"
    })
    void unrelatedEvidenceDoesNotAnswerPetPolicy(String question) {
        var retrieval=mock(ScopedRagRetrievalService.class);
        var generator=mock(RagAnswerGeneratorPort.class);
        when(retrieval.retrieve(any())).thenReturn(List.of(
                evidence("DINING", "Nhà hàng phục vụ món Á và Âu, tại sảnh khách sạn."),
                evidence("WEDDING", "Khách sạn có không gian tổ chức tiệc cưới tại sảnh và bãi biển."),
                evidence("MEETING", "Khách sạn có phòng hội họp, sự kiện và khu làm việc bên trong.")));
        var service=new RagGroundedAnswerService(retrieval,new RagCorpusGovernanceValidator(),generator);
        var response=service.answerQuestion(RagQueryRequestDto.builder().hotelId(4).question(question).build());
        assertThat(response.getStatus()).isEqualTo(RagAnswerStatus.INSUFFICIENT_EVIDENCE);
        assertThat(response.getCitations()).isEmpty();
        assertThat(response.getAnswer()).doesNotContain("món Á", "tiệc cưới", "hội họp");
        verifyNoInteractions(generator);
    }

    @ParameterizedTest
    @ValueSource(strings={
        "Không cho phép mang thú cưng vào khách sạn.",
        "Cho phép mang vật nuôi theo chính sách của cơ sở.",
        "Pets are not allowed at this hotel.", "Dogs and cats are allowed in designated areas.",
        "Khong cho phep mang thu cung vao khach san."
    })
    void petEvidenceRemainsAnswerableAndUnrelatedChunksAreExcluded(String policy) {
        var retrieval=mock(ScopedRagRetrievalService.class);
        when(retrieval.retrieve(any())).thenReturn(List.of(
                evidence("PETS",policy), evidence("DINING","Nhà hàng phục vụ món Á và Âu.")));
        var service=new RagGroundedAnswerService(retrieval,new RagCorpusGovernanceValidator());
        var response=service.answerQuestion(RagQueryRequestDto.builder().hotelId(4)
                .question("Khách sạn có cho mang thú cưng không?").build());
        assertThat(response.getStatus()).isEqualTo(RagAnswerStatus.GROUNDED_ANSWER);
        assertThat(response.getAnswer()).isEqualTo(policy);
        assertThat(response.getCitations()).extracting(c->c.getSourceId()).containsExactly("PETS");
    }

    @Test
    void petEvidenceFromForeignHotelIsNotUsable() {
        var retrieval=mock(ScopedRagRetrievalService.class);
        var foreign=evidence("FOREIGN-PETS","Pets are allowed.");
        foreign.getChunk().setHotelId(1);
        when(retrieval.retrieve(any())).thenReturn(List.of(foreign));
        var service=new RagGroundedAnswerService(retrieval,new RagCorpusGovernanceValidator());
        var response=service.answerQuestion(RagQueryRequestDto.builder().hotelId(4)
                .question("Are pets allowed?").build());
        assertThat(response.getStatus()).isEqualTo(RagAnswerStatus.INSUFFICIENT_EVIDENCE);
        assertThat(response.getCitations()).isEmpty();
    }

    private ScopedRagRetrievalService.ScoredChunk evidence(String source,String content) {
        return new ScopedRagRetrievalService.ScoredChunk(KnowledgeChunk.builder()
                .chunkId(UUID.randomUUID()).documentId(UUID.randomUUID()).sourceId(source).version(1)
                .scopeType(RagScopeType.HOTEL_SCOPED).hotelId(4).category(RagDocumentCategory.POLICY)
                .status(RagVersionStatus.ACTIVE).content(content).sanitizedContent(content).build(),0.9);
    }
}
