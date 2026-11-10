package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.KnowledgeState;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.UnifiedIntent;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSession;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionNotFoundException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionVersionConflictException;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagPublicQueryFacade;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagPublicQueryOutcome;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagRequestBudget;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrator for the unified conversational flow, combining authoritative room recommendations
 * and grounded RAG knowledge in a single conversational turn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedAdvisorService {

    private final PolicyIntentClassifier intentClassifier;
    private final RoomAdvisorOrchestrationService roomAdvisorService;
    private final RagPublicQueryFacade ragFacade;
    private final RoomAdvisorSessionStore sessionStore;

    public UnifiedAdvisorResultDto converse(UnifiedAdvisorRequestDto request, String ownerKey) {
        Objects.requireNonNull(request, "request must not be null");
        UUID requestId = UUID.randomUUID();

        // 1. Classify and partition intent
        PolicyIntentClassifier.ClassificationResult classification =
                intentClassifier.classifyAndSplit(request.getMessage());

        if (classification.isWriteIntent()) {
            AdvisorChatRequestDto chatReq = AdvisorChatRequestDto.builder()
                    .message(request.getMessage())
                    .sessionId(request.getSessionId())
                    .expectedSessionVersion(request.getExpectedSessionVersion())
                    .build();
            AdvisorResultDto advisorResult = roomAdvisorService.chat(chatReq, ownerKey);
            return UnifiedAdvisorResultDto.builder()
                    .contractVersion("2.1-proposed")
                    .requestId(requestId)
                    .intent(UnifiedIntent.OUT_OF_SCOPE)
                    .advisor(advisorResult)
                    .knowledge(null)
                    .build();
        }

        AdvisorResultDto advisorResult = null;
        RoomAdvisorSession existingSession = null;

        // 2. Validate session if POLICY_ONLY
        if (classification.getIntent() == UnifiedIntent.POLICY_ONLY) {
            if (request.getSessionId() != null) {
                existingSession = sessionStore.getSession(request.getSessionId(), ownerKey)
                        .orElseThrow(() -> new SessionNotFoundException("Session not found or expired"));
                if (request.getExpectedSessionVersion() != null
                        && !request.getExpectedSessionVersion().equals(existingSession.getSessionVersion())) {
                    throw new SessionVersionConflictException("Session version conflict: expected "
                            + request.getExpectedSessionVersion() + " but was " + existingSession.getSessionVersion());
                }
            }
        } else {
            // SEARCH_ONLY or SEARCH_AND_POLICY
            AdvisorChatRequestDto chatReq = AdvisorChatRequestDto.builder()
                    .message(classification.getSearchText())
                    .sessionId(request.getSessionId())
                    .expectedSessionVersion(request.getExpectedSessionVersion())
                    .build();
            advisorResult = roomAdvisorService.chat(chatReq, ownerKey);
            if (advisorResult.getStatus() == AdvisorStatus.OUT_OF_SCOPE) {
                return UnifiedAdvisorResultDto.builder()
                        .contractVersion("2.1-proposed")
                        .requestId(requestId)
                        .intent(UnifiedIntent.OUT_OF_SCOPE)
                        .advisor(advisorResult)
                        .knowledge(null)
                        .build();
            }
        }

        // 3. Resolve Scope & Execute Knowledge Query
        KnowledgeAnswerDto knowledgeAnswer = null;
        if (classification.getPolicyQuestion() != null) {
            knowledgeAnswer = resolveAndQueryKnowledge(
                    classification.getPolicyQuestion(),
                    request.getPolicyHotelId(),
                    advisorResult,
                    existingSession
            );
        }

        return UnifiedAdvisorResultDto.builder()
                .contractVersion("2.1-proposed")
                .requestId(requestId)
                .intent(classification.getIntent())
                .advisor(advisorResult)
                .knowledge(knowledgeAnswer)
                .build();
    }

    private KnowledgeAnswerDto resolveAndQueryKnowledge(
            String policyQuestion,
            Integer explicitPolicyHotelId,
            AdvisorResultDto advisorResult,
            RoomAdvisorSession existingSession
    ) {
        String safePolicyQuestion = RagRequestBudget.redactQuestion(policyQuestion);
        Integer targetHotelId = explicitPolicyHotelId;
        String targetHotelName = null;
        List<HotelChoiceDto> choices = null;

        // Check recommendations if targetHotelId not explicitly provided
        if (targetHotelId == null && advisorResult != null && advisorResult.getRecommendations() != null
                && !advisorResult.getRecommendations().isEmpty()) {
            Map<Integer, String> hotelsFound = advisorResult.getRecommendations().stream()
                    .filter(c -> c.getRoom() != null && c.getRoom().getHotelId() != null)
                    .collect(Collectors.toMap(
                            c -> c.getRoom().getHotelId(),
                            c -> c.getRoom().getHotelName() != null ? c.getRoom().getHotelName() : "Khách sạn " + c.getRoom().getHotelId(),
                            (existing, replacement) -> existing,
                            LinkedHashMap::new
                    ));

            if (hotelsFound.size() == 1) {
                Map.Entry<Integer, String> single = hotelsFound.entrySet().iterator().next();
                targetHotelId = single.getKey();
                targetHotelName = single.getValue();
            } else if (hotelsFound.size() > 1) {
                // Multiple distinct hotels: fail-closed to avoid attributing wrong policies
                choices = hotelsFound.entrySet().stream()
                        .map(e -> HotelChoiceDto.builder().hotelId(e.getKey()).hotelName(e.getValue()).build())
                        .toList();
                return KnowledgeAnswerDto.builder()
                        .state(KnowledgeState.SCOPE_REQUIRED)
                        .policyQuestion(safePolicyQuestion)
                        .hotelChoices(choices)
                        .result(RagQueryResultDto.builder()
                                .status(RagAnswerStatus.SCOPE_REQUIRED)
                                .answer("Có nhiều khách sạn phù hợp với tìm kiếm của bạn. Vui lòng chọn khách sạn bên dưới để xem chính sách chi tiết.")
                                .build())
                        .build();
            }
        }

        // Fallback to advisorResult criteria if recommendations was empty (e.g. NEED_INFO or NO_MATCH)
        if (targetHotelId == null && advisorResult != null && advisorResult.getCriteria() != null
                && advisorResult.getCriteria().getHotelId() != null) {
            targetHotelId = advisorResult.getCriteria().getHotelId();
            targetHotelName = advisorResult.getCriteria().getLocationKeyword();
        }

        // Fallback to session criteria if available
        if (targetHotelId == null && existingSession != null && existingSession.getCriteria() != null
                && existingSession.getCriteria().getHotelId() != null) {
            targetHotelId = existingSession.getCriteria().getHotelId();
        }

        // Execute query through facade
        RagPublicQueryOutcome outcome = ragFacade.ask(safePolicyQuestion, targetHotelId);

        return KnowledgeAnswerDto.builder()
                .state(outcome.getState())
                .hotelId(targetHotelId)
                .hotelName(targetHotelName)
                .policyQuestion(safePolicyQuestion)
                .result(outcome.getResult())
                .retryAfterSeconds(outcome.getRetryAfterSeconds())
                .hotelChoices(choices)
                .build();
    }
}
