package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagCitationDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagPublicQueryFacade;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagPublicQueryOutcome;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomComparisonServiceTest {

    @Mock
    private RagPublicQueryFacade ragFacade;

    private RoomComparisonService comparisonService;

    @BeforeEach
    void setUp() {
        comparisonService = new RoomComparisonService(ragFacade);
    }

    @Test
    @DisplayName("Computes exact BigDecimal deltas, shared vs unique features, queries RAG once per hotelId, and generates FAMILY_KIDS advice")
    void buildsFullComparisonMatrixAndQueriesRagOncePerHotel() {
        RoomRecommendationDto superior = buildRec(
                "rec-1", 101, 1, "Viettel Luxury Hà Nội", "SUP", "Superior Room",
                "3000000", "VND", 2, 2, 0, 2, 0,
                List.of(feature("WIFI", "Wi-Fi tốc độ cao"), feature("TV", "Smart TV"))
        );
        RoomRecommendationDto deluxe = buildRec(
                "rec-2", 102, 1, "Viettel Luxury Hà Nội", "DLX", "Deluxe Room",
                "4000000", "VND", 2, 2, 1, 3, 1,
                List.of(feature("WIFI", "Wi-Fi tốc độ cao"), feature("TV", "Smart TV"), feature("BALCONY", "Ban công"))
        );
        RoomRecommendationDto executive = buildRec(
                "rec-3", 103, 1, "Viettel Luxury Hà Nội", "EXE", "Executive Suite",
                "6000000", "VND", 2, 2, 2, 4, 2,
                List.of(feature("WIFI", "Wi-Fi tốc độ cao"), feature("TV", "Smart TV"), feature("BATHTUB", "Bồn tắm nằm"))
        );

        AdvisorResultDto advisorResult = AdvisorResultDto.builder()
                .status(AdvisorStatus.RESULTS)
                .recommendations(List.of(superior, deluxe, executive))
                .build();

        RagCitationDto staticCitation = RagCitationDto.builder()
                .sourceId("ROOM-DESC-HN01")
                .category(RagDocumentCategory.STATIC_ROOM_DESCRIPTION)
                .hotelId(1)
                .chunkId(UUID.randomUUID())
                .build();
        RagQueryResultDto ragResult = RagQueryResultDto.builder()
                .status(RagAnswerStatus.GROUNDED_ANSWER)
                .answer("[DLX] Deluxe: Diện tích 38m², có sofa.\n[EXE] Executive Suite: Diện tích 58m², phòng khách riêng và bồn tắm nằm.")
                .citations(List.of(staticCitation))
                .build();
        when(ragFacade.ask(anyString(), eq(1))).thenReturn(RagPublicQueryOutcome.answer(ragResult));

        RoomComparisonMatrixDto matrix = comparisonService.compareRooms(
                advisorResult,
                List.of("deluxe", "executive"),
                ComparisonFocus.FAMILY_KIDS,
                null
        );

        assertThat(matrix.getStatus()).isEqualTo(ComparisonStatus.READY);
        assertThat(matrix.getFocus()).isEqualTo(ComparisonFocus.FAMILY_KIDS);
        assertThat(matrix.getItems()).hasSize(2);
        assertThat(matrix.getSharedFeatureNames()).containsExactly("Wi-Fi tốc độ cao", "Smart TV");
        assertThat(matrix.getMaxSavingsAmount()).isEqualTo("2000000");
        assertThat(matrix.getMaxSavingsPercent()).isEqualByComparingTo("33.3");

        RoomComparisonItemDto dlxItem = matrix.getItems().get(0);
        RoomComparisonItemDto exeItem = matrix.getItems().get(1);

        assertThat(dlxItem.getRoomTypeCode()).isEqualTo("DLX");
        assertThat(dlxItem.getQuoteId()).isEqualTo("quote-rec-2");
        assertThat(dlxItem.getObservedAt()).isNotNull();
        assertThat(dlxItem.getBudgetCheck()).isEqualTo(BudgetCheck.PASS);
        assertThat(dlxItem.getPriceDeltaFromCheapest()).isEqualTo("0");
        assertThat(dlxItem.getPriceDiffPercentFromCheapest()).isEqualByComparingTo("0.0");
        assertThat(dlxItem.getPerNightAverageAmount()).isEqualTo("2000000");
        assertThat(dlxItem.getExtraBedsAllowed()).isEqualTo(1);
        assertThat(dlxItem.getUniqueFeatureNames()).containsExactly("Ban công");
        assertThat(dlxItem.getStaticAreaAndLayout()).contains("38m²");

        assertThat(exeItem.getRoomTypeCode()).isEqualTo("EXE");
        assertThat(exeItem.getQuoteId()).isEqualTo("quote-rec-3");
        assertThat(exeItem.getPriceDeltaFromCheapest()).isEqualTo("2000000");
        assertThat(exeItem.getPriceDiffPercentFromCheapest()).isEqualByComparingTo("50.0");
        assertThat(exeItem.getExtraBedsAllowed()).isEqualTo(2);
        assertThat(exeItem.getUniqueFeatureNames()).containsExactly("Bồn tắm nằm");
        assertThat(exeItem.getStaticAreaAndLayout()).contains("58m²");

        assertThat(matrix.getTradeOffSummary())
                .contains("Executive Suite")
                .contains("Deluxe Room")
                .contains("58m²")
                .contains("2.000.000 VND");

        // Called strictly ONCE for hotelId=1 regardless of comparing 2-3 rooms of hotelId=1
        verify(ragFacade, times(1)).ask(anyString(), eq(1));
    }

    @Test
    @DisplayName("Safely skips price delta subtraction when currencies differ or cheapest amount is zero")
    void guardsCurrencyMismatchAndZeroDivision() {
        RoomRecommendationDto roomA = buildRec(
                "rec-1", 101, 1, "Hotel A", "STD", "Standard",
                "0", "VND", 2, 2, 0, 2, 0, List.of()
        );
        RoomRecommendationDto roomB = buildRec(
                "rec-2", 102, 1, "Hotel A", "DLX", "Deluxe",
                "1000000", "VND", 2, 2, 1, 3, 1, List.of()
        );
        when(ragFacade.ask(anyString(), eq(1))).thenReturn(RagPublicQueryOutcome.rateLimited(10));

        RoomComparisonMatrixDto zeroPriceMatrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(roomA, roomB)).build(),
                List.of(),
                ComparisonFocus.BUDGET_SAVER,
                null
        );

        assertThat(zeroPriceMatrix.getStatus()).isEqualTo(ComparisonStatus.READY);
        assertThat(zeroPriceMatrix.getItems().get(1).getPriceDiffPercentFromCheapest()).isNull();

        RoomRecommendationDto usdRoom = buildRec(
                "rec-3", 103, 2, "Hotel B", "EXE", "Executive",
                "200", "USD", 2, 2, 2, 4, 1, List.of()
        );
        RoomComparisonMatrixDto currencyMismatch = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(roomB, usdRoom)).build(),
                List.of(),
                ComparisonFocus.GENERAL,
                List.of(102, 103)
        );

        assertThat(currencyMismatch.getStatus()).isEqualTo(ComparisonStatus.READY);
        assertThat(currencyMismatch.getMaxSavingsAmount()).isNull();
        assertThat(currencyMismatch.getItems().get(0).getPriceDeltaFromCheapest()).isNull();
    }

    @Test
    @DisplayName("Returns NEED_SEARCH_FIRST when session has no recommendations")
    void returnsNeedSearchFirstWhenEmpty() {
        RoomComparisonMatrixDto matrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().status(AdvisorStatus.NEED_INFO).recommendations(List.of()).build(),
                List.of("deluxe", "executive"),
                ComparisonFocus.GENERAL,
                null
        );

        assertThat(matrix.getStatus()).isEqualTo(ComparisonStatus.NEED_SEARCH_FIRST);
        verify(ragFacade, never()).ask(anyString(), any());
    }

    @Test
    @DisplayName("Returns INSUFFICIENT_CANDIDATES when only one recommendation is available")
    void returnsInsufficientCandidatesWhenOnlyOneRecommendation() {
        RoomRecommendationDto single = buildRec(
                "rec-1", 101, 1, "Hotel A", "DLX", "Deluxe",
                "1000000", "VND", 2, 2, 0, 2, 0, List.of()
        );
        RoomComparisonMatrixDto matrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().status(AdvisorStatus.RESULTS).recommendations(List.of(single)).build(),
                List.of(),
                ComparisonFocus.GENERAL,
                null
        );

        assertThat(matrix.getStatus()).isEqualTo(ComparisonStatus.INSUFFICIENT_CANDIDATES);
        assertThat(matrix.getTradeOffSummary()).contains("Hiện chỉ có 1 hạng phòng phù hợp");
        verify(ragFacade, never()).ask(anyString(), any());
    }

    @Test
    @DisplayName("Generates specific trade-off summaries for BUDGET_SAVER, SPACE_COMFORT, WORK_TRIP, and GENERAL")
    void generatesSpecificTradeOffSummariesForFocusTypes() {
        RoomRecommendationDto std = buildRec(
                "rec-1", 101, 1, "Hotel A", "STD", "Standard Room",
                "2000000", "VND", 2, 2, 0, 2, 0, List.of()
        );
        RoomRecommendationDto suite = buildRec(
                "rec-2", 102, 1, "Hotel A", "STE", "Suite Room",
                "4000000", "VND", 2, 2, 2, 4, 1, List.of(feature("DESK", "Bàn làm việc"))
        );
        when(ragFacade.ask(anyString(), eq(1))).thenReturn(RagPublicQueryOutcome.rateLimited(5));

        // BUDGET_SAVER
        RoomComparisonMatrixDto budgetMatrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(std, suite)).build(),
                List.of(),
                ComparisonFocus.BUDGET_SAVER,
                null
        );
        assertThat(budgetMatrix.getTradeOffSummary())
                .contains("Để tối ưu ngân sách")
                .contains("Standard Room");

        // SPACE_COMFORT
        RoomComparisonMatrixDto comfortMatrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(std, suite)).build(),
                List.of(),
                ComparisonFocus.SPACE_COMFORT,
                null
        );
        assertThat(comfortMatrix.getTradeOffSummary())
                .contains("ưu tiên trải nghiệm nghỉ dưỡng")
                .contains("Suite Room");

        // WORK_TRIP
        RoomComparisonMatrixDto workMatrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(std, suite)).build(),
                List.of(),
                ComparisonFocus.WORK_TRIP,
                null
        );
        assertThat(workMatrix.getTradeOffSummary())
                .contains("tiếp khách hoặc làm việc")
                .contains("Suite Room");

        // GENERAL
        RoomComparisonMatrixDto generalMatrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(std, suite)).build(),
                List.of(),
                ComparisonFocus.GENERAL,
                null
        );
        assertThat(generalMatrix.getTradeOffSummary())
                .contains("nổi bật về không gian & tiện nghi")
                .contains("kinh tế hơn");
    }

    @Test
    @DisplayName("Handles RAG exception gracefully by proceeding with Core facts and null static description")
    void handlesRagExceptionGracefully() {
        RoomRecommendationDto roomA = buildRec(
                "rec-1", 101, 1, "Hotel A", "STD", "Standard Room",
                "2000000", "VND", 2, 2, 0, 2, 0, List.of()
        );
        RoomRecommendationDto roomB = buildRec(
                "rec-2", 102, 1, "Hotel A", "DLX", "Deluxe Room",
                "3000000", "VND", 2, 2, 1, 3, 1, List.of()
        );
        when(ragFacade.ask(anyString(), eq(1))).thenThrow(new RuntimeException("Simulated RAG connection error"));

        RoomComparisonMatrixDto matrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(roomA, roomB)).build(),
                List.of(),
                ComparisonFocus.GENERAL,
                null
        );

        assertThat(matrix.getStatus()).isEqualTo(ComparisonStatus.READY);
        assertThat(matrix.getItems()).hasSize(2);
        assertThat(matrix.getItems().get(0).getStaticAreaAndLayout()).isNull();
        assertThat(matrix.getItems().get(1).getStaticAreaAndLayout()).isNull();
        assertThat(matrix.getMaxSavingsAmount()).isEqualTo("1000000");
    }

    @Test
    @DisplayName("Explicit compareRoomTypeIds selects exact matching rooms and respects MAX_COMPARISON_ROOMS = 3")
    void explicitRoomTypeIdsSelection() {
        RoomRecommendationDto r1 = buildRec("rec-1", 101, 1, "Hotel A", "SUP", "Superior", "1000000", "VND", 1, 2, 0, 2, 0, List.of());
        RoomRecommendationDto r2 = buildRec("rec-2", 102, 1, "Hotel A", "DLX", "Deluxe", "2000000", "VND", 1, 2, 0, 2, 0, List.of());
        RoomRecommendationDto r3 = buildRec("rec-3", 103, 1, "Hotel A", "EXE", "Executive", "3000000", "VND", 1, 2, 0, 2, 0, List.of());
        RoomRecommendationDto r4 = buildRec("rec-4", 104, 1, "Hotel A", "STE", "Suite", "4000000", "VND", 1, 2, 0, 2, 0, List.of());

        when(ragFacade.ask(anyString(), eq(1))).thenReturn(RagPublicQueryOutcome.rateLimited(5));

        RoomComparisonMatrixDto matrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(r1, r2, r3, r4)).build(),
                List.of(),
                ComparisonFocus.GENERAL,
                List.of(104, 101, 103)
        );

        assertThat(matrix.getStatus()).isEqualTo(ComparisonStatus.READY);
        assertThat(matrix.getItems()).hasSize(3);
        assertThat(matrix.getItems().get(0).getHotelRoomTypeId()).isEqualTo(104);
        assertThat(matrix.getItems().get(1).getHotelRoomTypeId()).isEqualTo(101);
        assertThat(matrix.getItems().get(2).getHotelRoomTypeId()).isEqualTo(103);
    }

    @Test
    @DisplayName("Scales perNightAverageAmount with 2 decimal places for USD currency")
    void handlesNonVndCurrencyScaling() {
        RoomRecommendationDto r1 = buildRec("rec-1", 101, 1, "Hotel A", "STD", "Standard", "150.50", "USD", 3, 2, 0, 2, 0, List.of());
        RoomRecommendationDto r2 = buildRec("rec-2", 102, 1, "Hotel A", "DLX", "Deluxe", "240.00", "USD", 3, 2, 0, 2, 0, List.of());

        when(ragFacade.ask(anyString(), eq(1))).thenReturn(RagPublicQueryOutcome.rateLimited(5));

        RoomComparisonMatrixDto matrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(r1, r2)).build(),
                List.of(),
                ComparisonFocus.GENERAL,
                null
        );

        assertThat(matrix.getStatus()).isEqualTo(ComparisonStatus.READY);
        assertThat(matrix.getItems().get(0).getPerNightAverageAmount()).isEqualTo("50.17");
        assertThat(matrix.getItems().get(1).getPerNightAverageAmount()).isEqualTo("80.00");
        assertThat(matrix.getMaxSavingsAmount()).isEqualTo("89.50");
    }

    @Test
    @DisplayName("Suite and Executive hints resolve both rooms correctly without greedy match drop")
    void suiteAndExecutiveHintsResolveBothRoomsCorrectlyWithoutGreedyDrop() {
        RoomRecommendationDto deluxe = buildRec(
                "rec-1", 101, 1, "Hotel A", "DLX", "Deluxe Room",
                "2500000", "VND", 2, 2, 0, 2, 0, List.of()
        );
        RoomRecommendationDto exeSuite = buildRec(
                "rec-2", 102, 1, "Hotel A", "EXE", "Executive Suite",
                "5000000", "VND", 2, 2, 1, 3, 1, List.of()
        );
        RoomRecommendationDto suite = buildRec(
                "rec-3", 103, 1, "Hotel A", "STE", "Suite Room",
                "4000000", "VND", 2, 2, 1, 3, 1, List.of()
        );

        // available list has [Deluxe, Executive Suite, Suite]
        RoomComparisonMatrixDto matrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(deluxe, exeSuite, suite)).build(),
                List.of("suite", "executive"),
                ComparisonFocus.GENERAL,
                null
        );

        assertThat(matrix.getStatus()).isEqualTo(ComparisonStatus.READY);
        assertThat(matrix.getItems()).hasSize(2);
        List<Integer> chosenIds = matrix.getItems().stream()
                .map(RoomComparisonItemDto::getHotelRoomTypeId)
                .toList();
        assertThat(chosenIds).containsExactlyInAnyOrder(102, 103);
    }

    @Test
    @DisplayName("Two-pass static description extraction prevents Executive Suite from matching [STE] line")
    void twoPassStaticDescriptionExtractsCorrectCodeLinesWithoutCrossRoomNameContamination() {
        RoomRecommendationDto suite = buildRec(
                "rec-1", 101, 1, "Hotel A", "STE", "Suite",
                "4000000", "VND", 2, 2, 0, 2, 0, List.of()
        );
        RoomRecommendationDto exeSuite = buildRec(
                "rec-2", 102, 1, "Hotel A", "EXE", "Executive Suite",
                "6000000", "VND", 2, 2, 1, 3, 1, List.of()
        );

        String ragDoc = "[STE] Suite: 45m2, ban công nhìn ra hồ.\n[EXE] Executive Suite: 65m2, phòng khách riêng và bồn tắm sục.";
        RagCitationDto staticCitation = RagCitationDto.builder()
                .sourceId("ROOM-DESC-HN01")
                .category(RagDocumentCategory.STATIC_ROOM_DESCRIPTION)
                .hotelId(1)
                .chunkId(UUID.randomUUID())
                .build();
        RagQueryResultDto ragResult = RagQueryResultDto.builder()
                .status(RagAnswerStatus.GROUNDED_ANSWER)
                .answer(ragDoc)
                .citations(List.of(staticCitation))
                .build();
        when(ragFacade.ask(anyString(), eq(1))).thenReturn(
                vn.edu.utc.hotel_booking.modules.ai.rag.service.RagPublicQueryOutcome.answer(ragResult)
        );

        RoomComparisonMatrixDto matrix = comparisonService.compareRooms(
                AdvisorResultDto.builder().recommendations(List.of(suite, exeSuite)).build(),
                List.of(),
                ComparisonFocus.GENERAL,
                null
        );

        assertThat(matrix.getStatus()).isEqualTo(ComparisonStatus.READY);
        var itemSuite = matrix.getItems().stream()
                .filter(i -> Integer.valueOf(101).equals(i.getHotelRoomTypeId())).findFirst().orElseThrow();
        var itemExe = matrix.getItems().stream()
                .filter(i -> Integer.valueOf(102).equals(i.getHotelRoomTypeId())).findFirst().orElseThrow();

        assertThat(itemSuite.getStaticAreaAndLayout()).isEqualTo("45m2, ban công nhìn ra hồ.");
        assertThat(itemExe.getStaticAreaAndLayout()).isEqualTo("65m2, phòng khách riêng và bồn tắm sục.");
    }

    private static RoomRecommendationDto buildRec(
            String recId,
            int hrtId,
            int hotelId,
            String hotelName,
            String code,
            String name,
            String totalAmount,
            String currency,
            int nights,
            int maxAdults,
            int maxChildren,
            int maxTotal,
            int extraBeds,
            List<RoomFeatureDto> features
    ) {
        return RoomRecommendationDto.builder()
                .recommendationId(recId)
                .room(RoomSummaryDto.builder()
                        .hotelId(hotelId)
                        .hotelName(hotelName)
                        .hotelRoomTypeId(hrtId)
                        .roomTypeCode(code)
                        .roomTypeName(name)
                        .maxAdults(maxAdults)
                        .maxChildren(maxChildren)
                        .maxTotalGuests(maxTotal)
                        .extraBedsAllowed(extraBeds)
                        .features(features)
                        .build())
                .price(PriceQuoteDto.builder()
                        .hotelRoomTypeId(hrtId)
                        .quoteId("quote-" + recId)
                        .observedAt(java.time.OffsetDateTime.parse("2026-10-20T12:00:00Z"))
                        .finalTotalAmount(totalAmount)
                        .currency(currency)
                        .totalNights(nights)
                        .chargeCoverage(BudgetCoverage.ALL_MANDATORY_CHARGES)
                        .serviceFeeStatus(ServiceFeeStatus.INCLUDED)
                        .budgetCheck(BudgetCheck.PASS)
                        .build())
                .action(RecommendationActionDto.builder()
                        .path("/hotels/" + hotelId + "/rooms/" + hrtId)
                        .build())
                .build();
    }

    private static RoomFeatureDto feature(String code, String name) {
        return RoomFeatureDto.builder().code(code).name(name).build();
    }
}
