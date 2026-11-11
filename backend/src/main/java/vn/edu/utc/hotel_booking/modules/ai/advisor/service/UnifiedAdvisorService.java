package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ComparisonFocus;
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
 * Orchestrator for the unified conversational flow, combining authoritative room recommendations,
 * multi-criteria room comparisons, and grounded RAG knowledge in a single conversational turn.
 */
@Slf4j
@Service
public class UnifiedAdvisorService {

    private final PolicyIntentClassifier intentClassifier;
    private final RoomAdvisorOrchestrationService roomAdvisorService;
    private final RagPublicQueryFacade ragFacade;
    private final RoomAdvisorSessionStore sessionStore;
    private final RoomComparisonService roomComparisonService;

    public UnifiedAdvisorService(
            PolicyIntentClassifier intentClassifier,
            RoomAdvisorOrchestrationService roomAdvisorService,
            RagPublicQueryFacade ragFacade,
            RoomAdvisorSessionStore sessionStore
    ) {
        this(intentClassifier, roomAdvisorService, ragFacade, sessionStore, new RoomComparisonService(ragFacade));
    }

    @Autowired
    public UnifiedAdvisorService(
            PolicyIntentClassifier intentClassifier,
            RoomAdvisorOrchestrationService roomAdvisorService,
            RagPublicQueryFacade ragFacade,
            RoomAdvisorSessionStore sessionStore,
            RoomComparisonService roomComparisonService
    ) {
        this.intentClassifier = intentClassifier;
        this.roomAdvisorService = roomAdvisorService;
        this.ragFacade = ragFacade;
        this.sessionStore = sessionStore;
        this.roomComparisonService = roomComparisonService;
    }

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
        boolean reusedSessionResultForComparison = false;

        UnifiedIntent effectiveIntent = classification.getIntent();
        String effectivePolicyQuestion = classification.getPolicyQuestion();

        boolean hasExplicitCompareIds = request.getCompareRoomTypeIds() != null
                && !request.getCompareRoomTypeIds().isEmpty();
        if (hasExplicitCompareIds && !classification.isWriteIntent()) {
            if (effectiveIntent == UnifiedIntent.SEARCH_ONLY) {
                effectiveIntent = UnifiedIntent.ROOM_COMPARISON;
            } else if (effectiveIntent == UnifiedIntent.POLICY_ONLY) {
                if (!intentClassifier.hasExplicitPolicyMarker(request.getMessage())) {
                    // Question was asking about the rooms to compare (e.g. "Gia đình có con nhỏ nên chọn cái nào?"),
                    // not a hotel policy inquiry.
                    effectiveIntent = UnifiedIntent.ROOM_COMPARISON;
                    effectivePolicyQuestion = null;
                } else {
                    effectiveIntent = UnifiedIntent.SEARCH_AND_COMPARISON;
                }
            } else if (effectiveIntent == UnifiedIntent.SEARCH_AND_POLICY) {
                effectiveIntent = UnifiedIntent.SEARCH_AND_COMPARISON;
            }
        }
        ComparisonFocus comparisonFocus = classification.getComparisonFocus() != null
                && classification.getComparisonFocus() != ComparisonFocus.GENERAL
                ? classification.getComparisonFocus()
                : intentClassifier.detectComparisonFocus(request.getMessage());

        // 2. Execute session/search flow depending on intent
        if (effectiveIntent == UnifiedIntent.POLICY_ONLY) {
            if (request.getSessionId() != null) {
                existingSession = validateSession(request, ownerKey);
            }
        } else if (effectiveIntent == UnifiedIntent.ROOM_COMPARISON) {
            if (request.getSessionId() != null) {
                existingSession = validateSession(request, ownerKey);
            }
            if (existingSession != null && existingSession.getLastResult() != null
                    && existingSession.getLastResult().getRecommendations() != null
                    && !existingSession.getLastResult().getRecommendations().isEmpty()) {
                // Read-only reuse of verified recommendations from active session (no sessionVersion bump).
                // If recommendations.size() < 2, RoomComparisonService will cleanly return INSUFFICIENT_CANDIDATES.
                advisorResult = existingSession.getLastResult();
                reusedSessionResultForComparison = true;
            } else {
                AdvisorChatRequestDto chatReq = AdvisorChatRequestDto.builder()
                        .message(classification.getSearchText())
                        .sessionId(request.getSessionId())
                        .expectedSessionVersion(request.getExpectedSessionVersion())
                        .build();
                try {
                    advisorResult = roomAdvisorService.chat(chatReq, ownerKey);
                } catch (AdvisorDependencyUnavailableException unavailable) {
                    coreFailed = true;
                    if (effectivePolicyQuestion == null) throw unavailable;
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
        } else {
            // SEARCH_ONLY, SEARCH_AND_POLICY, or SEARCH_AND_COMPARISON
            AdvisorChatRequestDto chatReq = AdvisorChatRequestDto.builder()
                    .message(classification.getSearchText())
                    .sessionId(request.getSessionId())
                    .expectedSessionVersion(request.getExpectedSessionVersion())
                    .build();
            try {
                advisorResult = roomAdvisorService.chat(chatReq, ownerKey);
            } catch (AdvisorDependencyUnavailableException unavailable) {
                coreFailed = true;
                if (effectivePolicyQuestion == null) throw unavailable;
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
                && !coreFailed && !reusedSessionResultForComparison) {
            existingSession = sessionStore.getSession(advisorResult.getSessionId(), ownerKey)
                    .orElseThrow(() -> new SessionNotFoundException("Session not found or expired"));
            if (existingSession.getSessionVersion() != advisorResult.getSessionVersion()) {
                throw new SessionVersionConflictException("Session changed after search");
            }
        }

        // 3. Build Multi-Criteria Room Comparison Matrix when requested
        RoomComparisonMatrixDto comparisonMatrix = null;
        if (effectiveIntent == UnifiedIntent.ROOM_COMPARISON
                || (effectiveIntent == UnifiedIntent.SEARCH_AND_COMPARISON && !coreFailed)) {
            comparisonMatrix = roomComparisonService.compareRooms(
                    advisorResult,
                    classification.getTargetRoomHints(),
                    comparisonFocus,
                    request.getCompareRoomTypeIds()
            );
        }

        // 4. Resolve Scope & Execute Knowledge Query
        KnowledgeAnswerDto knowledgeAnswer = null;
        if (effectivePolicyQuestion != null) {
            knowledgeAnswer = resolveAndQueryKnowledge(
                    effectivePolicyQuestion,
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
                .intent(effectiveIntent)
                .advisor(advisorResult)
                .knowledge(knowledgeAnswer)
                .comparison(comparisonMatrix)
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
