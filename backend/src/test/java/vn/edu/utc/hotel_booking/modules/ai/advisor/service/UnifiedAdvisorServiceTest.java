package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.KnowledgeState;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.UnifiedIntent;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSession;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionNotFoundException;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagPublicQueryFacade;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagPublicQueryOutcome;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedAdvisorServiceTest {

    private PolicyIntentClassifier intentClassifier;

    @Mock
    private RoomAdvisorOrchestrationService roomAdvisorService;

    @Mock
    private RagPublicQueryFacade ragFacade;

    @Mock
    private RoomAdvisorSessionStore sessionStore;

    private UnifiedAdvisorService unifiedService;

    @BeforeEach
    void setUp() {
        intentClassifier = new PolicyIntentClassifier();
        unifiedService = new UnifiedAdvisorService(intentClassifier, roomAdvisorService, ragFacade, sessionStore);
    }

    @Test
    @DisplayName("Search-only message delegates to roomAdvisorService and leaves knowledge null")
    void searchOnlyDelegatesToRoomAdvisor() {
        AdvisorResultDto mockResult = AdvisorResultDto.builder()
                .status(AdvisorStatus.RESULTS)
                .reply("Đã tìm thấy phòng.")
                .build();
        when(roomAdvisorService.chat(any(), eq("owner-1"))).thenReturn(mockResult);

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng tại Đà Nẵng cuối tuần")
                .build();

        UnifiedAdvisorResultDto result = unifiedService.converse(request, "owner-1");

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_ONLY);
        assertThat(result.getAdvisor()).isNotNull();
        assertThat(result.getKnowledge()).isNull();
        verify(ragFacade, never()).ask(any(), any());
    }

    @Test
    @DisplayName("Combined search and policy with single hotel resolves scope and returns both parts")
    void combinedSearchAndPolicyWithSingleHotel() {
        RoomRecommendationDto card = RoomRecommendationDto.builder()
                .room(RoomSummaryDto.builder().hotelId(2).hotelName("Đà Nẵng Resort").build())
                .build();
        AdvisorResultDto mockAdvisor = AdvisorResultDto.builder()
                .status(AdvisorStatus.RESULTS)
                .recommendations(List.of(card))
                .build();
        when(roomAdvisorService.chat(any(), eq("owner-1"))).thenReturn(mockAdvisor);

        RagQueryResultDto ragResult = RagQueryResultDto.builder()
                .status(RagAnswerStatus.GROUNDED_ANSWER)
                .answer("Trẻ dưới 6 tuổi được miễn phí buffet sáng.")
                .build();
        when(ragFacade.ask(anyString(), eq(2))).thenReturn(RagPublicQueryOutcome.answer(ragResult));

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng đôi ở Đà Nẵng cuối tuần này cho 2 người lớn và 1 bé 4 tuổi, bé có được miễn phí buffet sáng không?")
                .build();

        UnifiedAdvisorResultDto result = unifiedService.converse(request, "owner-1");

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_AND_POLICY);
        assertThat(result.getAdvisor()).isNotNull();
        assertThat(result.getKnowledge()).isNotNull();
        assertThat(result.getKnowledge().getState()).isEqualTo(KnowledgeState.ANSWERED);
        assertThat(result.getKnowledge().getHotelId()).isEqualTo(2);
        assertThat(result.getKnowledge().getResult().getAnswer()).contains("miễn phí");
    }

    @Test
    @DisplayName("Combined search and policy with multiple distinct hotels fails-closed to SCOPE_REQUIRED with choices")
    void combinedSearchWithMultipleHotelsFailsClosedWithChoices() {
        RoomRecommendationDto card1 = RoomRecommendationDto.builder()
                .room(RoomSummaryDto.builder().hotelId(1).hotelName("Hà Nội Central").build())
                .build();
        RoomRecommendationDto card2 = RoomRecommendationDto.builder()
                .room(RoomSummaryDto.builder().hotelId(2).hotelName("Đà Nẵng Resort").build())
                .build();
        AdvisorResultDto mockAdvisor = AdvisorResultDto.builder()
                .status(AdvisorStatus.RESULTS)
                .recommendations(List.of(card1, card2))
                .build();
        when(roomAdvisorService.chat(any(), eq("owner-1"))).thenReturn(mockAdvisor);

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng đôi và hồ bơi mở cửa lúc mấy giờ?")
                .build();

        UnifiedAdvisorResultDto result = unifiedService.converse(request, "owner-1");

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.SEARCH_AND_POLICY);
        assertThat(result.getKnowledge()).isNotNull();
        assertThat(result.getKnowledge().getState()).isEqualTo(KnowledgeState.SCOPE_REQUIRED);
        assertThat(result.getKnowledge().getHotelChoices()).hasSize(2);
        verify(ragFacade, never()).ask(any(), any());
    }

    @Test
    @DisplayName("Explicit policyHotelId parameter overrides card resolution")
    void explicitPolicyHotelIdOverridesCards() {
        RoomRecommendationDto card1 = RoomRecommendationDto.builder()
                .room(RoomSummaryDto.builder().hotelId(1).hotelName("Hà Nội Central").build())
                .build();
        RoomRecommendationDto card2 = RoomRecommendationDto.builder()
                .room(RoomSummaryDto.builder().hotelId(2).hotelName("Đà Nẵng Resort").build())
                .build();
        AdvisorResultDto mockAdvisor = AdvisorResultDto.builder()
                .status(AdvisorStatus.RESULTS)
                .recommendations(List.of(card1, card2))
                .build();
        when(roomAdvisorService.chat(any(), eq("owner-1"))).thenReturn(mockAdvisor);

        RagQueryResultDto ragResult = RagQueryResultDto.builder()
                .status(RagAnswerStatus.GROUNDED_ANSWER)
                .answer("Hồ bơi mở cửa 06:00 - 21:00.")
                .build();
        when(ragFacade.ask(anyString(), eq(2))).thenReturn(RagPublicQueryOutcome.answer(ragResult));

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng đôi và hồ bơi mở cửa lúc mấy giờ?")
                .policyHotelId(2)
                .build();

        UnifiedAdvisorResultDto result = unifiedService.converse(request, "owner-1");

        assertThat(result.getKnowledge()).isNotNull();
        assertThat(result.getKnowledge().getState()).isEqualTo(KnowledgeState.ANSWERED);
        assertThat(result.getKnowledge().getHotelId()).isEqualTo(2);
    }

    @Test
    @DisplayName("Policy-only with missing sessionId throws SessionNotFoundException")
    void policyOnlyWithMissingSessionThrowsException() {
        UUID unknown = UUID.randomUUID();
        when(sessionStore.getSession(eq(unknown), eq("owner-1"))).thenReturn(Optional.empty());

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Chính sách nhận phòng từ mấy giờ?")
                .sessionId(unknown)
                .build();

        assertThatThrownBy(() -> unifiedService.converse(request, "owner-1"))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("Policy-only without sessionId answers safely without creating session")
    void policyOnlyWithoutSessionAnswersSafely() {
        RagQueryResultDto ragResult = RagQueryResultDto.builder()
                .status(RagAnswerStatus.GROUNDED_ANSWER)
                .answer("Hủy phòng trước 48 giờ miễn phí.")
                .build();
        when(ragFacade.ask(anyString(), isNull())).thenReturn(RagPublicQueryOutcome.answer(ragResult));

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Chính sách hủy phòng và hoàn tiền của khách sạn như thế nào?")
                .build();

        UnifiedAdvisorResultDto result = unifiedService.converse(request, "owner-1");

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.POLICY_ONLY);
        assertThat(result.getAdvisor()).isNull();
        assertThat(result.getKnowledge()).isNotNull();
        assertThat(result.getKnowledge().getState()).isEqualTo(KnowledgeState.ANSWERED);
    }

    @Test
    @DisplayName("Independent degradation: RAG failure does not suppress room search results")
    void ragFailurePreservesAdvisorCards() {
        RoomRecommendationDto card = RoomRecommendationDto.builder()
                .room(RoomSummaryDto.builder().hotelId(2).hotelName("Đà Nẵng Resort").build())
                .build();
        AdvisorResultDto mockAdvisor = AdvisorResultDto.builder()
                .status(AdvisorStatus.RESULTS)
                .recommendations(List.of(card))
                .build();
        when(roomAdvisorService.chat(any(), eq("owner-1"))).thenReturn(mockAdvisor);
        when(ragFacade.ask(anyString(), eq(2))).thenReturn(RagPublicQueryOutcome.unavailable());

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng Đà Nẵng và ăn sáng mấy giờ?")
                .build();

        UnifiedAdvisorResultDto result = unifiedService.converse(request, "owner-1");

        assertThat(result.getAdvisor()).isNotNull();
        assertThat(result.getAdvisor().getRecommendations()).hasSize(1);
        assertThat(result.getKnowledge()).isNotNull();
        assertThat(result.getKnowledge().getState()).isEqualTo(KnowledgeState.UNAVAILABLE);
    }

    @Test
    @DisplayName("Write intent command returns OUT_OF_SCOPE and delegates to orchestrationService")
    void writeIntentDelegatesOutOfScope() {
        AdvisorResultDto outOfScopeResult = AdvisorResultDto.builder()
                .status(AdvisorStatus.OUT_OF_SCOPE)
                .reply("Tôi là trợ lý tìm phòng...")
                .build();
        when(roomAdvisorService.chat(any(), eq("owner-1"))).thenReturn(outOfScopeResult);

        UnifiedAdvisorRequestDto request = UnifiedAdvisorRequestDto.builder()
                .message("Hãy đặt phòng này cho tôi ngay")
                .build();

        UnifiedAdvisorResultDto result = unifiedService.converse(request, "owner-1");

        assertThat(result.getIntent()).isEqualTo(UnifiedIntent.OUT_OF_SCOPE);
        assertThat(result.getKnowledge()).isNull();
    }
}
