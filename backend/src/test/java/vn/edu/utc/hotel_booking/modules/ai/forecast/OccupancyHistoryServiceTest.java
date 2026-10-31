package vn.edu.utc.hotel_booking.modules.ai.forecast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample.Coverage;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch.DataMode;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryResult;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryResult.Status;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryAuthorizationPort;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryAuthorizationPort.AuthorizedHotelScope;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryPort;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.OccupancyHistoryService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OccupancyHistoryServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-29T17:00:00Z"); // Local 30/10 midnight.
    private static final LocalDate START = LocalDate.of(2026, 10, 27);
    private static final LocalDate END = LocalDate.of(2026, 10, 29);
    private static final AuthorizedHotelScope SCOPE = new AuthorizedHotelScope(1, 10);
    private OccupancyHistoryAuthorizationPort authorization;
    private OccupancyHistoryPort history;
    private OccupancyHistoryService service;

    @BeforeEach
    void setup() {
        authorization = mock(OccupancyHistoryAuthorizationPort.class);
        history = mock(OccupancyHistoryPort.class);
        when(authorization.authorize(1)).thenReturn(Optional.of(SCOPE));
        service = new OccupancyHistoryService(authorization, history, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void authorizesBeforeReadingSortsByDateAndRevalidatesBeforeReturn() {
        when(history.read(any())).thenReturn(batch(sample(END, 75, 100), sample(START, 0, 100), sample(START.plusDays(1), 80, 200)));
        var result = service.read(1, START, END);
        assertThat(result.status()).isEqualTo(Status.COMPLETE);
        assertThat(result.dataMode()).isEqualTo(DataMode.SYNTHETIC);
        assertThat(result.samples()).extracting(DailyOccupancySample::date).containsExactly(START, START.plusDays(1), END);
        assertThat(result.samples()).extracting(s -> s.occupancyPercent().orElseThrow()).containsExactly(0.0, 40.0, 75.0);
        assertThat(result.completeDays()).isEqualTo(3);
        assertThat(result.usableDays()).isEqualTo(3);
        var order = inOrder(authorization, history);
        order.verify(authorization).authorize(1);
        order.verify(history).read(new OccupancyHistoryPort.Query(SCOPE, START, END, NOW));
        order.verify(authorization).authorize(1);
        order.verifyNoMoreInteractions();
        assertThatThrownBy(() -> result.samples().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void anEmptySuccessfulBatchMeansMissingDaysNotOutage() {
        when(history.read(any())).thenReturn(batch());
        var result = service.read(1, START, END);
        assertThat(result.status()).isEqualTo(Status.PARTIAL);
        assertThat(result.samples()).hasSize(3).allSatisfy(sample -> {
            assertThat(sample.coverage()).isEqualTo(Coverage.MISSING);
            assertThat(sample.occupiedRooms()).isNull();
        });
        assertThat(result.completeDays()).isZero();
        assertThat(result.usableDays()).isZero();
    }

    @Test
    void preservesExplicitMissingAndZeroCapacityAsDifferentCoverage() {
        when(history.read(any())).thenReturn(batch(DailyOccupancySample.missing(1, START), sample(END, 0, 0)));
        var result = service.read(1, START, END);
        assertThat(result.status()).isEqualTo(Status.PARTIAL);
        assertThat(result.completeDays()).isEqualTo(1);
        assertThat(result.usableDays()).isZero();
        assertThat(result.samples().getLast().capacityRooms()).isZero();
        assertThat(result.samples().getFirst().capacityRooms()).isNull();
    }

    @Test
    void neverRelabelsSourceProvenance() {
        when(history.read(any())).thenReturn(new OccupancyHistoryBatch(DataMode.AUTHORITATIVE, List.of(sample(START, 1, 2))));
        assertThat(service.read(1, START, END).dataMode()).isEqualTo(DataMode.AUTHORITATIVE);
    }

    @Test
    void deniedAuthorizationNeverReadsHistory() {
        when(authorization.authorize(1)).thenReturn(Optional.empty());
        assertFailure(service.read(1, START, END), Status.ACCESS_DENIED);
        verifyNoInteractions(history);
    }

    @Test
    void authorizationFailureAndNullAreUnavailableNotEmptyHistory() {
        when(authorization.authorize(1)).thenThrow(new IllegalStateException("SENSITIVE_SOURCE_MESSAGE"));
        assertFailure(service.read(1, START, END), Status.DEPENDENCY_UNAVAILABLE);
        doReturn(null).when(authorization).authorize(1);
        assertFailure(service.read(1, START, END), Status.DEPENDENCY_UNAVAILABLE);
        verifyNoInteractions(history);
    }

    @Test
    void wrongAuthorizedHotelCannotReadAnotherHotel() {
        when(authorization.authorize(1)).thenReturn(Optional.of(new AuthorizedHotelScope(2, 10)));
        assertFailure(service.read(1, START, END), Status.DEPENDENCY_UNAVAILABLE);
        verifyNoInteractions(history);
    }

    @Test
    void revokedAuthorizationDiscardsAlreadyReadSamples() {
        when(authorization.authorize(1)).thenReturn(Optional.of(SCOPE), Optional.empty());
        when(history.read(any())).thenReturn(batch(sample(START, 1, 2)));
        assertFailure(service.read(1, START, END), Status.ACCESS_DENIED);
        verify(history).read(any());
    }

    @Test
    void changedRegionDiscardsAlreadyReadSamples() {
        when(authorization.authorize(1)).thenReturn(Optional.of(SCOPE), Optional.of(new AuthorizedHotelScope(1, 11)));
        when(history.read(any())).thenReturn(batch(sample(START, 1, 2)));
        assertFailure(service.read(1, START, END), Status.ACCESS_DENIED);
    }

    @Test
    void finalAuthorizationOutageDiscardsAlreadyReadSamples() {
        when(authorization.authorize(1)).thenReturn(Optional.of(SCOPE)).thenThrow(new IllegalStateException("SENSITIVE_SOURCE_MESSAGE"));
        when(history.read(any())).thenReturn(batch(sample(START, 1, 2)));
        assertFailure(service.read(1, START, END), Status.DEPENDENCY_UNAVAILABLE);
    }

    @Test
    void historyNullOrExceptionNeverBecomesMissingData() {
        when(history.read(any())).thenReturn(null);
        assertFailure(service.read(1, START, END), Status.DEPENDENCY_UNAVAILABLE);
        when(history.read(any())).thenThrow(new IllegalStateException("SENSITIVE_SOURCE_MESSAGE"));
        assertFailure(service.read(1, START, END), Status.DEPENDENCY_UNAVAILABLE);
    }

    static Stream<Arguments> invalidBatches() {
        return Stream.of(
                Arguments.of(List.of(new DailyOccupancySample(2, START, 1, 2, Coverage.COMPLETE))),
                Arguments.of(List.of(sample(START, 1, 2), sample(START, 2, 2))),
                Arguments.of(List.of(sample(START.minusDays(1), 1, 2))),
                Arguments.of(List.of(sample(END.plusDays(1), 1, 2))),
                Arguments.of(List.of(sample(START, 1, 2), sample(START.plusDays(1), 1, 2), sample(END, 1, 2), sample(END.plusDays(1), 1, 2)))
        );
    }

    @ParameterizedTest
    @MethodSource("invalidBatches")
    void rejectsCrossHotelDuplicateOutOfRangeAndOversizedBatches(List<DailyOccupancySample> samples) {
        when(history.read(any())).thenReturn(new OccupancyHistoryBatch(DataMode.SYNTHETIC, samples));
        assertFailure(service.read(1, START, END), Status.DEPENDENCY_UNAVAILABLE);
        verify(authorization, times(1)).authorize(1);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(null, START, END), Arguments.of(0, START, END), Arguments.of(-1, START, END),
                Arguments.of(32768, START, END), Arguments.of(1, null, END), Arguments.of(1, START, null),
                Arguments.of(1, END, START), Arguments.of(1, END.plusDays(1), END.plusDays(1)),
                Arguments.of(1, START, END.plusDays(2)), Arguments.of(1, END.minusDays(420), END)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidRequestsBeforeAnyDependencyCall(Integer hotel, LocalDate start, LocalDate end) {
        assertFailure(service.read(hotel, start, end), Status.INVALID_REQUEST);
        verifyNoInteractions(history, authorization);
    }

    @Test
    void acceptsExactly420CompletedDaysWithoutIncreasingQuerySize() {
        when(history.read(any())).thenReturn(batch());
        var start = END.minusDays(419);
        var result = service.read(1, start, END);
        assertThat(result.status()).isEqualTo(Status.PARTIAL);
        assertThat(result.samples()).hasSize(420);
        verify(history).read(new OccupancyHistoryPort.Query(SCOPE, start, END, NOW));
    }

    @Test
    void trimsTodayAtVietnamMidnightAndUsesOneClockInstant() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW, NOW.plusSeconds(86400));
        service = new OccupancyHistoryService(authorization, history, clock);
        when(history.read(any())).thenAnswer(call -> {
            OccupancyHistoryPort.Query query = call.getArgument(0);
            assertThat(query.fromUtcInclusive()).isEqualTo(Instant.parse("2026-10-28T17:00:00Z"));
            assertThat(query.toUtcExclusive()).isEqualTo(NOW);
            return batch(sample(END, 1, 2));
        });
        var result = service.read(1, END, END.plusDays(1));
        assertThat(result.status()).isEqualTo(Status.COMPLETE);
        assertThat(result.toInclusive()).isEqualTo(END);
        assertThat(result.asOf()).isEqualTo(NOW);
        verify(clock, times(1)).instant();
        verifyNoMoreInteractions(clock);
    }

    @Test
    void usesVietnamDateEvenWhenClockZoneDiffers() {
        service = new OccupancyHistoryService(authorization, history,
                Clock.fixed(Instant.parse("2026-10-29T16:59:59Z"), ZoneOffset.ofHours(-10)));
        when(history.read(any())).thenReturn(batch());
        var result = service.read(1, START, END);
        assertThat(result.toInclusive()).isEqualTo(END.minusDays(1));
        assertThat(result.samples()).hasSize(2);
        verify(history).read(new OccupancyHistoryPort.Query(SCOPE, START, END.minusDays(1), Instant.parse("2026-10-29T16:59:59Z")));
    }

    @Test
    void queryRejectsIncompleteOversizedReversedOrUnscopedWindows() {
        assertThatThrownBy(() -> new OccupancyHistoryPort.Query(null, START, END, NOW)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OccupancyHistoryPort.Query(SCOPE, START, END.plusDays(1), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OccupancyHistoryPort.Query(SCOPE, END.minusDays(420), END, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OccupancyHistoryPort.Query(SCOPE, END, START, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    private static DailyOccupancySample sample(LocalDate date, int occupied, int capacity) {
        return new DailyOccupancySample(1, date, occupied, capacity, Coverage.COMPLETE);
    }

    private static OccupancyHistoryBatch batch(DailyOccupancySample... samples) {
        return new OccupancyHistoryBatch(DataMode.SYNTHETIC, List.of(samples));
    }

    private static void assertFailure(OccupancyHistoryResult result, Status status) {
        assertThat(result.status()).isEqualTo(status);
        assertThat(result.samples()).isEmpty();
        assertThat(result.dataMode()).isNull();
        assertThat(result.toString()).doesNotContain("SENSITIVE_SOURCE_MESSAGE");
    }
}
