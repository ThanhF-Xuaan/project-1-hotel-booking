package vn.edu.utc.hotel_booking.modules.ai.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.CoreCandidateDiscoveryResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.DestinationResolutionResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.RoomAdvisorCorePort;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCheck;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCoverage;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ServiceFeeStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.RoomAdvisorLlmPort;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.CandidateAvailability;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.RoomCandidate;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.CriteriaValidationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.DeterministicRoomRecommendationEngine;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.RoomAdvisorOrchestrationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.InMemoryRoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.rag.controller.PublicRagKnowledgeController;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagIndependentDegradationTest {

    @Mock
    private RagGroundedAnswerService failingRagService;

    @Mock
    private RoomAdvisorCorePort corePort;

    @Mock
    private RoomAdvisorLlmPort llmPort;

    private CriteriaValidationService criteriaValidationService;
    private DeterministicRoomRecommendationEngine recommendationEngine;
    private RoomAdvisorSessionStore sessionStore;
    private RoomAdvisorOrchestrationService orchestrationService;
    private PublicRagKnowledgeController publicRagKnowledgeController;

    @BeforeEach
    void setUp() {
        criteriaValidationService = new CriteriaValidationService();
        recommendationEngine = new DeterministicRoomRecommendationEngine();
        sessionStore = new InMemoryRoomAdvisorSessionStore();

        // OrchestrationService wired with failing RAG service
        orchestrationService = new RoomAdvisorOrchestrationService(
                criteriaValidationService,
                recommendationEngine,
                corePort,
                sessionStore,
                llmPort,
                failingRagService
        );

        publicRagKnowledgeController = new PublicRagKnowledgeController(failingRagService);
    }

    @Test
    @DisplayName("T12-V2-030: queryKnowledgeSafely catches RAG outage exception and returns Optional.empty() without crashing")
    void shouldDegradeSafelyWhenRagThrowsRuntimeException() {
        when(failingRagService.answerQuestion(any()))
                .thenThrow(new RuntimeException("Vector store connection timeout"));

        RagQueryRequestDto req = RagQueryRequestDto.builder()
                .hotelId(1)
                .question("Hồ bơi ở tầng mấy?")
                .build();

        // Must not throw exception
        Optional<RagQueryResultDto> result = orchestrationService.queryKnowledgeSafely(req);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("T12-V2-030 & R2.5: Room recommendation core (Release 1) remains 100% operational when RAG is degraded/unavailable")
    void shouldEnsureRelease1RecommendationFlowIsIndependentOfRagOutage() {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .hotelId(1)
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .build();

        AdvisorSearchRequestDto searchRequest = AdvisorSearchRequestDto.builder()
                .contractVersion("2.0")
                .criteria(criteria)
                .build();

        DestinationResolutionResult dest = DestinationResolutionResult.builder()
                .hotelId(1)
                .resolvedName("Synthetic Hotel 1")
                .ambiguous(false)
                .build();

        when(corePort.resolveDestination(any(), any(), any())).thenReturn(dest);

        RoomCandidate candidate = RoomCandidate.builder()
                .hotelId(1)
                .hotelName("Synthetic Hotel 1")
                .hotelRoomTypeId(1001)
                .roomTypeCode("DELUXE")
                .roomTypeName("Deluxe Room")
                .maxAdults(2)
                .maxChildren(1)
                .maxTotalGuests(3)
                .extraBedsAllowed(1)
                .status("ACTIVE")
                .features(List.of(new RoomFeatureDto("WIFI", "Wi-Fi")))
                .build();

        CandidateAvailability av1 = CandidateAvailability.builder()
                .hotelRoomTypeId(1001)
                .availabilityDate(LocalDate.of(2026, 11, 20))
                .availableCount(2)
                .build();

        CandidateAvailability av2 = CandidateAvailability.builder()
                .hotelRoomTypeId(1001)
                .availabilityDate(LocalDate.of(2026, 11, 21))
                .availableCount(2)
                .build();

        PriceQuoteDto quote = PriceQuoteDto.builder()
                .hotelRoomTypeId(1001)
                .source("BACKEND_PRICING")
                .quoteId("q-1001")
                .observedAt(OffsetDateTime.now())
                .totalNights(2)
                .currency("VND")
                .preTaxAmount("1000000")
                .vatPercent("10")
                .vatAmount("105000")
                .finalTotalAmount("1155000")
                .serviceFeeStatus(ServiceFeeStatus.INCLUDED)
                .serviceFeeAmount("50000")
                .chargeCoverage(BudgetCoverage.ALL_MANDATORY_CHARGES)
                .budgetCheck(BudgetCheck.NOT_REQUESTED)
                .displayLabel("Tổng giá API đã gồm 5% phí dịch vụ và 10% VAT.")
                .build();

        CoreCandidateDiscoveryResult discovery = CoreCandidateDiscoveryResult.builder()
                .candidates(List.of(candidate))
                .availabilityByRoomTypeId(Map.of(1001, List.of(av1, av2)))
                .quoteByRoomTypeId(Map.of(1001, quote))
                .catalogComplete(true)
                .build();

        when(corePort.discoverAndQuoteCandidates(any(), any(), anyInt())).thenReturn(discovery);

        // Execute Release 1 search
        AdvisorResultDto advisorResult = orchestrationService.search(searchRequest, "user-session-123");

        // Verify Release 1 search operates completely normally despite RAG failure
        assertThat(advisorResult.getStatus()).isEqualTo(AdvisorStatus.RESULTS);
        assertThat(advisorResult.getRecommendations()).hasSize(1);
        assertThat(advisorResult.getRecommendations().get(0).getRoom().getHotelRoomTypeId()).isEqualTo(1001);
    }

    @Test
    @DisplayName("T12-V2-030: PublicRagKnowledgeController returns 503 SERVICE_UNAVAILABLE with fallback on RAG outage")
    void shouldReturnServiceUnavailableGracefullyInController() {
        when(failingRagService.answerQuestion(any()))
                .thenThrow(new RuntimeException("Vector store connection timeout"));

        RagQueryRequestDto request = RagQueryRequestDto.builder()
                .hotelId(1)
                .question("Chính sách hủy phòng thế nào?")
                .build();

        ResponseEntity<ApiResponse<RagQueryResultDto>> response = publicRagKnowledgeController.ask(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(503);
        assertThat(response.getBody().getMessage()).contains("RAG_TEMP_UNAVAILABLE");

        RagQueryResultDto fallback = response.getBody().getResult();
        assertThat(fallback).isNotNull();
        assertThat(fallback.getStatus()).isEqualTo(RagAnswerStatus.INSUFFICIENT_EVIDENCE);
        assertThat(fallback.getAnswer()).contains("tạm thời gián đoạn");
        assertThat(fallback.getDisclaimers()).anyMatch(d -> d.contains("đặt phòng vẫn hoạt động bình thường"));
    }
}
