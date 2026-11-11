package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class UnifiedAdvisorReviewRegressionTest {
    private final RoomAdvisorOrchestrationService rooms = mock(RoomAdvisorOrchestrationService.class);
    private final RagPublicQueryFacade rag = mock(RagPublicQueryFacade.class);
    private final InMemoryRoomAdvisorSessionStore store = new InMemoryRoomAdvisorSessionStore();
    private UnifiedAdvisorService service;

    @BeforeEach void setup() {
        service = new UnifiedAdvisorService(new PolicyIntentClassifier(), rooms, rag, store);
    }

    private void answerFor(int hotelId) {
        when(rag.ask(anyString(), eq(hotelId))).thenReturn(RagPublicQueryOutcome.answer(
                RagQueryResultDto.builder().status(RagAnswerStatus.INSUFFICIENT_EVIDENCE)
                        .answer("Synthetic public policy fallback").build()));
    }

    private RoomAdvisorSession searchSession(int... hotels) {
        var created = store.getOrCreateSession(null, "owner");
        var cards = java.util.Arrays.stream(hotels).mapToObj(id -> RoomRecommendationDto.builder()
                .room(RoomSummaryDto.builder().hotelId(id).hotelName("Synthetic H" + id).build()).build()).toList();
        return store.updateSession(created.getSessionId(), "owner", created.getSessionVersion(),
                RoomSearchCriteriaDto.builder().locationKeyword("Synthetic City").build(), List.of(),
                AdvisorResultDto.builder().status(AdvisorStatus.RESULTS).recommendations(cards).build());
    }

    @Test void policyFollowupUsesSingleHotelFromLastVerifiedCards() {
        var session = searchSession(2);
        answerFor(2);
        var result = service.converse(UnifiedAdvisorRequestDto.builder().message("Hồ bơi mở cửa mấy giờ?")
                .sessionId(session.getSessionId()).expectedSessionVersion(session.getSessionVersion()).build(), "owner");
        assertThat(result.getKnowledge().getHotelId()).isEqualTo(2);
        assertThat(result.getAdvisor()).isNull();
        verifyNoInteractions(rooms);
    }

    @Test void hotelChoiceIsRememberedAndClearedOnMaterialSearchChange() {
        var session = searchSession(1, 2);
        var initial = service.converse(UnifiedAdvisorRequestDto.builder().message("Hồ bơi mở cửa mấy giờ?")
                .sessionId(session.getSessionId()).build(), "owner");
        assertThat(initial.getKnowledge().getHotelChoices()).extracting(HotelChoiceDto::getHotelId).containsExactly(1, 2);
        verifyNoInteractions(rag);
        answerFor(2);
        var chosen = service.converse(UnifiedAdvisorRequestDto.builder().message("Hồ bơi mở cửa mấy giờ?")
                .sessionId(session.getSessionId()).expectedSessionVersion(session.getSessionVersion()).policyHotelId(2).build(), "owner");
        assertThat(chosen.getSessionVersion()).isEqualTo(session.getSessionVersion() + 1);
        var followup = service.converse(UnifiedAdvisorRequestDto.builder().message("Bữa sáng phục vụ mấy giờ?")
                .sessionId(chosen.getSessionId()).expectedSessionVersion(chosen.getSessionVersion()).build(), "owner");
        assertThat(followup.getKnowledge().getHotelId()).isEqualTo(2);
        var changed = store.updateSession(chosen.getSessionId(), "owner", chosen.getSessionVersion(),
                RoomSearchCriteriaDto.builder().locationKeyword("Other City").build(), List.of(),
                AdvisorResultDto.builder().status(AdvisorStatus.NEED_INFO).build());
        assertThat(changed.getPolicyHotelId()).isNull();
        when(rag.ask(anyString(), isNull())).thenReturn(RagPublicQueryOutcome.scopeRequired(
                RagQueryResultDto.builder().status(RagAnswerStatus.SCOPE_REQUIRED).build()));
        var afterChange = service.converse(UnifiedAdvisorRequestDto.builder().message("Hồ bơi mở cửa mấy giờ?")
                .sessionId(changed.getSessionId()).expectedSessionVersion(changed.getSessionVersion()).build(), "owner");
        assertThat(afterChange.getKnowledge().getHotelId()).isNull();
        assertThat(afterChange.getKnowledge().getState()).isEqualTo(KnowledgeState.SCOPE_REQUIRED);
    }

    @Test void standalonePolicySelectionReturnsSessionForNextTurn() {
        answerFor(2);
        var first = service.converse(UnifiedAdvisorRequestDto.builder().message("Hồ bơi mở cửa mấy giờ?")
                .policyHotelId(2).build(), "owner");
        assertThat(first.getSessionId()).isNotNull();
        var next = service.converse(UnifiedAdvisorRequestDto.builder().message("Bữa sáng phục vụ mấy giờ?")
                .sessionId(first.getSessionId()).expectedSessionVersion(first.getSessionVersion()).build(), "owner");
        assertThat(next.getKnowledge().getHotelId()).isEqualTo(2);
    }

    @Test void coreOutagePreservesPolicyButDoesNotReturnCardsOrFakeSuccess() {
        when(rooms.chat(any(), eq("owner"))).thenThrow(new AdvisorDependencyUnavailableException());
        answerFor(2);
        var result = service.converse(UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng ở Đà Nẵng, hồ bơi mở cửa mấy giờ?").policyHotelId(2).build(), "owner");
        assertThat(result.getAdvisor().getStatus()).isEqualTo(AdvisorStatus.TEMP_UNAVAILABLE);
        assertThat(result.getAdvisor().getRecommendations()).isEmpty();
        assertThat(result.getKnowledge().getState()).isEqualTo(KnowledgeState.ANSWERED);
        assertThat(result.getSessionId()).isNotNull();
    }

    @Test void coreOutageDoesNotReuseScopeOfPreviousSearch() {
        var previous = searchSession(1);
        var withCriteria = store.updateSession(previous.getSessionId(), "owner", previous.getSessionVersion(),
                RoomSearchCriteriaDto.builder().hotelId(1).build(), List.of(), previous.getLastResult());
        var session = store.updatePolicyScope(withCriteria.getSessionId(), "owner", withCriteria.getSessionVersion(), 1);
        when(rooms.chat(any(), eq("owner"))).thenThrow(new AdvisorDependencyUnavailableException());
        when(rag.ask(anyString(), isNull())).thenReturn(RagPublicQueryOutcome.scopeRequired(
                RagQueryResultDto.builder().status(RagAnswerStatus.SCOPE_REQUIRED).build()));
        var result = service.converse(UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng tại Đà Nẵng, hồ bơi mở cửa mấy giờ?")
                .sessionId(session.getSessionId()).expectedSessionVersion(session.getSessionVersion()).build(), "owner");
        assertThat(result.getAdvisor().getSessionVersion()).isEqualTo(session.getSessionVersion() + 1);
        assertThat(result.getKnowledge().getHotelId()).isNull();
        var followup = service.converse(UnifiedAdvisorRequestDto.builder().message("Hồ bơi mở cửa mấy giờ?")
                .sessionId(result.getSessionId()).expectedSessionVersion(result.getSessionVersion()).build(), "owner");
        assertThat(followup.getKnowledge().getHotelId()).isNull();
    }

    @Test void authorizationAndSessionErrorsStillFailClosed() {
        when(rooms.chat(any(), eq("owner"))).thenThrow(new AccessDeniedException("synthetic"));
        assertThatThrownBy(() -> service.converse(UnifiedAdvisorRequestDto.builder()
                .message("Tìm phòng, hồ bơi mở cửa mấy giờ?").policyHotelId(2).build(), "owner"))
                .isInstanceOf(AccessDeniedException.class);
        var session = searchSession(1);
        assertThatThrownBy(() -> service.converse(UnifiedAdvisorRequestDto.builder().message("Hồ bơi mở cửa mấy giờ?")
                .sessionId(session.getSessionId()).policyHotelId(2).build(), "other-owner"))
                .isInstanceOf(SessionSecurityException.class);
        assertThatThrownBy(() -> service.converse(UnifiedAdvisorRequestDto.builder().message("Hồ bơi mở cửa mấy giờ?")
                .sessionId(session.getSessionId()).expectedSessionVersion(0L).policyHotelId(2).build(), "owner"))
                .isInstanceOf(SessionVersionConflictException.class);
        verifyNoInteractions(rag);
    }
}
