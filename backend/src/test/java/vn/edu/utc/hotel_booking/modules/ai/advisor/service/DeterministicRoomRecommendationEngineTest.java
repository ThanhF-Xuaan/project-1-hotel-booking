package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.CandidateAvailability;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.RoomCandidate;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicRoomRecommendationEngineTest {

    private DeterministicRoomRecommendationEngine engine;
    private ObjectMapper mapper;

    private List<RoomCandidate> candidateList;
    private Map<Integer, List<CandidateAvailability>> availabilityMap;
    private Map<Integer, PriceQuoteDto> quoteMap;

    @BeforeEach
    void setUp() throws Exception {
        engine = new DeterministicRoomRecommendationEngine();
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Load synthetic fixtures from classpath
        candidateList = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/contracts/t12/r1/v2.0.0-draft.1/discovery-pages-proposed.json")) {
            assertThat(is).as("discovery fixture must exist").isNotNull();
            JsonNode root = mapper.readTree(is);
            for (JsonNode page : root) {
                JsonNode content = page.path("result").path("content");
                for (JsonNode rNode : content) {
                    List<RoomFeatureDto> features = new ArrayList<>();
                    for (JsonNode fNode : rNode.path("features")) {
                        features.add(new RoomFeatureDto(fNode.path("code").asText(), fNode.path("name").asText()));
                    }
                    candidateList.add(RoomCandidate.builder()
                            .hotelId(rNode.path("hotelId").asInt())
                            .hotelName(rNode.path("hotelName").asText())
                            .hotelRoomTypeId(rNode.path("hotelRoomTypeId").asInt())
                            .roomTypeId(rNode.path("roomTypeId").asInt())
                            .roomTypeCode(rNode.path("roomTypeCode").asText())
                            .roomTypeName(rNode.path("roomTypeName").asText())
                            .maxAdults(rNode.path("maxAdults").asInt())
                            .maxChildren(rNode.path("maxChildren").asInt())
                            .maxTotalGuests(rNode.path("maxTotalGuests").asInt())
                            .extraBedsAllowed(rNode.path("extraBedsAllowed").asInt())
                            .features(features)
                            .status(rNode.path("status").asText())
                            .build());
                }
            }
        }

        availabilityMap = new HashMap<>();
        try (InputStream is = getClass().getResourceAsStream("/contracts/t12/r1/v2.0.0-draft.1/availability-responses-native.json")) {
            assertThat(is).as("availability fixture must exist").isNotNull();
            JsonNode root = mapper.readTree(is);
            for (JsonNode resp : root) {
                JsonNode results = resp.path("result");
                for (JsonNode aNode : results) {
                    int hrtId = aNode.path("hotelRoomTypeId").asInt();
                    availabilityMap.computeIfAbsent(hrtId, k -> new ArrayList<>()).add(
                            CandidateAvailability.builder()
                                    .hotelRoomTypeId(hrtId)
                                    .availabilityDate(LocalDate.parse(aNode.path("availabilityDate").asText()))
                                    .totalRooms(aNode.path("totalRooms").asInt())
                                    .bookedRooms(aNode.path("bookedRooms").asInt())
                                    .lockedRooms(aNode.path("lockedRooms").asInt())
                                    .oooRooms(aNode.path("oooRooms").asInt())
                                    .availableCount(aNode.path("availableCount").asInt())
                                    .build()
                    );
                }
            }
        }

        quoteMap = new HashMap<>();
        try (InputStream is = getClass().getResourceAsStream("/contracts/t12/r1/v2.0.0-draft.1/quotes-normalized-proposed.json")) {
            assertThat(is).as("quotes fixture must exist").isNotNull();
            PriceQuoteDto[] quotes = mapper.readValue(is, PriceQuoteDto[].class);
            for (PriceQuoteDto q : quotes) {
                quoteMap.put(q.getHotelRoomTypeId(), q);
            }
        }
    }

    @Test
    @DisplayName("Should produce exact Top 5 ranking matching results-response.json for 26 synthetic candidates")
    void shouldProduceExactTop5RankingMatchingResultsResponse() {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .children(0)
                .extraBeds(0)
                .roomsCount(1)
                .requiredFeatureCodes(Collections.emptyList())
                .preferredFeatureCodes(Collections.emptyList())
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.fromString("00000000-0000-4000-8000-000000000001"),
                UUID.fromString("00000000-0000-4000-8000-000000000001"),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.RESULTS);
        assertThat(result.getCoverage().getStatus()).isEqualTo(CoverageStatus.COMPLETE);
        assertThat(result.getCoverage().getDiscoveredCount()).isEqualTo(26);
        assertThat(result.getCoverage().getEvaluatedCount()).isEqualTo(26);
        assertThat(result.getCoverage().getVerifiedMatchCount()).isEqualTo(26);

        List<RoomRecommendationDto> recs = result.getRecommendations();
        assertThat(recs).hasSize(5);

        // Assert exact Top 5 lexicographic ranking by price:
        // Rank 1: 1026 (924,000 VND)
        assertThat(recs.get(0).getRank()).isEqualTo(1);
        assertThat(recs.get(0).getRoom().getHotelRoomTypeId()).isEqualTo(1026);
        assertThat(recs.get(0).getPrice().getFinalTotalAmount()).isEqualTo("924000");

        // Rank 2: 1001 (1,155,000 VND)
        assertThat(recs.get(1).getRank()).isEqualTo(2);
        assertThat(recs.get(1).getRoom().getHotelRoomTypeId()).isEqualTo(1001);
        assertThat(recs.get(1).getPrice().getFinalTotalAmount()).isEqualTo("1155000");

        // Rank 3: 1002 (1,178,100 VND)
        assertThat(recs.get(2).getRank()).isEqualTo(3);
        assertThat(recs.get(2).getRoom().getHotelRoomTypeId()).isEqualTo(1002);
        assertThat(recs.get(2).getPrice().getFinalTotalAmount()).isEqualTo("1178100");

        // Rank 4: 1003 (1,201,200 VND)
        assertThat(recs.get(3).getRank()).isEqualTo(4);
        assertThat(recs.get(3).getRoom().getHotelRoomTypeId()).isEqualTo(1003);
        assertThat(recs.get(3).getPrice().getFinalTotalAmount()).isEqualTo("1201200");

        // Rank 5: 1004 (1,224,300 VND)
        assertThat(recs.get(4).getRank()).isEqualTo(5);
        assertThat(recs.get(4).getRoom().getHotelRoomTypeId()).isEqualTo(1004);
        assertThat(recs.get(4).getPrice().getFinalTotalAmount()).isEqualTo("1224300");

        // Verify action paths and stay window
        assertThat(recs.get(0).getAction().getPath()).isEqualTo("/portal/hotels/6/room-types/1026");
        assertThat(recs.get(0).getStay().getRoomsCount()).isEqualTo(1);
        assertThat(recs.get(0).getReasons()).anyMatch(r -> r.getCode() == ReasonCode.AVAILABLE_ALL_NIGHTS);
        assertThat(recs.get(0).getReasons())
                .filteredOn(reason -> reason.getCode() == ReasonCode.AVAILABLE_ALL_NIGHTS)
                .extracting(RecommendationReasonDto::getText)
                .containsExactly("Còn phòng trong mọi đêm lưu trú theo dữ liệu API.");
    }

    @Test
    @DisplayName("Should filter out candidates exceeding guest capacity")
    void shouldFilterOutExcessiveGuests() {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(5) // Max adults in fixture is 2
                .children(0)
                .extraBeds(0)
                .roomsCount(1)
                .requiredFeatureCodes(Collections.emptyList())
                .preferredFeatureCodes(Collections.emptyList())
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NO_MATCH);
        assertThat(result.getRecommendations()).isEmpty();
        assertThat(result.getCoverage().getVerifiedMatchCount()).isEqualTo(0);
        assertThat(result.getCoverage().getStatus()).isEqualTo(CoverageStatus.COMPLETE);
    }

    @Test
    @DisplayName("Should filter out candidate if not available on all occupied nights")
    void shouldFilterOutUnavailableNights() {
        // Tamper availability for room 1026: make night 2 unavailable
        List<CandidateAvailability> avList = availabilityMap.get(1026);
        avList.removeIf(a -> a.getAvailabilityDate().equals(LocalDate.of(2026, 11, 21)));

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .children(0)
                .extraBeds(0)
                .roomsCount(1)
                .requiredFeatureCodes(Collections.emptyList())
                .preferredFeatureCodes(Collections.emptyList())
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        // Room 1026 must be dropped, making 1001 rank 1
        assertThat(result.getRecommendations().get(0).getRoom().getHotelRoomTypeId()).isEqualTo(1001);
        assertThat(result.getRecommendations()).noneMatch(r -> r.getRoom().getHotelRoomTypeId() == 1026);
    }

    @Test
    @DisplayName("Should filter out candidates missing required features")
    void shouldFilterOutMissingRequiredFeatures() {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .children(0)
                .extraBeds(0)
                .roomsCount(1)
                .requiredFeatureCodes(List.of("OCEAN_VIEW")) // None of the synthetic rooms have this
                .preferredFeatureCodes(Collections.emptyList())
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NO_MATCH);
        assertThat(result.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("Should enforce hard budget and only return candidates that PASS")
    void shouldEnforceHardBudgetConstraint() {
        BudgetDto hardBudget = BudgetDto.builder()
                .amount("1000000") // Only 1026 has 924,000; next is 1001 at 1,155,000
                .currency("VND")
                .basis(BudgetBasis.STAY_TOTAL)
                .coverage(BudgetCoverage.ALL_MANDATORY_CHARGES)
                .hard(true)
                .build();

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .children(0)
                .extraBeds(0)
                .roomsCount(1)
                .requiredFeatureCodes(Collections.emptyList())
                .preferredFeatureCodes(Collections.emptyList())
                .budget(hardBudget)
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.RESULTS);
        assertThat(result.getRecommendations()).hasSize(1);
        assertThat(result.getRecommendations().get(0).getRoom().getHotelRoomTypeId()).isEqualTo(1026);
        assertThat(result.getRecommendations().get(0).getPrice().getBudgetCheck()).isEqualTo(BudgetCheck.PASS);
        assertThat(result.getRecommendations().get(0).getReasons()).anyMatch(r -> r.getCode() == ReasonCode.BUDGET_MATCH);
    }

    @Test
    @DisplayName("Should prioritize preferred features in PROPOSED_LEXICOGRAPHIC_V1 ranking")
    void shouldPrioritizePreferredFeatures() {
        // Add a preferred feature code 'BATHTUB' only to room 1004
        candidateList.stream()
                .filter(c -> c.getHotelRoomTypeId() == 1004)
                .findFirst()
                .ifPresent(c -> c.getFeatures().add(new RoomFeatureDto("BATHTUB", "Bồn tắm nằm")));

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .children(0)
                .extraBeds(0)
                .roomsCount(1)
                .requiredFeatureCodes(Collections.emptyList())
                .preferredFeatureCodes(List.of("BATHTUB"))
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        // Room 1004 has 1 preferred feature match -> Rank 1 despite higher price
        assertThat(result.getRecommendations().get(0).getRank()).isEqualTo(1);
        assertThat(result.getRecommendations().get(0).getRoom().getHotelRoomTypeId()).isEqualTo(1004);
        assertThat(result.getRecommendations().get(0).getReasons()).anyMatch(r -> r.getCode() == ReasonCode.PREFERRED_FEATURE_MATCH);

        // Followed by 1026 (0 preferred features, but cheapest)
        assertThat(result.getRecommendations().get(1).getRoom().getHotelRoomTypeId()).isEqualTo(1026);
    }

    @Test
    @DisplayName("Should return TEMP_UNAVAILABLE when all candidates fail upstream")
    void shouldReturnTempUnavailableWhenAllFail() {
        Set<Integer> allFailed = new HashSet<>();
        for (RoomCandidate c : candidateList) {
            allFailed.add(c.getHotelRoomTypeId());
        }

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .children(0)
                .extraBeds(0)
                .roomsCount(1)
                .requiredFeatureCodes(Collections.emptyList())
                .preferredFeatureCodes(Collections.emptyList())
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                false,
                allFailed,
                List.of(CoverageReason.PRICE_FAILED)
        );

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.TEMP_UNAVAILABLE);
        assertThat(result.getCoverage().getStatus()).isEqualTo(CoverageStatus.PARTIAL);
        assertThat(result.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("Should reject candidate when capacity fields are null")
    void shouldRejectCandidateWhenCapacityIsNull() {
        RoomCandidate nullCapacityCandidate = RoomCandidate.builder()
                .hotelId(1)
                .hotelName("Test Hotel")
                .hotelRoomTypeId(9999)
                .roomTypeCode("UNKNOWN_CAP")
                .roomTypeName("Unknown Capacity Room")
                .maxAdults(null) // null required capacity
                .maxTotalGuests(null)
                .status("ACTIVE")
                .build();

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(1)
                .roomsCount(1)
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                List.of(nullCapacityCandidate),
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NO_MATCH);
        assertThat(result.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("Should evaluate budget directly using authoritative finalTotalAmount without VAT recalculation")
    void shouldEvaluateBudgetUsingAuthoritativeFinalTotalAmountDirectly() {
        // Candidate with preTax = 800000, vat = 10% (would be 880000), but finalTotalAmount = 950000 (with service fees and surcharges)
        PriceQuoteDto quoteWithSurcharges = PriceQuoteDto.builder()
                .hotelRoomTypeId(1026)
                .source("BACKEND_PRICING")
                .quoteId("quote-1026")
                .observedAt(null)
                .totalNights(2)
                .currency("VND")
                .preTaxAmount("800000")
                .vatPercent("10")
                .vatAmount("80000")
                .finalTotalAmount("950000")
                .chargeCoverage(BudgetCoverage.BACKEND_QUOTED_TOTAL)
                .serviceFeeStatus(ServiceFeeStatus.INCLUDED)
                .build();

        Map<Integer, PriceQuoteDto> customQuoteMap = new HashMap<>(quoteMap);
        customQuoteMap.put(1026, quoteWithSurcharges);

        // Budget is 900,000 VND (less than 950,000, but greater than 880,000)
        BudgetDto hardBudget = BudgetDto.builder()
                .amount("900000")
                .currency("VND")
                .basis(BudgetBasis.STAY_TOTAL)
                .coverage(BudgetCoverage.BACKEND_QUOTED_TOTAL)
                .hard(true)
                .build();

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .budget(hardBudget)
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                customQuoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        // Room 1026 must be rejected because 950,000 > 900,000 (no recalculation to 880,000)
        assertThat(result.getRecommendations()).noneMatch(r -> r.getRoom().getHotelRoomTypeId() == 1026);
    }

    @Test
    @DisplayName("Should reject hard budget when quote currency is null (unverified)")
    void shouldRejectHardBudgetWhenCurrencyIsNull() {
        PriceQuoteDto quoteNullCurrency = PriceQuoteDto.builder()
                .hotelRoomTypeId(1026)
                .source("BACKEND_PRICING")
                .quoteId("quote-1026")
                .totalNights(2)
                .currency(null) // Unverified currency
                .finalTotalAmount("500000")
                .chargeCoverage(BudgetCoverage.BACKEND_QUOTED_TOTAL)
                .serviceFeeStatus(ServiceFeeStatus.INCLUDED)
                .build();

        Map<Integer, PriceQuoteDto> customQuoteMap = new HashMap<>(quoteMap);
        customQuoteMap.put(1026, quoteNullCurrency);

        BudgetDto hardBudget = BudgetDto.builder()
                .amount("1000000")
                .currency("VND")
                .basis(BudgetBasis.STAY_TOTAL)
                .coverage(BudgetCoverage.BACKEND_QUOTED_TOTAL)
                .hard(true)
                .build();

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .budget(hardBudget)
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                customQuoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        // Room 1026 must be rejected because currency is null -> UNVERIFIABLE
        assertThat(result.getRecommendations()).noneMatch(r -> r.getRoom().getHotelRoomTypeId() == 1026);
    }

    @Test
    @DisplayName("Should mark EACH_NIGHT budget check as UNVERIFIABLE and reject under hard budget")
    void shouldMarkEachNightBudgetAsUnverifiable() {
        BudgetDto hardBudget = BudgetDto.builder()
                .amount("1000000")
                .currency("VND")
                .basis(BudgetBasis.EACH_NIGHT) // EACH_NIGHT is unverifiable with single total
                .coverage(BudgetCoverage.ALL_MANDATORY_CHARGES)
                .hard(true)
                .build();

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .budget(hardBudget)
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NO_MATCH);
        assertThat(result.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("Should return TEMP_UNAVAILABLE and PARTIAL coverage when empty candidates list has failed price quotes")
    void shouldReturnTempUnavailableWhenEmptyCandidatesHasFailedQuotes() {
        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                Collections.emptyList(), // empty candidates
                Collections.emptyMap(),
                Collections.emptyMap(),
                true, // catalog was scanned
                Set.of(101, 102), // but all failed detailed quote
                List.of(CoverageReason.PRICE_FAILED),
                2, // totalDiscoveredCount
                0
        );

        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.TEMP_UNAVAILABLE);
        assertThat(result.getCoverage().getStatus()).isEqualTo(CoverageStatus.PARTIAL);
        assertThat(result.getCoverage().isCatalogComplete()).isFalse();
        assertThat(result.getCoverage().getFailedCount()).isEqualTo(2);
        assertThat(result.getCoverage().getDiscoveredCount()).isEqualTo(2);
        assertThat(result.getCoverage().getReasons()).contains(CoverageReason.PRICE_FAILED);
    }

    @Test
    @DisplayName("Should prove that a 924,000 VND room fails a hard stay-total budget of 900,000 VND")
    void shouldProve924000RoomFailsHardBudgetOf900000() {
        BudgetDto hardBudget = BudgetDto.builder()
                .amount("900000")
                .currency("VND")
                .basis(BudgetBasis.STAY_TOTAL)
                .coverage(BudgetCoverage.BACKEND_QUOTED_TOTAL)
                .hard(true)
                .build();

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .budget(hardBudget)
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        // Room 1026 has finalTotalAmount = 924,000 > 900,000, so it must be rejected
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NO_MATCH);
        assertThat(result.getRecommendations()).isEmpty();
        assertThat(result.getCoverage().getVerifiedMatchCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should prove that a 924,000 VND room fails an average nightly hard budget of 450,000 VND over 2 nights")
    void shouldProve924000RoomFailsAverageNightlyBudgetOf450000OverTwoNights() {
        BudgetDto hardBudget = BudgetDto.builder()
                .amount("450000")
                .currency("VND")
                .basis(BudgetBasis.AVERAGE_PER_NIGHT)
                .coverage(BudgetCoverage.BACKEND_QUOTED_TOTAL)
                .hard(true)
                .build();

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22)) // 2 nights
                .adults(2)
                .roomsCount(1)
                .budget(hardBudget)
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        // Room 1026 average per night = 924,000 / 2 = 462,000 > 450,000, so it must be rejected
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NO_MATCH);
        assertThat(result.getRecommendations()).isEmpty();
        assertThat(result.getCoverage().getVerifiedMatchCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should prove that a 924,000 VND room passes an exact stay-total budget of 924,000 VND")
    void shouldProve924000RoomPassesExactStayTotalBudgetOf924000() {
        BudgetDto exactBudget = BudgetDto.builder()
                .amount("924000")
                .currency("VND")
                .basis(BudgetBasis.STAY_TOTAL)
                .coverage(BudgetCoverage.BACKEND_QUOTED_TOTAL)
                .hard(true)
                .build();

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Synthetic City")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .budget(exactBudget)
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                quoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        // Room 1026 has finalTotalAmount = 924,000 == 924,000, passes exact boundary
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.RESULTS);
        assertThat(result.getRecommendations()).hasSize(1);
        assertThat(result.getRecommendations().get(0).getRoom().getHotelRoomTypeId()).isEqualTo(1026);
        assertThat(result.getRecommendations().get(0).getPrice().getBudgetCheck()).isEqualTo(BudgetCheck.PASS);
        assertThat(result.getRecommendations().get(0).getPrice().getFinalTotalAmount()).isEqualTo("924000");
    }

    @Test
    @DisplayName("Should prove that unverified charge coverage cannot produce a hard-budget PASS")
    void shouldProveUnverifiedCoverageCannotProduceHardBudgetPass() {
        PriceQuoteDto quoteUnverifiedCoverage = PriceQuoteDto.builder()
                .hotelRoomTypeId(1026)
                .source("BACKEND_PRICING")
                .quoteId("quote-1026")
                .totalNights(2)
                .currency("VND")
                .preTaxAmount("800000")
                .finalTotalAmount("924000")
                .chargeCoverage(null) // Unverified charge coverage
                .serviceFeeStatus(ServiceFeeStatus.UNKNOWN)
                .build();

        Map<Integer, PriceQuoteDto> customQuoteMap = new HashMap<>(quoteMap);
        customQuoteMap.put(1026, quoteUnverifiedCoverage);

        BudgetDto hardBudget = BudgetDto.builder()
                .amount("1000000")
                .currency("VND")
                .basis(BudgetBasis.STAY_TOTAL)
                .coverage(BudgetCoverage.ALL_MANDATORY_CHARGES)
                .hard(true)
                .build();

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .budget(hardBudget)
                .build();

        AdvisorResultDto result = engine.evaluateAndRank(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                criteria,
                Collections.emptyList(),
                candidateList,
                availabilityMap,
                customQuoteMap,
                true,
                Collections.emptySet(),
                Collections.emptyList()
        );

        // Room 1026 must be rejected because chargeCoverage is null -> UNVERIFIABLE
        assertThat(result.getRecommendations()).noneMatch(r -> r.getRoom().getHotelRoomTypeId() == 1026);
    }
}
