package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.CoreCandidateDiscoveryResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.DestinationResolutionResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.RoomAdvisorCorePort;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.RoomAdvisorLlmPort;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.ValidationResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSession;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionNotFoundException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionSecurityException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionVersionConflictException;

import java.util.*;

@Slf4j
@Service
public class RoomAdvisorOrchestrationService {

    private final CriteriaValidationService criteriaValidationService;
    private final DeterministicRoomRecommendationEngine recommendationEngine;
    private final RoomAdvisorCorePort corePort;
    private final RoomAdvisorSessionStore sessionStore;
    private final RoomAdvisorLlmPort llmPort;
    private final vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService ragGroundedAnswerService;

    public RoomAdvisorOrchestrationService(
            CriteriaValidationService criteriaValidationService,
            DeterministicRoomRecommendationEngine recommendationEngine,
            RoomAdvisorCorePort corePort,
            RoomAdvisorSessionStore sessionStore,
            RoomAdvisorLlmPort llmPort
    ) {
        this(criteriaValidationService, recommendationEngine, corePort, sessionStore, llmPort, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RoomAdvisorOrchestrationService(
            CriteriaValidationService criteriaValidationService,
            DeterministicRoomRecommendationEngine recommendationEngine,
            RoomAdvisorCorePort corePort,
            RoomAdvisorSessionStore sessionStore,
            RoomAdvisorLlmPort llmPort,
            @org.springframework.beans.factory.annotation.Autowired(required = false) vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService ragGroundedAnswerService
    ) {
        this.criteriaValidationService = criteriaValidationService;
        this.recommendationEngine = recommendationEngine;
        this.corePort = corePort;
        this.sessionStore = sessionStore;
        this.llmPort = llmPort;
        this.ragGroundedAnswerService = ragGroundedAnswerService;
    }

    /**
     * T12-V2-030 & R2.5: Safe knowledge inquiry helper with fail-safe degradation.
     * If RAG is unavailable or throws an exception, this never crashes and returns Optional.empty().
     */
    public Optional<vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto> queryKnowledgeSafely(
            vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto ragRequest
    ) {
        if (ragGroundedAnswerService == null || ragRequest == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(ragGroundedAnswerService.answerQuestion(ragRequest));
        } catch (Exception e) {
            log.warn("T12-V2-030: RAG knowledge service degraded/unavailable safely: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public AdvisorResultDto chat(AdvisorChatRequestDto request, String ownerKey) {
        UUID requestId = UUID.randomUUID();

        // 1. Guard against out-of-scope write intents
        if (request != null && criteriaValidationService.isOutOfScopeIntent(request.getMessage())) {
            UUID sessionId = (request.getSessionId() != null) ? request.getSessionId() : UUID.randomUUID();
            return buildOutOfScopeResult(requestId, sessionId, 1L);
        }

        // 2. Load or initialize session
        UUID reqSessionId = request != null ? request.getSessionId() : null;
        RoomAdvisorSession session = sessionStore.getOrCreateSession(reqSessionId, ownerKey);

        if (request != null && request.getExpectedSessionVersion() != null) {
            if (request.getExpectedSessionVersion() != session.getSessionVersion()) {
                throw new SessionVersionConflictException("Session version conflict: expected "
                        + request.getExpectedSessionVersion() + " but was " + session.getSessionVersion());
            }
        }
        long snapshotVersion = session.getSessionVersion();

        // 3. Extract criteria patch from natural language message
        CriteriaPatchDto extractedPatch = null;
        if (request != null && request.getMessage() != null && !request.getMessage().isBlank()) {
            extractedPatch = llmPort.extractCriteriaPatch(request.getMessage(), session.getCriteria());
        }

        // Merge explicit criteriaPatch from request over extracted patch
        CriteriaPatchDto combinedPatch = combinePatches(extractedPatch, request != null ? request.getCriteriaPatch() : null);

        // 4. Merge into session criteria with provenance tracking
        RoomSearchCriteriaDto mergedCriteria = criteriaValidationService.mergeCriteria(
                session.getCriteria(),
                combinedPatch,
                session.getCriteriaProvenance()
        );

        // 5. Semantic criteria validation
        ValidationResult valResult = criteriaValidationService.validateForSearch(
                mergedCriteria,
                session.getCriteriaProvenance()
        );

        if (!valResult.isValid()) {
            AdvisorResultDto needInfoResult = buildNeedInfoResult(
                    requestId,
                    session.getSessionId(),
                    session.getSessionVersion(),
                    valResult.getCriteria(),
                    valResult.getMissingFields(),
                    valResult.getAmbiguities(),
                    valResult.getProvenance(),
                    valResult.getReply()
            );

            RoomAdvisorSession updatedSession = sessionStore.updateSession(
                    session.getSessionId(),
                    ownerKey,
                    snapshotVersion,
                    mergedCriteria,
                    session.getCriteriaProvenance(),
                    needInfoResult
            );
            needInfoResult.setSessionVersion(updatedSession.getSessionVersion());

            return needInfoResult;
        }

        // 6. Resolve destination
        DestinationResolutionResult dest = corePort.resolveDestination(
                mergedCriteria.getLocationKeyword(),
                mergedCriteria.getHotelId(),
                null
        );

        if (dest.isAmbiguous()) {
            AdvisorResultDto ambiguousResult = buildNeedInfoResult(
                    requestId,
                    session.getSessionId(),
                    session.getSessionVersion(),
                    mergedCriteria,
                    Collections.emptyList(),
                    dest.getAmbiguities(),
                    session.getCriteriaProvenance(),
                    "Có nhiều điểm đến phù hợp với yêu cầu của bạn. Vui lòng chọn rõ khách sạn hoặc khu vực cụ thể."
            );

            RoomAdvisorSession updatedSession = sessionStore.updateSession(
                    session.getSessionId(),
                    ownerKey,
                    snapshotVersion,
                    mergedCriteria,
                    session.getCriteriaProvenance(),
                    ambiguousResult
            );
            ambiguousResult.setSessionVersion(updatedSession.getSessionVersion());

            return ambiguousResult;
        }

        if (dest.getHotelId() != null) {
            mergedCriteria.setHotelId(dest.getHotelId());
        }

        // 7. Discover candidates & retrieve quotes
        CoreCandidateDiscoveryResult discovery = corePort.discoverAndQuoteCandidates(
                mergedCriteria,
                dest.getRegionId(),
                50
        );

        // 8. Run deterministic recommendation engine
        AdvisorResultDto engineResult = recommendationEngine.evaluateAndRank(
                requestId,
                session.getSessionId(),
                session.getSessionVersion(),
                mergedCriteria,
                session.getCriteriaProvenance(),
                discovery.getCandidates(),
                discovery.getAvailabilityByRoomTypeId(),
                discovery.getQuoteByRoomTypeId(),
                discovery.isCatalogComplete(),
                discovery.getFailedRoomTypeIds(),
                discovery.getCoverageReasons(),
                discovery.getTotalDiscoveredCount(),
                discovery.getRemainingCount()
        );

        // 9. Generate polite, grounded natural language reply
        String userMsg = request != null ? request.getMessage() : "";
        String naturalReply = llmPort.generateReply(engineResult, userMsg);
        if (naturalReply != null && !naturalReply.isBlank()) {
            engineResult.setReply(naturalReply);
        }

        // 10. Update session state
        RoomAdvisorSession updatedSession = sessionStore.updateSession(
                session.getSessionId(),
                ownerKey,
                snapshotVersion,
                mergedCriteria,
                session.getCriteriaProvenance(),
                engineResult
        );
        engineResult.setSessionVersion(updatedSession.getSessionVersion());

        return engineResult;
    }

    public AdvisorResultDto search(AdvisorSearchRequestDto request, String ownerKey) {
        UUID requestId = UUID.randomUUID();

        UUID reqSessionId = request != null ? request.getSessionId() : null;
        RoomAdvisorSession session = sessionStore.getOrCreateSession(reqSessionId, ownerKey);

        if (request != null && request.getExpectedSessionVersion() != null) {
            if (request.getExpectedSessionVersion() != session.getSessionVersion()) {
                throw new SessionVersionConflictException("Session version conflict: expected "
                        + request.getExpectedSessionVersion() + " but was " + session.getSessionVersion());
            }
        }
        long snapshotVersion = session.getSessionVersion();

        RoomSearchCriteriaDto criteria = request != null ? request.getCriteria() : null;
        ValidationResult valResult = criteriaValidationService.validateForSearch(
                criteria,
                session.getCriteriaProvenance()
        );

        if (!valResult.isValid()) {
            AdvisorResultDto needInfoResult = buildNeedInfoResult(
                    requestId,
                    session.getSessionId(),
                    session.getSessionVersion(),
                    valResult.getCriteria(),
                    valResult.getMissingFields(),
                    valResult.getAmbiguities(),
                    valResult.getProvenance(),
                    valResult.getReply()
            );

            RoomAdvisorSession updatedSession = sessionStore.updateSession(
                    session.getSessionId(),
                    ownerKey,
                    snapshotVersion,
                    criteria,
                    session.getCriteriaProvenance(),
                    needInfoResult
            );
            needInfoResult.setSessionVersion(updatedSession.getSessionVersion());

            return needInfoResult;
        }

        DestinationResolutionResult dest = corePort.resolveDestination(
                criteria.getLocationKeyword(),
                criteria.getHotelId(),
                null
        );

        if (dest.isAmbiguous()) {
            AdvisorResultDto ambiguousResult = buildNeedInfoResult(
                    requestId,
                    session.getSessionId(),
                    session.getSessionVersion(),
                    criteria,
                    Collections.emptyList(),
                    dest.getAmbiguities(),
                    session.getCriteriaProvenance(),
                    "Có nhiều điểm đến phù hợp với yêu cầu của bạn. Vui lòng chọn rõ khách sạn hoặc khu vực cụ thể."
            );

            RoomAdvisorSession updatedSession = sessionStore.updateSession(
                    session.getSessionId(),
                    ownerKey,
                    snapshotVersion,
                    criteria,
                    session.getCriteriaProvenance(),
                    ambiguousResult
            );
            ambiguousResult.setSessionVersion(updatedSession.getSessionVersion());

            return ambiguousResult;
        }

        if (dest.getHotelId() != null) {
            criteria.setHotelId(dest.getHotelId());
        }

        CoreCandidateDiscoveryResult discovery = corePort.discoverAndQuoteCandidates(
                criteria,
                dest.getRegionId(),
                50
        );

        AdvisorResultDto engineResult = recommendationEngine.evaluateAndRank(
                requestId,
                session.getSessionId(),
                session.getSessionVersion(),
                criteria,
                session.getCriteriaProvenance(),
                discovery.getCandidates(),
                discovery.getAvailabilityByRoomTypeId(),
                discovery.getQuoteByRoomTypeId(),
                discovery.isCatalogComplete(),
                discovery.getFailedRoomTypeIds(),
                discovery.getCoverageReasons(),
                discovery.getTotalDiscoveredCount(),
                discovery.getRemainingCount()
        );

        RoomAdvisorSession updatedSession = sessionStore.updateSession(
                session.getSessionId(),
                ownerKey,
                snapshotVersion,
                criteria,
                session.getCriteriaProvenance(),
                engineResult
        );
        engineResult.setSessionVersion(updatedSession.getSessionVersion());

        return engineResult;
    }

    public AdvisorResultDto refresh(AdvisorRefreshRequestDto request, String ownerKey) {
        if (request == null || request.getSessionId() == null) {
            throw new IllegalArgumentException("Session ID is required for refresh operation");
        }

        RoomAdvisorSession session = sessionStore.getSession(request.getSessionId(), ownerKey)
                .orElseThrow(() -> new SessionNotFoundException("Session not found or expired"));

        if (request.getExpectedSessionVersion() != null) {
            if (request.getExpectedSessionVersion() != session.getSessionVersion()) {
                throw new SessionVersionConflictException("Session version conflict: expected "
                        + request.getExpectedSessionVersion() + " but was " + session.getSessionVersion());
            }
        }

        RoomSearchCriteriaDto criteria = session.getCriteria();
        if (criteria == null) {
            return buildNeedInfoResult(
                    UUID.randomUUID(),
                    session.getSessionId(),
                    session.getSessionVersion(),
                    null,
                    List.of("locationKeyword", "checkInDate", "checkOutDate", "adults"),
                    Collections.emptyList(),
                    session.getCriteriaProvenance(),
                    "Chưa có tiêu chí tìm kiếm nào được lưu trong phiên."
            );
        }

        DestinationResolutionResult dest = corePort.resolveDestination(
                criteria.getLocationKeyword(),
                criteria.getHotelId(),
                null
        );

        CoreCandidateDiscoveryResult discovery = corePort.discoverAndQuoteCandidates(
                criteria,
                dest.getRegionId(),
                50
        );

        UUID requestId = UUID.randomUUID();
        long nextVersion = session.getSessionVersion() + 1;

        AdvisorResultDto refreshedResult = recommendationEngine.evaluateAndRank(
                requestId,
                session.getSessionId(),
                nextVersion,
                criteria,
                session.getCriteriaProvenance(),
                discovery.getCandidates(),
                discovery.getAvailabilityByRoomTypeId(),
                discovery.getQuoteByRoomTypeId(),
                discovery.isCatalogComplete(),
                discovery.getFailedRoomTypeIds(),
                discovery.getCoverageReasons(),
                discovery.getTotalDiscoveredCount(),
                discovery.getRemainingCount()
        );

        RoomAdvisorSession updatedSession = sessionStore.updateSession(
                session.getSessionId(),
                ownerKey,
                session.getSessionVersion(),
                criteria,
                session.getCriteriaProvenance(),
                refreshedResult
        );
        refreshedResult.setSessionVersion(updatedSession.getSessionVersion());

        return refreshedResult;
    }

    private CriteriaPatchDto combinePatches(CriteriaPatchDto primary, CriteriaPatchDto secondary) {
        if (primary == null) {
            return secondary;
        }
        if (secondary == null) {
            return primary;
        }

        CriteriaPatchDto.CriteriaPatchDtoBuilder b = CriteriaPatchDto.builder();
        b.hotelId(secondary.getHotelId() != null ? secondary.getHotelId() : primary.getHotelId());
        b.locationKeyword(secondary.getLocationKeyword() != null ? secondary.getLocationKeyword() : primary.getLocationKeyword());
        b.checkInDate(secondary.getCheckInDate() != null ? secondary.getCheckInDate() : primary.getCheckInDate());
        b.checkOutDate(secondary.getCheckOutDate() != null ? secondary.getCheckOutDate() : primary.getCheckOutDate());
        b.adults(secondary.getAdults() != null ? secondary.getAdults() : primary.getAdults());
        b.children(secondary.getChildren() != null ? secondary.getChildren() : primary.getChildren());
        b.infants(secondary.getInfants() != null ? secondary.getInfants() : primary.getInfants());
        b.extraBeds(secondary.getExtraBeds() != null ? secondary.getExtraBeds() : primary.getExtraBeds());
        b.roomsCount(secondary.getRoomsCount() != null ? secondary.getRoomsCount() : primary.getRoomsCount());
        b.requiredFeatureCodes(secondary.getRequiredFeatureCodes() != null ? secondary.getRequiredFeatureCodes() : primary.getRequiredFeatureCodes());
        b.preferredFeatureCodes(secondary.getPreferredFeatureCodes() != null ? secondary.getPreferredFeatureCodes() : primary.getPreferredFeatureCodes());
        b.budget(secondary.getBudget() != null ? secondary.getBudget() : primary.getBudget());

        return b.build();
    }

    private AdvisorResultDto buildOutOfScopeResult(UUID requestId, UUID sessionId, long sessionVersion) {
        return AdvisorResultDto.builder()
                .contractVersion("2.0")
                .requestId(requestId)
                .sessionId(sessionId)
                .sessionVersion(sessionVersion)
                .status(AdvisorStatus.OUT_OF_SCOPE)
                .reply("Trợ lý hỗ trợ tìm và xem thông tin phòng. Các thao tác đặt hoặc giữ phòng cần thực hiện tại giao diện thông thường.")
                .criteria(RoomSearchCriteriaDto.builder()
                        .roomsCount(1)
                        .children(0)
                        .extraBeds(0)
                        .requiredFeatureCodes(new ArrayList<>())
                        .preferredFeatureCodes(new ArrayList<>())
                        .build())
                .missingFields(new ArrayList<>())
                .recommendations(new ArrayList<>())
                .coverage(SearchCoverageDto.builder()
                        .status(CoverageStatus.NOT_RUN)
                        .catalogComplete(false)
                        .discoveredCount(0)
                        .evaluatedCount(0)
                        .failedCount(0)
                        .remainingCount(0)
                        .verifiedMatchCount(0)
                        .reasons(new ArrayList<>())
                        .build())
                .knowledgeStatus("DISABLED")
                .sources(Collections.emptyList())
                .warnings(List.of(WarningCode.SERVICE_FEE_UNVERIFIED, WarningCode.CURRENCY_SYNTHETIC_ASSUMPTION))
                .criteriaProvenance(new ArrayList<>())
                .ambiguities(new ArrayList<>())
                .build();
    }

    private AdvisorResultDto buildNeedInfoResult(
            UUID requestId,
            UUID sessionId,
            long sessionVersion,
            RoomSearchCriteriaDto criteria,
            List<String> missingFields,
            List<AmbiguityDto> ambiguities,
            List<CriteriaEvidenceDto> provenance,
            String reply
    ) {
        RoomSearchCriteriaDto safeCriteria = criteria != null ? criteria : RoomSearchCriteriaDto.builder()
                .roomsCount(1)
                .children(0)
                .extraBeds(0)
                .requiredFeatureCodes(new ArrayList<>())
                .preferredFeatureCodes(new ArrayList<>())
                .build();

        return AdvisorResultDto.builder()
                .contractVersion("2.0")
                .requestId(requestId)
                .sessionId(sessionId)
                .sessionVersion(sessionVersion)
                .status(AdvisorStatus.NEED_INFO)
                .reply(reply != null ? reply : "Vui lòng cung cấp thêm thông tin tìm kiếm phòng.")
                .criteria(safeCriteria)
                .missingFields(missingFields != null ? new ArrayList<>(missingFields) : new ArrayList<>())
                .recommendations(new ArrayList<>())
                .coverage(SearchCoverageDto.builder()
                        .status(CoverageStatus.NOT_RUN)
                        .catalogComplete(false)
                        .discoveredCount(0)
                        .evaluatedCount(0)
                        .failedCount(0)
                        .remainingCount(0)
                        .verifiedMatchCount(0)
                        .reasons(new ArrayList<>())
                        .build())
                .knowledgeStatus("DISABLED")
                .sources(Collections.emptyList())
                .warnings(List.of(WarningCode.SERVICE_FEE_UNVERIFIED, WarningCode.CURRENCY_SYNTHETIC_ASSUMPTION))
                .criteriaProvenance(provenance != null ? new ArrayList<>(provenance) : new ArrayList<>())
                .ambiguities(ambiguities != null ? new ArrayList<>(ambiguities) : new ArrayList<>())
                .build();
    }
}
