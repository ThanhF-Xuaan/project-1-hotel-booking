package vn.edu.utc.hotel_booking.modules.ai.forecast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.UtilityAnomalyResponse;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.UtilityAnomalyResponse.*;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample.*;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingAuthorizationPort;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingAuthorizationPort.Scope;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingHistoryPort;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingHistoryPort.*;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.UtilityAnomalyDetectionService;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UtilityAnomalyDetectionServiceTest {
    static final LocalDate FROM = LocalDate.of(2026, 9, 19), END = FROM.plusDays(41);
    static final Instant NOW = Instant.parse("2026-10-30T17:00:00Z");
    static final Scope SCOPE = new Scope(1, 10, 11, Unit.KWH);
    UtilityReadingAuthorizationPort authorization;
    UtilityReadingHistoryPort history;
    UtilityAnomalyDetectionService service;

    @BeforeEach
    void setup() {
        authorization = mock(UtilityReadingAuthorizationPort.class); history = mock(UtilityReadingHistoryPort.class);
        when(authorization.authorize(1, 11)).thenReturn(Optional.of(SCOPE));
        service = new UtilityAnomalyDetectionService(authorization, history, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    static Stream<Arguments> boundaryOracles() {
        return Stream.of(Arguments.of("10", "13", "10", "2", PointStatus.NEEDS_REVIEW),
                Arguments.of("10", "12", "10", "2", PointStatus.NORMAL), Arguments.of("10", "10", "10", "2", PointStatus.NORMAL),
                Arguments.of("0", "0.001", "0", "0.001", PointStatus.NORMAL),
                Arguments.of("0", "0.002", "0", "0.001", PointStatus.NEEDS_REVIEW));
    }

    @ParameterizedTest
    @MethodSource("boundaryOracles")
    void c01StrictBoundaryAndAbsoluteFloorUseExactDecimalArithmetic(String reference, String target, String median, String threshold, PointStatus status) {
        var usages = constants(29, reference); usages.set(28, new BigDecimal(target));
        var response = run(rows(usages), 29);
        var point = response.points().getLast();
        assertThat(response.status()).isEqualTo(Status.ANALYZED);
        assertThat(response.evaluatedPoints()).isEqualTo(1); assertThat(response.insufficientPoints()).isEqualTo(28);
        assertThat(point.referenceCount()).isEqualTo(28);
        assertThat(point.median()).isEqualByComparingTo(median); assertThat(point.mad()).isZero();
        assertThat(point.threshold()).isEqualByComparingTo(threshold); assertThat(point.status()).isEqualTo(status);
        assertThat(point.label()).isEqualTo(status == PointStatus.NEEDS_REVIEW ? "Cần kiểm tra" : null);
    }

    @Test
    void nonzeroMadAndEvenMedianHaveIndependentNumericOracles() {
        List<BigDecimal> usages = new ArrayList<>();
        for (int i = 0; i < 30; i++) usages.add(BigDecimal.valueOf(new int[] {8, 10, 12}[i % 3]));
        usages.add(new BigDecimal("30"));
        var point = run(rows(usages), 31).points().getLast();
        assertThat(point.median()).isEqualByComparingTo("10"); assertThat(point.mad()).isEqualByComparingTo("2");
        assertThat(point.threshold()).isEqualByComparingTo("10.3782"); assertThat(point.status()).isEqualTo(PointStatus.NEEDS_REVIEW);
        usages.clear(); for (int i = 0; i < 28; i++) usages.add(BigDecimal.valueOf(i)); usages.add(new BigDecimal("50"));
        point = run(rows(usages), 29).points().getLast();
        assertThat(point.median()).isEqualByComparingTo("13.5"); assertThat(point.mad()).isEqualByComparingTo("7");
        assertThat(point.threshold()).isEqualByComparingTo("36.3237");
    }

    @Test
    void currentAndFutureLabelsCannotLeakIntoTheirOwnOrEarlierBaseline() {
        var original = run(rows(constants(42, "10")), 42);
        var mutated = constants(42, "10"); mutated.set(41, new BigDecimal("10000"));
        var changed = run(rows(mutated), 42);
        assertThat(changed.points().subList(0, 41)).isEqualTo(original.points().subList(0, 41));
        assertThat(changed.points().getLast().median()).isEqualByComparingTo(original.points().getLast().median());
        assertThat(changed.points().getLast().mad()).isEqualByComparingTo(original.points().getLast().mad());
        assertThat(changed.points().getLast().threshold()).isEqualByComparingTo(original.points().getLast().threshold());
        assertThat(changed.points().getLast().referenceCount()).isEqualTo(41);
        assertThat(changed.points().getLast().status()).isEqualTo(PointStatus.NEEDS_REVIEW);
    }

    @Test
    void missingReadingExcludesTwoDeltasWithoutApportioningMultiDayUsage() {
        var samples = rows(constants(42, "10")); samples.remove(5);
        var result = run(samples, 42);
        assertThat(result.points().get(4).reasonCode()).isEqualTo("MISSING_READING");
        assertThat(result.points().get(5).reasonCode()).isEqualTo("MISSING_PREDECESSOR");
        assertThat(result.excludedPoints()).isEqualTo(2); assertThat(result.evaluatedPoints()).isEqualTo(12);
        assertThat(result.points().get(6).usage()).isEqualByComparingTo("10");
        assertThat(result.points()).noneMatch(point -> point.status() == PointStatus.NEEDS_REVIEW);
    }

    @Test
    void resetReplacementAndNegativeDropClearHistoricalReference() {
        for (String kind : List.of("RESET", "REPLACEMENT", "NEGATIVE_DELTA")) {
            var samples = rows(constants(42, "10"));
            for (int day = 35; day <= 42; day++) {
                var old = samples.get(day);
                BigDecimal value = BigDecimal.valueOf((day - 35L) * 10 + (kind.equals("NEGATIVE_DELTA") ? 5 : 0));
                samples.set(day, new UtilityReadingSample(1, 11, Unit.KWH, old.date(), value,
                        kind.equals("REPLACEMENT") ? "epoch2" : "epoch1", day == 35 && !kind.equals("NEGATIVE_DELTA") ? Marker.valueOf(kind) : Marker.NORMAL));
            }
            var result = run(samples, 42);
            assertThat(result.points().get(34).reasonCode()).isEqualTo(kind);
            assertThat(result.points().get(34).status()).isEqualTo(PointStatus.EXCLUDED);
            assertThat(result.points().getLast().referenceCount()).isEqualTo(6);
            assertThat(result.points().getLast().status()).isEqualTo(PointStatus.INSUFFICIENT_COVERAGE);
            assertThat(result.evaluatedPoints()).isEqualTo(6);
        }
    }

    @Test
    void negativeRawAndInvalidPredecessorAreNotClampedToNormalZero() {
        var samples = rows(constants(42, "10"));
        var old = samples.get(35); samples.set(35, new UtilityReadingSample(1, 11, Unit.KWH, old.date(), new BigDecimal("-1"), "epoch1", Marker.NORMAL));
        var response = run(samples, 42);
        assertThat(response.points().get(34).reasonCode()).isEqualTo("NEGATIVE_READING");
        assertThat(response.points().get(35).reasonCode()).isEqualTo("INVALID_PREDECESSOR");
        assertThat(response.points().get(34).usage()).isNull(); assertThat(response.points().get(35).usage()).isNull();
    }

    @Test
    void missingAnchorAndEmptySuccessfulBatchRemainCoverageNotOutage() {
        var samples = rows(constants(42, "10")); samples.removeFirst();
        var response = run(samples, 42);
        assertThat(response.points().getFirst().reasonCode()).isEqualTo("MISSING_PREDECESSOR");
        assertThat(response.evaluatedPoints()).isEqualTo(13);
        response = run(List.of(), 42);
        assertThat(response.status()).isEqualTo(Status.INSUFFICIENT_COVERAGE);
        assertThat(response.excludedPoints()).isEqualTo(42); assertThat(response.evaluatedPoints()).isZero();
        assertThat(response.dataMode()).isEqualTo("SYNTHETIC");
    }

    @Test
    void authorizationSurroundsReadAndRevokeDiscardsComputedPoints() {
        var response = run(rows(constants(42, "10")), 42);
        var order = inOrder(authorization, history);
        order.verify(authorization).authorize(1, 11); order.verify(history).read(new Query(SCOPE, FROM, END, NOW));
        order.verify(authorization).authorize(1, 11); order.verifyNoMoreInteractions();
        assertThat(response.validPoints()).isEqualTo(42);
        when(authorization.authorize(1, 11)).thenReturn(Optional.of(SCOPE), Optional.empty());
        assertFailure(service.analyze(1, 11, FROM, END), Status.ACCESS_DENIED);
        when(authorization.authorize(1, 11)).thenReturn(Optional.of(SCOPE), Optional.of(new Scope(1, 10, 11, Unit.M3)));
        assertFailure(service.analyze(1, 11, FROM, END), Status.ACCESS_DENIED);
    }

    @Test
    void deniedNullWrongScopeOrAuthOutageNeverReadsHistory() {
        when(authorization.authorize(1, 11)).thenReturn(Optional.empty());
        assertFailure(service.analyze(1, 11, FROM, END), Status.ACCESS_DENIED);
        when(authorization.authorize(1, 11)).thenReturn(null);
        assertFailure(service.analyze(1, 11, FROM, END), Status.DEPENDENCY_UNAVAILABLE);
        when(authorization.authorize(1, 11)).thenReturn(Optional.of(new Scope(2, 10, 11, Unit.KWH)));
        assertFailure(service.analyze(1, 11, FROM, END), Status.DEPENDENCY_UNAVAILABLE);
        when(authorization.authorize(1, 11)).thenThrow(new IllegalStateException("PRIVATE_AUTH"));
        var result = service.analyze(1, 11, FROM, END); assertFailure(result, Status.DEPENDENCY_UNAVAILABLE);
        assertThat(result.toString()).doesNotContain("PRIVATE_AUTH"); verifyNoInteractions(history);
    }

    @Test
    void dependencyNullAndExceptionAreNotSyntheticFallback() {
        when(history.read(any())).thenReturn(null);
        assertFailure(service.analyze(1, 11, FROM, END), Status.DEPENDENCY_UNAVAILABLE);
        when(history.read(any())).thenThrow(new IllegalStateException("PRIVATE_SOURCE"));
        var response = service.analyze(1, 11, FROM, END); assertFailure(response, Status.DEPENDENCY_UNAVAILABLE);
        assertThat(response.toString()).doesNotContain("PRIVATE_SOURCE");
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(Arguments.of(null, 11, FROM, END), Arguments.of(0, 11, FROM, END), Arguments.of(32768, 11, FROM, END),
                Arguments.of(1, null, FROM, END), Arguments.of(1, 0, FROM, END), Arguments.of(1, 11, null, END), Arguments.of(1, 11, FROM, null),
                Arguments.of(1, 11, END, FROM), Arguments.of(1, 11, FROM.minusDays(1), END), Arguments.of(1, 11, END.plusDays(1), END.plusDays(1)),
                Arguments.of(1, 11, FROM, END.plusDays(2)), Arguments.of(1, 11, LocalDate.MIN, LocalDate.MIN.plusDays(1)));
    }
    @ParameterizedTest @MethodSource("invalidRequests")
    void invalidRequestStopsBeforeDependencies(Integer hotel, Integer meter, LocalDate from, LocalDate to) {
        assertFailure(service.analyze(hotel, meter, from, to), Status.INVALID_REQUEST); verifyNoInteractions(authorization, history);
    }

    static Stream<Arguments> invalidBatches() {
        List<UnaryOperator<Batch>> faults = List.of(
                b -> new Batch(b.scope(), b.dataMode(), b.basis(), Coverage.PARTIAL, b.readings()),
                b -> new Batch(b.scope(), "AUTHORITATIVE", b.basis(), b.coverage(), b.readings()),
                b -> new Batch(b.scope(), b.dataMode(), "UNKNOWN", b.coverage(), b.readings()),
                b -> new Batch(new Scope(2, 10, 11, Unit.KWH), b.dataMode(), b.basis(), b.coverage(), b.readings()),
                b -> { var rows = new ArrayList<>(b.readings()); rows.set(1, rows.getFirst()); return batch(rows); },
                b -> { var rows = new ArrayList<>(b.readings()); rows.add(rows.getFirst()); return batch(rows); },
                b -> mutate(b, new UtilityReadingSample(1, 11, Unit.KWH, FROM.minusDays(2), BigDecimal.ZERO, "epoch1", Marker.NORMAL)),
                b -> mutate(b, new UtilityReadingSample(1, 12, Unit.KWH, FROM, BigDecimal.TEN, "epoch1", Marker.NORMAL)),
                b -> mutate(b, new UtilityReadingSample(1, 11, Unit.M3, FROM, BigDecimal.TEN, "epoch1", Marker.NORMAL)),
                b -> mutate(b, new UtilityReadingSample(1, 11, Unit.KWH, FROM, null, "epoch1", Marker.NORMAL)),
                b -> mutate(b, new UtilityReadingSample(1, 11, Unit.KWH, FROM, new BigDecimal("1000000000"), "epoch1", Marker.NORMAL)),
                b -> mutate(b, new UtilityReadingSample(1, 11, Unit.KWH, FROM, new BigDecimal("0.0001"), "epoch1", Marker.NORMAL)),
                b -> mutate(b, new UtilityReadingSample(1, 11, Unit.KWH, FROM, BigDecimal.TEN, "epoch1", null)),
                b -> mutate(b, new UtilityReadingSample(1, 11, Unit.KWH, FROM, BigDecimal.TEN, "epoch2", Marker.NORMAL)),
                b -> new Batch(b.scope(), b.dataMode(), b.basis(), b.coverage(), null));
        return faults.stream().map(Arguments::of);
    }
    @ParameterizedTest @MethodSource("invalidBatches")
    void invalidSourceDataNeverReturnsPartialOrMixedUnitAnalysis(UnaryOperator<Batch> fault) {
        when(history.read(any())).thenReturn(fault.apply(batch(rows(constants(42, "10")))));
        assertFailure(service.analyze(1, 11, FROM, END), Status.DEPENDENCY_UNAVAILABLE);
    }

    @Test
    void inputOrderingAndReturnedCollectionsAreStableAndImmutable() {
        var samples = rows(constants(42, "10")); var before = run(samples, 42);
        Collections.shuffle(samples, new Random(31)); assertThat(run(samples, 42)).isEqualTo(before);
        assertThatThrownBy(() -> before.points().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void vietnamDayBoundaryTrimsTodayAndUsesOneClockInstant() {
        Clock clock = mock(Clock.class); when(clock.instant()).thenReturn(NOW, NOW.plusSeconds(86400));
        service = new UtilityAnomalyDetectionService(authorization, history, clock);
        when(history.read(any())).thenAnswer(call -> {
            Query query = call.getArgument(0);
            assertThat(query.fromUtcInclusive()).isEqualTo(Instant.parse("2026-10-29T17:00:00Z"));
            assertThat(query.toUtcExclusive()).isEqualTo(NOW);
            return batch(List.of(new UtilityReadingSample(1, 11, Unit.KWH, END.minusDays(1), BigDecimal.ZERO, "epoch1", Marker.NORMAL),
                    new UtilityReadingSample(1, 11, Unit.KWH, END, BigDecimal.TEN, "epoch1", Marker.NORMAL)));
        });
        var result = service.analyze(1, 11, END, END.plusDays(1));
        assertThat(result.toInclusive()).isEqualTo(END); assertThat(result.points()).hasSize(1); verify(clock, times(1)).instant();
    }

    private UtilityAnomalyResponse run(List<UtilityReadingSample> samples, int days) {
        when(history.read(any())).thenReturn(batch(samples)); return service.analyze(1, 11, FROM, FROM.plusDays(days - 1));
    }
    static List<BigDecimal> constants(int count, String value) { return new ArrayList<>(Collections.nCopies(count, new BigDecimal(value))); }
    static List<UtilityReadingSample> rows(List<BigDecimal> usages) {
        var result = new ArrayList<UtilityReadingSample>(); BigDecimal value = BigDecimal.ZERO;
        result.add(new UtilityReadingSample(1, 11, Unit.KWH, FROM.minusDays(1), value, "epoch1", Marker.NORMAL));
        for (int i = 0; i < usages.size(); i++) { value = value.add(usages.get(i)); result.add(new UtilityReadingSample(1, 11, Unit.KWH, FROM.plusDays(i), value, "epoch1", Marker.NORMAL)); }
        return result;
    }
    static Batch batch(List<UtilityReadingSample> readings) { return new Batch(SCOPE, "SYNTHETIC", "SYNTHETIC_CUMULATIVE_DAILY_V1", Coverage.COMPLETE, readings); }
    private static Batch mutate(Batch source, UtilityReadingSample row) { var rows = new ArrayList<>(source.readings()); rows.set(1, row); return batch(rows); }
    private static void assertFailure(UtilityAnomalyResponse response, Status status) {
        assertThat(response.status()).isEqualTo(status); assertThat(response.points()).isEmpty();
        assertThat(response.unit()).isNull(); assertThat(response.dataMode()).isNull();
    }
}
