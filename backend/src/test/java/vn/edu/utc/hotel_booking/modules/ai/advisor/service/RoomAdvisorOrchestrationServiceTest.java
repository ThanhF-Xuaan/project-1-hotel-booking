package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.CoreCandidateDiscoveryResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.DestinationResolutionResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.RoomAdvisorCorePort;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CoverageStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.RoomAdvisorLlmPort;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.CandidateAvailability;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.RoomCandidate;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.InMemoryRoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSessionStore;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomAdvisorOrchestrationServiceTest {

    private CriteriaValidationService criteriaValidationService;
    private DeterministicRoomRecommendationEngine recommendationEngine;
    private RoomAdvisorSessionStore sessionStore;

    @Mock
    private RoomAdvisorCorePort corePort;

    @Mock
    private RoomAdvisorLlmPort llmPort;

    private RoomAdvisorOrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        criteriaValidationService = new CriteriaValidationService();
        recommendationEngine = new DeterministicRoomRecommendationEngine();
        sessionStore = new InMemoryRoomAdvisorSessionStore();

        orchestrationService = new RoomAdvisorOrchestrationService(
                criteriaValidationService,
                recommendationEngine,
                corePort,
                sessionStore,
                llmPort
        );
    }

    @Test
    @DisplayName("Should return OUT_OF_SCOPE when user requests payment/booking writes")
    void shouldReturnOutOfScope() {
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tôi muốn đặt phòng này và thanh toán luôn")
                .build();

        AdvisorResultDto result = orchestrationService.chat(request, "user-123");

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.OUT_OF_SCOPE);
        assertThat(result.getCoverage().getStatus()).isEqualTo(CoverageStatus.NOT_RUN);
        assertThat(result.getRecommendations()).isEmpty();
        verifyNoInteractions(corePort);
    }

    @Test
    @DisplayName("Should return NEED_INFO when criteria has missing fields")
    void shouldReturnNeedInfoWhenCriteriaMissing() {
        AdvisorChatRequestDto request = AdvisorChatRequestDto.builder()
                .contractVersion("2.0")
                .message("Tìm phòng giúp tôi")
                .build();

        when(llmPort.extractCriteriaPatch(any(), any()))
                .thenReturn(null);

        AdvisorResultDto result = orchestrationService.chat(request, "user-123");

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NEED_INFO);
        assertThat(result.getMissingFields()).isNotEmpty();
        assertThat(result.getRecommendations()).isEmpty();
        verifyNoInteractions(corePort);
    }

    @Test
    @DisplayName("Should execute search and return RESULTS with recommendation cards")
    void shouldExecuteSearchAndReturnResults() {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .build();

        AdvisorSearchRequestDto request = AdvisorSearchRequestDto.builder()
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
                .serviceFeeStatus(vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ServiceFeeStatus.INCLUDED)
                .serviceFeeAmount("50000")
                .chargeCoverage(vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCoverage.ALL_MANDATORY_CHARGES)
                .budgetCheck(vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCheck.NOT_REQUESTED)
                .displayLabel("Tổng giá API đã gồm 5% phí dịch vụ và 10% VAT.")
                .build();

        CoreCandidateDiscoveryResult discovery = CoreCandidateDiscoveryResult.builder()
                .candidates(List.of(candidate))
                .availabilityByRoomTypeId(Map.of(1001, List.of(av1, av2)))
                .quoteByRoomTypeId(Map.of(1001, quote))
                .catalogComplete(true)
                .build();

        when(corePort.discoverAndQuoteCandidates(any(), any(), anyInt())).thenReturn(discovery);

        AdvisorResultDto result = orchestrationService.search(request, "user-123");

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.RESULTS);
        assertThat(result.getRecommendations()).hasSize(1);
        assertThat(result.getRecommendations().get(0).getRoom().getHotelRoomTypeId()).isEqualTo(1001);
    }

    @Test
    @DisplayName("Should refresh session criteria and increment sessionVersion")
    void shouldRefreshSession() {
        // First establish session with search
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .hotelId(1)
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .build();

        AdvisorSearchRequestDto sReq = AdvisorSearchRequestDto.builder()
                .contractVersion("2.0")
                .criteria(criteria)
                .build();

        when(corePort.resolveDestination(any(), any(), any()))
                .thenReturn(DestinationResolutionResult.builder().hotelId(1).build());

        when(corePort.discoverAndQuoteCandidates(any(), any(), anyInt()))
                .thenReturn(CoreCandidateDiscoveryResult.builder().catalogComplete(true).build());

        AdvisorResultDto initial = orchestrationService.search(sReq, "user-123");
        UUID sid = initial.getSessionId();
        long v1 = initial.getSessionVersion();

        // Now refresh
        AdvisorRefreshRequestDto rReq = AdvisorRefreshRequestDto.builder()
                .contractVersion("2.0")
                .sessionId(sid)
                .expectedSessionVersion(v1)
                .build();

        AdvisorResultDto refreshed = orchestrationService.refresh(rReq, "user-123");

        assertThat(refreshed.getSessionId()).isEqualTo(sid);
        assertThat(refreshed.getSessionVersion()).isEqualTo(v1 + 1);
    }
}
