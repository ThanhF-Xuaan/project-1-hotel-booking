package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.controller.PublicUnifiedAdvisorController;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.HybridRoomAdvisorLlmService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.InMemoryRoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.HybridRagEmbeddingService;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.InMemoryScopedVectorStore;
import vn.edu.utc.hotel_booking.modules.organization.service.PublicHotelEligibilityReader;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real controller -> unified -> offline NLU/ranker/session -> scoped retrieval/grounding.
 * Only owner-owned Core/eligibility contracts use synthetic fixtures; no DB or external provider. */
class UnifiedAdvisorFlowIntegrationTest {
    private final ObjectMapper json = new ObjectMapper();
    private RoomAdvisorCorePort core;
    private ScopedRagRetrievalService retrieval;
    private MockMvc http;
    private Cookie cookie;

    @BeforeEach void setup() {
        core = mock(RoomAdvisorCorePort.class);
        var eligibility = mock(PublicHotelEligibilityReader.class);
        when(eligibility.isActivePublicHotel((short) 2)).thenReturn(true);
        var sessions = new InMemoryRoomAdvisorSessionStore();
        var validator = new RagCorpusGovernanceValidator();
        var embedding = new HybridRagEmbeddingService();
        var vectors = new InMemoryScopedVectorStore(256);
        var lifecycle = new RagKnowledgeLifecycleService(validator, embedding, vectors);
        lifecycle.ingestAndStage(RagIngestRequestDto.builder().sourceId("SYNTHETIC-POOL")
                .scopeType(RagScopeType.HOTEL_SCOPED).hotelId(2).category(RagDocumentCategory.AMENITY)
                .title("Synthetic hồ bơi").content("Hồ bơi mở cửa từ 06:00 và đóng cửa lúc 21:00 hàng ngày.")
                .effectiveFrom(LocalDate.of(2026, 1, 1)).build(), 1);
        lifecycle.activate(RagActivationRequestDto.builder().sourceId("SYNTHETIC-POOL")
                .scopeType(RagScopeType.HOTEL_SCOPED).hotelId(2).version(1).build());
        lifecycle.ingestAndStage(RagIngestRequestDto.builder().sourceId("SYNTHETIC-BREAKFAST")
                .scopeType(RagScopeType.HOTEL_SCOPED).hotelId(2).category(RagDocumentCategory.POLICY)
                .title("Synthetic chính sách buffet trẻ em")
                .content("Trẻ em dưới 6 tuổi đi cùng bố mẹ được miễn phí buffet sáng tiêu chuẩn.")
                .effectiveFrom(LocalDate.of(2026, 1, 1)).build(), 1);
        lifecycle.activate(RagActivationRequestDto.builder().sourceId("SYNTHETIC-BREAKFAST")
                .scopeType(RagScopeType.HOTEL_SCOPED).hotelId(2).version(1).build());
        retrieval = spy(new ScopedRagRetrievalService(vectors, embedding));
        var rag = new RagPublicQueryFacade(new RagGroundedAnswerService(retrieval, validator), eligibility, new RagRequestBudget());
        var rooms = new RoomAdvisorOrchestrationService(new CriteriaValidationService(),
                new DeterministicRoomRecommendationEngine(), core, sessions, new HybridRoomAdvisorLlmService());
        var unified = new UnifiedAdvisorService(new PolicyIntentClassifier(), rooms, rag, sessions);
        var clock = Clock.systemUTC();
        var tokens = new RoomAdvisorSessionTokenService("synthetic-test-secret-7f3c1a9e5b2d8460c4f19a3e7b2d5c80", null);
        http = MockMvcBuilders.standaloneSetup(new PublicUnifiedAdvisorController(unified,
                new RoomAdvisorPrincipalExtractor(tokens, clock), new RoomAdvisorSessionCookieFactory(),
                new RoomAdvisorRateLimiter(clock))).build();
        when(core.resolveDestination(any(), any(), any())).thenReturn(DestinationResolutionResult.builder().regionId((short) 2).build());
        when(core.discoverAndQuoteCandidates(any(), any(), anyInt())).thenAnswer(inv -> {
            RoomSearchCriteriaDto criteria = inv.getArgument(0);
            if (criteria == null || criteria.getCheckInDate() == null || criteria.getCheckOutDate() == null) {
                return null;
            }
            var candidate = RoomCandidate.builder().hotelId(2).hotelName("Synthetic Hotel 2")
                    .hotelRoomTypeId(201).roomTypeId(1).roomTypeCode("DOUBLE").roomTypeName("Synthetic Double")
                    .maxAdults(2).maxChildren(1).maxTotalGuests(4).extraBedsAllowed(1).status("ACTIVE")
                    .features(List.of(new RoomFeatureDto("BREAKFAST", "Synthetic breakfast"))).build();
            var availability = criteria.getCheckInDate().datesUntil(criteria.getCheckOutDate())
                    .map(day -> CandidateAvailability.builder().hotelRoomTypeId(201).availabilityDate(day).availableCount(2).build()).toList();
            var quote = PriceQuoteDto.builder().hotelRoomTypeId(201).source("SYNTHETIC_CORE")
                    .totalNights(2).currency("VND").finalTotalAmount("1200000")
                    .chargeCoverage(BudgetCoverage.BACKEND_QUOTED_TOTAL).serviceFeeStatus(ServiceFeeStatus.UNKNOWN).build();
            return CoreCandidateDiscoveryResult.builder().candidates(List.of(candidate))
                    .availabilityByRoomTypeId(Map.of(201, availability)).quoteByRoomTypeId(Map.of(201, quote)).build();
        });
    }

