package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.KnowledgeState;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.organization.service.PublicHotelEligibilityReader;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagPublicQueryFacadeTest {

    @Mock
    private RagGroundedAnswerService ragService;

    @Mock
    private PublicHotelEligibilityReader hotelEligibility;

    @Mock
    private RagRequestBudget requestBudget;

    private RagPublicQueryFacade facade;

    @BeforeEach
    void setUp() {
        facade = new RagPublicQueryFacade(ragService, hotelEligibility, requestBudget);
    }

    @Test
    @DisplayName("Valid question and active hotel returns ANSWERED")
    void validQuestionAndActiveHotelReturnsAnswered() throws Exception {
        when(requestBudget.execute(any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return callable.call();
        });
        when(hotelEligibility.isActivePublicHotel((short) 2)).thenReturn(true);
        when(ragService.answerQuestion(any(RagQueryRequestDto.class))).thenReturn(RagQueryResultDto.builder()
                .status(RagAnswerStatus.GROUNDED_ANSWER)
                .answer("Trẻ em được miễn phí buffet.")
                .build());

        RagPublicQueryOutcome outcome = facade.ask("Bé có được miễn phí buffet sáng không?", 2);

        assertThat(outcome.getState()).isEqualTo(KnowledgeState.ANSWERED);
        assertThat(outcome.getResult().getAnswer()).contains("miễn phí");
    }

    @Test
    @DisplayName("Blank or over-length question returns SCOPE_REQUIRED without touching budget")
    void invalidQuestionLengthReturnsScopeRequired() {
        RagPublicQueryOutcome blank = facade.ask("   ", 1);
        assertThat(blank.getState()).isEqualTo(KnowledgeState.SCOPE_REQUIRED);

        String tooLong = "a".repeat(501);
        RagPublicQueryOutcome over = facade.ask(tooLong, 1);
        assertThat(over.getState()).isEqualTo(KnowledgeState.SCOPE_REQUIRED);
    }

    @Test
    @DisplayName("Invalid hotelId bounds returns SCOPE_REQUIRED")
    void invalidHotelIdBoundsReturnsScopeRequired() {
        RagPublicQueryOutcome negative = facade.ask("Hồ bơi mở mấy giờ?", -1);
        assertThat(negative.getState()).isEqualTo(KnowledgeState.SCOPE_REQUIRED);

        RagPublicQueryOutcome tooLarge = facade.ask("Hồ bơi mở mấy giờ?", 40000);
        assertThat(tooLarge.getState()).isEqualTo(KnowledgeState.SCOPE_REQUIRED);
    }

    @Test
    @DisplayName("Inactive hotel returns SCOPE_REQUIRED inside budget")
    void inactiveHotelReturnsScopeRequired() throws Exception {
        when(requestBudget.execute(any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return callable.call();
        });
        when(hotelEligibility.isActivePublicHotel((short) 99)).thenReturn(false);

        RagPublicQueryOutcome outcome = facade.ask("Hồ bơi mở mấy giờ?", 99);

        assertThat(outcome.getState()).isEqualTo(KnowledgeState.SCOPE_REQUIRED);
        assertThat(outcome.getResult().getAnswer()).contains("công khai");
    }

    @Test
    @DisplayName("LimitExceededException maps to RATE_LIMITED with retryAfterSeconds")
    void rateLimitedMapsToRateLimitedState() throws Exception {
        when(requestBudget.execute(any())).thenThrow(new RagRequestBudget.LimitExceededException(15));

        RagPublicQueryOutcome outcome = facade.ask("Hồ bơi mở mấy giờ?", 1);

        assertThat(outcome.getState()).isEqualTo(KnowledgeState.RATE_LIMITED);
        assertThat(outcome.getRetryAfterSeconds()).isEqualTo(15L);
    }

    @Test
    @DisplayName("Upstream exception maps to UNAVAILABLE")
    void upstreamExceptionMapsToUnavailable() throws Exception {
        when(requestBudget.execute(any())).thenThrow(new RuntimeException("Gemini timeout"));

        RagPublicQueryOutcome outcome = facade.ask("Hồ bơi mở mấy giờ?", 1);

        assertThat(outcome.getState()).isEqualTo(KnowledgeState.UNAVAILABLE);
    }
}
