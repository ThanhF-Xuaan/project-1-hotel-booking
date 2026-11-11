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
        boolean coreFailed = false;
        RoomAdvisorSession existingSession = null;

        // 2. Validate session if POLICY_ONLY
        if (classification.getIntent() == UnifiedIntent.POLICY_ONLY) {
            if (request.getSessionId() != null) {
                existingSession = validateSession(request, ownerKey);
            }
        } else {
            // SEARCH_ONLY or SEARCH_AND_POLICY
            AdvisorChatRequestDto chatReq = AdvisorChatRequestDto.builder()
                    .message(classification.getSearchText())
                    .sessionId(request.getSessionId())
                    .expectedSessionVersion(request.getExpectedSessionVersion())
                    .build();
            try {
                advisorResult = roomAdvisorService.chat(chatReq, ownerKey);
            } catch (AdvisorDependencyUnavailableException unavailable) {
                coreFailed = true;
                if (classification.getPolicyQuestion() == null) throw unavailable;
                // Do not infer scope from an older search after a failed new search.
                // Session and authorization failures deliberately do not enter this catch.
                RoomAdvisorSession validated = request.getSessionId() != null ? validateSession(request, ownerKey) : null;
                advisorResult = AdvisorResultDto.builder()
                        .requestId(requestId)
                        .sessionId(request.getSessionId())
                        .sessionVersion(validated != null ? validated.getSessionVersion() : 0L)
                        .status(AdvisorStatus.TEMP_UNAVAILABLE)
                        .reply("Tìm phòng tạm thời không khả dụng. Tôi vẫn có thể tra cứu chính sách khi xác định được khách sạn.")
                        .build();
                if (validated != null) {
                    existingSession = sessionStore.updateSession(validated.getSessionId(), ownerKey,
                            validated.getSessionVersion(), null, null, advisorResult);
                    advisorResult.setSessionVersion(existingSession.getSessionVersion());
                }
            }
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

        // A successful search commits criteria first; its store clears a previous policy choice
        // on material criteria changes. Never reuse stale context after a Core failure.
        if (advisorResult != null && advisorResult.getSessionId() != null
                && !coreFailed) {
            existingSession = sessionStore.getSession(advisorResult.getSessionId(), ownerKey)
                    .orElseThrow(() -> new SessionNotFoundException("Session not found or expired"));
            if (existingSession.getSessionVersion() != advisorResult.getSessionVersion()) {
                throw new SessionVersionConflictException("Session changed after search");
            }
        }

        // 3. Resolve Scope & Execute Knowledge Query
        KnowledgeAnswerDto knowledgeAnswer = null;
        if (classification.getPolicyQuestion() != null) {
            knowledgeAnswer = resolveAndQueryKnowledge(
                    classification.getPolicyQuestion(),
                    request.getPolicyHotelId(),
                    advisorResult,
                    coreFailed ? null : existingSession
            );
        }

        // Persist only an explicitly selected, publicly verified hotel. No raw question is stored.
        RoomAdvisorSession responseSession = existingSession;
        if (request.getPolicyHotelId() != null && knowledgeAnswer != null
                && knowledgeAnswer.getState() == KnowledgeState.ANSWERED) {
            if (responseSession == null) {
                responseSession = sessionStore.getOrCreateSession(request.getSessionId(), ownerKey);
            }
            responseSession = sessionStore.updatePolicyScope(responseSession.getSessionId(), ownerKey,
                    responseSession.getSessionVersion(), request.getPolicyHotelId());
            if (advisorResult != null) {
                advisorResult.setSessionId(responseSession.getSessionId());
                advisorResult.setSessionVersion(responseSession.getSessionVersion());
            }
        }

        UUID responseSessionId = null;
        Long responseVersion = null;
        if (responseSession != null) {
            responseSessionId = responseSession.getSessionId();
            responseVersion = responseSession.getSessionVersion();
        } else if (advisorResult != null && advisorResult.getSessionId() != null) {
            responseSessionId = advisorResult.getSessionId();
            responseVersion = advisorResult.getSessionVersion();
        }
        return UnifiedAdvisorResultDto.builder()
                .contractVersion("2.1-proposed")
                .requestId(requestId)
                .sessionId(responseSessionId)
                .sessionVersion(responseVersion)
                .intent(classification.getIntent())
                .advisor(advisorResult)
                .knowledge(knowledgeAnswer)
                .build();
    }


    private RoomAdvisorSession validateSession(UnifiedAdvisorRequestDto request, String ownerKey) {
        RoomAdvisorSession session = sessionStore.getSession(request.getSessionId(), ownerKey)
                .orElseThrow(() -> new SessionNotFoundException("Session not found or expired"));
        if (request.getExpectedSessionVersion() != null
                && !request.getExpectedSessionVersion().equals(session.getSessionVersion())) {
            throw new SessionVersionConflictException("Session version conflict");
        }
        return session;
    }

    private KnowledgeAnswerDto resolveAndQueryKnowledge(
            String policyQuestion,
            Integer explicitPolicyHotelId,
            AdvisorResultDto advisorResult,
            RoomAdvisorSession existingSession
    ) {
        String safePolicyQuestion = RagRequestBudget.redactQuestion(policyQuestion);
        Integer targetHotelId = explicitPolicyHotelId;
        if (targetHotelId == null && existingSession != null) {
            targetHotelId = existingSession.getPolicyHotelId();
        }
        AdvisorResultDto scopeResult = advisorResult != null ? advisorResult
                : existingSession != null ? existingSession.getLastResult() : null;
        String targetHotelName = null;
        List<HotelChoiceDto> choices = null;

        // Check recommendations if targetHotelId not explicitly provided
        if (targetHotelId == null && scopeResult != null && scopeResult.getRecommendations() != null
                && !scopeResult.getRecommendations().isEmpty()) {
            Map<Integer, String> hotelsFound = scopeResult.getRecommendations().stream()
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
                && (existingSession.getLastResult() == null
                    || existingSession.getLastResult().getStatus() != AdvisorStatus.TEMP_UNAVAILABLE)
                && existingSession.getCriteria().getHotelId() != null) {
            targetHotelId = existingSession.getCriteria().getHotelId();
        }

        // Execute query through facade
        RagPublicQueryOutcome outcome = ragFacade.ask(safePolicyQuestion, targetHotelId);

        return KnowledgeAnswerDto.builder()
                .state(KnowledgeState.valueOf(outcome.getState().name()))
                .hotelId(targetHotelId)
                .hotelName(targetHotelName)
                .policyQuestion(safePolicyQuestion)
                .result(outcome.getResult())
                .retryAfterSeconds(outcome.getRetryAfterSeconds())
                .hotelChoices(choices)
                .build();
    }
}