    private String message() {
        var start = LocalDate.now().plusDays(4);
        return "Tìm phòng ở Đà Nẵng từ " + start + " đến " + start.plusDays(2)
                + " cho 2 người lớn và 1 bé 4 tuổi có ăn sáng và cần 1 giường phụ, "
                + "ngân sách tối đa 1.500.000 VND cả kỳ theo báo giá hệ thống, hồ bơi mở cửa mấy giờ?";
    }

    private JsonNode converse(Map<String, Object> body) throws Exception {
        var request = post("/api/v1/public/ai/room-advisor/converse").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
        if (cookie != null) request.cookie(cookie);
        var response = http.perform(request).andExpect(status().isOk()).andReturn().getResponse();
        String header = response.getHeader("Set-Cookie");
        if (header != null) cookie = new Cookie(RoomAdvisorSessionCookieFactory.COOKIE_NAME,
                header.split(";", 2)[0].split("=", 2)[1]);
        return json.readTree(response.getContentAsString()).path("result");
    }

    @Test void mixedTurnAndPolicyFollowupUseRealPipelineAndPreserveCoreFacts() throws Exception {
        var first = converse(Map.of("message", message()));
        assertThat(first.path("intent").asText()).isEqualTo("SEARCH_AND_POLICY");
        assertThat(first.at("/advisor/status").asText()).isEqualTo("RESULTS");
        assertThat(first.at("/advisor/criteria/children").asInt()).isEqualTo(1);
        assertThat(first.at("/advisor/criteria/extraBeds").asInt()).isEqualTo(1);
        assertThat(first.at("/advisor/criteria/budget/amount").asText()).isEqualTo("1500000");
        assertThat(first.at("/advisor/recommendations/0/price/finalTotalAmount").asText()).isEqualTo("1200000");
        assertThat(first.at("/knowledge/result/status").asText()).isEqualTo("GROUNDED_ANSWER");
        assertThat(first.at("/knowledge/result/citations/0/hotelId").asInt()).isEqualTo(2);
        clearInvocations(core);
        var next = converse(Map.of("message", "Hồ bơi mở cửa mấy giờ?", "sessionId", first.path("sessionId").asText(),
                "expectedSessionVersion", first.path("sessionVersion").asLong()));
        assertThat(next.path("intent").asText()).isEqualTo("POLICY_ONLY");
        assertThat(next.at("/knowledge/hotelId").asInt()).isEqualTo(2);
        assertThat(next.at("/knowledge/result/status").asText()).isEqualTo("GROUNDED_ANSWER");
        verifyNoInteractions(core);
    }

    @Test void relativeWeekendSearchAndBreakfastQuestionReturnRoomsAndGroundedPolicyTogether() throws Exception {
        var result = converse(Map.of("message", "Tìm phòng đôi ở Đà Nẵng cuối tuần này cho 2 người lớn và 1 bé 4 tuổi. Bé có được miễn phí buffet sáng không?"));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate saturday = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) saturday = today.plusDays(6);

