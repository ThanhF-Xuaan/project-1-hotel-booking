package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.CoreCandidateDiscoveryResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.DestinationResolutionResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.DestinationResolutionStatus;
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

        // Resolve the owner-bound session before replying, including when refusing a write intent.
        UUID reqSessionId = request != null ? request.getSessionId() : null;
        RoomAdvisorSession session = sessionStore.getOrCreateSession(reqSessionId, ownerKey);

        if (request != null && request.getExpectedSessionVersion() != null) {
            if (request.getExpectedSessionVersion() != session.getSessionVersion()) {
                throw new SessionVersionConflictException("Session version conflict: expected "
                        + request.getExpectedSessionVersion() + " but was " + session.getSessionVersion());
            }
        }
        long snapshotVersion = session.getSessionVersion();

        if (request != null && criteriaValidationService.isOutOfScopeIntent(request.getMessage())) {
            return buildOutOfScopeResult(requestId, session.getSessionId(), snapshotVersion);
        }

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
            String replyMessage = valResult.getReply();
            if (request != null && criteriaValidationService.isPolicyInquiry(request.getMessage())) {
                replyMessage = "Dạ, tôi hỗ trợ tìm và gợi ý phòng. Để tra cứu quy định hủy phòng, hoàn tiền cọc hoặc chính sách của khách sạn, bạn có thể chuyển sang tab 'Chính sách & tiện ích' hoặc xem quy định chi tiết của khách sạn nhé!";
            }
            AdvisorResultDto needInfoResult = buildNeedInfoResult(
                    requestId,
                    session.getSessionId(),
                    session.getSessionVersion(),
                    valResult.getCriteria(),
                    valResult.getMissingFields(),
                    valResult.getAmbiguities(),
                    valResult.getProvenance(),
                    replyMessage
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
        DestinationResolutionResult dest = resolveDestination(mergedCriteria);
        if (dest.getStatus() != DestinationResolutionStatus.RESOLVED) {
            return destinationFailure(requestId, session, ownerKey, snapshotVersion, mergedCriteria, dest);
        }



        if (dest.getHotelId() != null) {
            mergedCriteria.setHotelId(dest.getHotelId());
        }

        // 7. Discover candidates & retrieve quotes
        CoreCandidateDiscoveryResult discovery = discoverCandidates(
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

        DestinationResolutionResult dest = resolveDestination(criteria);
        if (dest.getStatus() != DestinationResolutionStatus.RESOLVED) {
            return destinationFailure(requestId, session, ownerKey, snapshotVersion, criteria, dest);
        }



        if (dest.getHotelId() != null) {
            criteria.setHotelId(dest.getHotelId());
        }

        CoreCandidateDiscoveryResult discovery = discoverCandidates(
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

        RoomSearchCriteriaDto criteria = vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSnapshots.copy(session.getCriteria());
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

        UUID requestId = UUID.randomUUID();
        DestinationResolutionResult dest = resolveDestination(criteria);
        if (dest.getStatus() != DestinationResolutionStatus.RESOLVED) {
            return destinationFailure(requestId, session, ownerKey, session.getSessionVersion(), criteria, dest);
        }
        if (dest.getHotelId() != null) criteria.setHotelId(dest.getHotelId());

        CoreCandidateDiscoveryResult discovery = discoverCandidates(
                criteria,
                dest.getRegionId(),
                50
        );

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

    private CoreCandidateDiscoveryResult discoverCandidates(RoomSearchCriteriaDto criteria, Short regionId, int limit) {
        try { return corePort.discoverAndQuoteCandidates(criteria, regionId, limit); }
        catch (org.springframework.security.access.AccessDeniedException denied) { throw denied; }
        catch (RuntimeException unavailable) { throw new AdvisorDependencyUnavailableException(); }
    }

    private DestinationResolutionResult resolveDestination(RoomSearchCriteriaDto criteria) {
        try {
            DestinationResolutionResult result = corePort.resolveDestination(criteria.getLocationKeyword(), criteria.getHotelId(), null);
            return result == null ? DestinationResolutionResult.builder().status(DestinationResolutionStatus.UNAVAILABLE).build() : result;
        } catch (org.springframework.security.access.AccessDeniedException denied) {
            throw denied;
        } catch (RuntimeException unavailable) {
            log.warn("T12_DESTINATION_UNAVAILABLE");
            return DestinationResolutionResult.builder().status(DestinationResolutionStatus.UNAVAILABLE).build();
        }
    }

    private AdvisorResultDto destinationFailure(UUID requestId, RoomAdvisorSession session, String ownerKey,
            long expectedVersion, RoomSearchCriteriaDto criteria, DestinationResolutionResult destination) {
        AdvisorResultDto result;
        if (destination.getStatus() == DestinationResolutionStatus.UNAVAILABLE) {
            result = recommendationEngine.evaluateAndRank(requestId, session.getSessionId(), expectedVersion,
                    criteria, session.getCriteriaProvenance(), List.of(), Map.of(), Map.of(), false, Set.of(),
                    List.of(CoverageReason.CATALOG_PAGE_FAILED));
        } else {
            boolean ambiguous = destination.getStatus() == DestinationResolutionStatus.AMBIGUOUS;
            result = buildNeedInfoResult(requestId, session.getSessionId(), expectedVersion, criteria,
                    ambiguous ? List.of() : List.of("locationKeyword"), destination.getAmbiguities(),
                    session.getCriteriaProvenance(), ambiguous
                            ? "Có nhiều điểm đến phù hợp. Vui lòng chọn rõ khách sạn hoặc khu vực."
                            : "Chưa xác định được điểm đến. Vui lòng chọn khách sạn hoặc khu vực cụ thể.");
        }
        RoomAdvisorSession updated = sessionStore.updateSession(session.getSessionId(), ownerKey, expectedVersion,
                criteria, session.getCriteriaProvenance(), result);
        result.setSessionVersion(updated.getSessionVersion());
        return result;
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
                .reply("Tôi hỗ trợ tìm và xem thông tin phòng. Các thao tác đặt hoặc giữ phòng cần thực hiện tại giao diện thông thường.")
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
                .warnings(Collections.emptyList())
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
                .warnings(Collections.emptyList())
                .criteriaProvenance(provenance != null ? new ArrayList<>(provenance) : new ArrayList<>())
                .ambiguities(ambiguities != null ? new ArrayList<>(ambiguities) : new ArrayList<>())
                .build();
    }
}