        assertThat(result.path("intent").asText()).isEqualTo("SEARCH_AND_POLICY");
        assertThat(result.at("/advisor/status").asText()).isEqualTo("RESULTS");
        assertThat(result.at("/advisor/criteria/checkInDate").asText()).isEqualTo(saturday.toString());
        assertThat(result.at("/advisor/criteria/checkOutDate").asText()).isEqualTo(saturday.plusDays(2).toString());
        assertThat(result.at("/advisor/criteria/children").asInt()).isEqualTo(1);
        assertThat(result.at("/knowledge/result/status").asText()).isEqualTo("GROUNDED_ANSWER");
        assertThat(result.at("/knowledge/result/answer").asText()).contains("miễn phí buffet sáng");
        assertThat(result.at("/knowledge/result/citations").size()).isPositive();
    }

    @Test void coreFailureStillReturnsGroundedPolicyThroughController() throws Exception {
        doThrow(new IllegalStateException("Synthetic outage")).when(core).discoverAndQuoteCandidates(any(), any(), anyInt());
        var result = converse(Map.of("message", message(), "policyHotelId", 2));
        assertThat(result.at("/advisor/status").asText()).isEqualTo("TEMP_UNAVAILABLE");
        assertThat(result.at("/advisor/recommendations").size()).isZero();
        assertThat(result.at("/knowledge/result/status").asText()).isEqualTo("GROUNDED_ANSWER");
        assertThat(result.path("sessionId").asText()).isNotBlank();
    }

    @Test void ragFailureDoesNotSuppressVerifiedRoomCard() throws Exception {
        doThrow(new IllegalStateException("Synthetic RAG outage")).when(retrieval).retrieve(any());
        var result = converse(Map.of("message", message()));
        assertThat(result.at("/advisor/status").asText()).isEqualTo("RESULTS");
        assertThat(result.at("/advisor/recommendations").size()).isEqualTo(1);
        assertThat(result.at("/knowledge/state").asText()).isEqualTo("UNAVAILABLE");
    }

    @Test void multiTurnSearchFollowedByRoomComparisonReusesSessionReadOnly() throws Exception {
        var candidate1 = RoomCandidate.builder().hotelId(2).hotelName("Synthetic Hotel 2")
                .hotelRoomTypeId(201).roomTypeId(1).roomTypeCode("DOUBLE").roomTypeName("Synthetic Double")
                .maxAdults(2).maxChildren(1).maxTotalGuests(4).extraBedsAllowed(1).status("ACTIVE")
                .features(List.of(new RoomFeatureDto("BREAKFAST", "Synthetic breakfast"))).build();
        var candidate2 = RoomCandidate.builder().hotelId(2).hotelName("Synthetic Hotel 2")
                .hotelRoomTypeId(202).roomTypeId(2).roomTypeCode("DELUXE").roomTypeName("Synthetic Deluxe")
                .maxAdults(2).maxChildren(2).maxTotalGuests(4).extraBedsAllowed(1).status("ACTIVE")
                .features(List.of(new RoomFeatureDto("BREAKFAST", "Synthetic breakfast"), new RoomFeatureDto("BALCONY", "Ban công"))).build();
        doAnswer(inv -> {
            RoomSearchCriteriaDto criteria = inv.getArgument(0);
            if (criteria == null || criteria.getCheckInDate() == null || criteria.getCheckOutDate() == null) {
                return null;
            }
            var avail1 = criteria.getCheckInDate().datesUntil(criteria.getCheckOutDate())
                    .map(day -> CandidateAvailability.builder().hotelRoomTypeId(201).availabilityDate(day).availableCount(2).build()).toList();
            var avail2 = criteria.getCheckInDate().datesUntil(criteria.getCheckOutDate())
                    .map(day -> CandidateAvailability.builder().hotelRoomTypeId(202).availabilityDate(day).availableCount(2).build()).toList();
            var quote1 = PriceQuoteDto.builder().hotelRoomTypeId(201).source("SYNTHETIC_CORE")
                    .totalNights(2).currency("VND").finalTotalAmount("1200000")
                    .chargeCoverage(BudgetCoverage.BACKEND_QUOTED_TOTAL).serviceFeeStatus(ServiceFeeStatus.UNKNOWN).build();
            var quote2 = PriceQuoteDto.builder().hotelRoomTypeId(202).source("SYNTHETIC_CORE")
                    .totalNights(2).currency("VND").finalTotalAmount("2000000")
                    .chargeCoverage(BudgetCoverage.BACKEND_QUOTED_TOTAL).serviceFeeStatus(ServiceFeeStatus.UNKNOWN).build();
            return CoreCandidateDiscoveryResult.builder().candidates(List.of(candidate1, candidate2))
                    .availabilityByRoomTypeId(Map.of(201, avail1, 202, avail2))
                    .quoteByRoomTypeId(Map.of(201, quote1, 202, quote2)).build();
        }).when(core).discoverAndQuoteCandidates(any(), any(), anyInt());

        var start = LocalDate.now().plusDays(4);
        String searchMsg = "Tìm phòng ở Đà Nẵng từ " + start + " đến " + start.plusDays(2) + " cho 2 người lớn";
        var firstTurn = converse(Map.of("message", searchMsg));

        assertThat(firstTurn.path("intent").asText()).isEqualTo("SEARCH_ONLY");
        assertThat(firstTurn.at("/advisor/recommendations").size()).isEqualTo(2);
        String sessionId = firstTurn.path("sessionId").asText();
        long sessionVersion = firstTurn.path("sessionVersion").asLong();

        clearInvocations(core);

        // Turn 2: Follow-up room comparison
        var secondTurn = converse(Map.of(
                "message", "So sánh giúp tôi hai phòng này, phòng nào tiết kiệm hơn?",
                "sessionId", sessionId,
                "expectedSessionVersion", sessionVersion
        ));

        assertThat(secondTurn.path("intent").asText()).isEqualTo("ROOM_COMPARISON");
        assertThat(secondTurn.path("sessionId").asText()).isEqualTo(sessionId);
        assertThat(secondTurn.path("sessionVersion").asLong()).isEqualTo(sessionVersion);
        assertThat(secondTurn.at("/comparison/status").asText()).isEqualTo("READY");
        assertThat(secondTurn.at("/comparison/items").size()).isEqualTo(2);
        assertThat(secondTurn.at("/comparison/maxSavingsAmount").asText()).isEqualTo("800000");
        assertThat(secondTurn.at("/comparison/tradeOffSummary").asText()).contains("Synthetic Double");

        verifyNoInteractions(core);
    }
}
