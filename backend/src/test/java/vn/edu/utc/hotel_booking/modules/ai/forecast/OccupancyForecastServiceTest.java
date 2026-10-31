package vn.edu.utc.hotel_booking.modules.ai.forecast;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.EvaluationStatus;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.Model;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.Status;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch.DataMode;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryResult;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.ForecastBacktestEvaluator;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.OccupancyForecastService;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.OccupancyHistoryService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OccupancyForecastServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void returnsThirtyFutureDatesModelMaeAndBoundedImmutableRanges() {
        var rows = ForecastBacktestEvaluatorTest.rows(420, true);
        var result = forecast(window(rows), rows.getFirst().date(), rows.getLast().date());
        assertThat(result.status()).isEqualTo(Status.FORECAST_READY);
        assertThat(result.model()).isEqualTo(Model.RIDGE_TREND_WEEKDAY);
        assertThat(result.modelVersion()).isEqualTo("u22-ridge-weekday-v1");
        assertThat(result.dataMode()).isEqualTo(DataMode.SYNTHETIC);
        assertThat(result.backtest().scoredDays()).isEqualTo(150);
        assertThat(result.rangeMethod()).isEqualTo("ROLLING_OUT_OF_SAMPLE_ABS_ERROR_P90");
        assertThat(result.points()).hasSize(30);
        for (int i = 0; i < 30; i++) {
            var day = result.points().get(i);
            assertThat(day.date()).isEqualTo(rows.getLast().date().plusDays(i + 1));
            assertThat(day.lowerPercent()).isBetween(0.0, day.occupancyPercent());
            assertThat(day.upperPercent()).isBetween(day.occupancyPercent(), 100.0);
        }
        assertThatThrownBy(() -> result.points().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shortHistoryUsesLabelledBaselineWithCausalRangeAndNoFakeFiveFoldMae() {
        var rows = ForecastBacktestEvaluatorTest.rows(120, false);
        var result = forecast(window(rows), rows.getFirst().date(), rows.getLast().date());
        assertThat(result.status()).isEqualTo(Status.BASELINE_FALLBACK);
        assertThat(result.model()).isEqualTo(Model.SEASONAL_NAIVE_7);
        assertThat(result.rangeMethod()).isEqualTo("CAUSAL_WEEKLY_DIFFERENCE_ABS_ERROR_P90");
        assertThat(result.backtest().status()).isEqualTo(EvaluationStatus.INSUFFICIENT_COVERAGE);
        assertThat(result.backtest().baselineMaePp()).isNull();
        assertThat(result.residualRadiusPp()).isZero();
        for (int i = 0; i < 30; i++) {
            assertThat(result.points().get(i).occupancyPercent()).isEqualTo(rows.get(rows.size() - 7 + i % 7).occupancyPercent().orElseThrow());
        }
    }

    @Test
    void tooFewUsableTargetsOrIncompleteRecentWeekDoesNotFabricateForecasts() {
        var rows = ForecastBacktestEvaluatorTest.rows(83, false);
        assertEmpty(forecast(window(rows), rows.getFirst().date(), rows.getLast().date()), Status.INSUFFICIENT_COVERAGE);
        rows = ForecastBacktestEvaluatorTest.rows(420, false);
        rows.set(419, DailyOccupancySample.missing(1, rows.getLast().date()));
        assertEmpty(forecast(window(rows), rows.getFirst().date(), rows.getLast().date()), Status.INSUFFICIENT_COVERAGE);
    }

    @Test
    void onlyRecent180DaysCountTowardFinalTrainingCoverage() {
        var rows = ForecastBacktestEvaluatorTest.rows(420, false);
        for (int i = 240; i < 410; i++) rows.set(i, DailyOccupancySample.missing(1, rows.get(i).date()));
        assertEmpty(forecast(window(rows), rows.getFirst().date(), rows.getLast().date()), Status.INSUFFICIENT_COVERAGE);
    }

    @Test
    void sparseAlternatingWeeksWithoutEnoughCausalResidualsCannotInventARange() {
        var rows = ForecastBacktestEvaluatorTest.rows(180, false);
        for (int i = 0; i < 173; i++) {
            if ((i / 7) % 2 == 1) rows.set(i, DailyOccupancySample.missing(1, rows.get(i).date()));
        }
        var result = forecast(window(rows), rows.getFirst().date(), rows.getLast().date());
        assertEmpty(result, Status.INSUFFICIENT_COVERAGE);
        assertThat(result.reasonCode()).isEqualTo("INSUFFICIENT_CAUSAL_RESIDUALS");
    }

    @Test
    void extrapolationAndResidualRangesClampAtBothPhysicalBounds() {
        for (boolean upward : new boolean[] {true, false}) {
            var rows = ForecastBacktestEvaluatorTest.rows(420, false);
            for (int i = 0; i < rows.size(); i++) {
                double value = upward ? 10 + i * 0.21 : 90 - i * 0.21;
                rows.set(i, new DailyOccupancySample(1, rows.get(i).date(), (int) Math.round(value * 100), 10000,
                        DailyOccupancySample.Coverage.COMPLETE));
            }
            var result = forecast(window(rows), rows.getFirst().date(), rows.getLast().date());
            assertThat(result.model()).isEqualTo(Model.RIDGE_TREND_WEEKDAY);
            assertThat(result.points().getLast().occupancyPercent()).isEqualTo(upward ? 100.0 : 0.0);
            assertThat(result.points()).allSatisfy(day -> {
                assertThat(day.lowerPercent()).isBetween(0.0, 100.0);
                assertThat(day.upperPercent()).isBetween(0.0, 100.0);
            });
        }
    }

    @ParameterizedTest
    @CsvSource({"ACCESS_DENIED,ACCESS_DENIED", "INVALID_REQUEST,INVALID_REQUEST", "DEPENDENCY_UNAVAILABLE,DEPENDENCY_UNAVAILABLE"})
    void preservesHistoryFailureWithoutForecastOrEvaluatorCall(OccupancyHistoryResult.Status historyStatus, Status status) {
        OccupancyHistoryService history = mock(OccupancyHistoryService.class);
        ForecastBacktestEvaluator evaluator = mock(ForecastBacktestEvaluator.class);
        var from = LocalDate.of(2026, 1, 1);
        when(history.read(1, from, from)).thenReturn(new OccupancyHistoryResult(historyStatus, 1, from, from,
                Instant.parse("2026-01-03T00:00:00Z"), null, List.of()));
        var result = new OccupancyForecastService(history, evaluator).forecast(1, from, from);
        assertEmpty(result, status);
        assertThat(result.dataMode()).isNull();
        verifyNoInteractions(evaluator);
    }

    @Test
    void historyOutageNeverReturnsSyntheticFallbackOrRawError() {
        OccupancyHistoryService history = mock(OccupancyHistoryService.class);
        var service = new OccupancyForecastService(history, new ForecastBacktestEvaluator());
        var date = LocalDate.of(2026, 1, 1);
        when(history.read(1, date, date)).thenReturn(null);
        assertEmpty(service.forecast(1, date, date), Status.DEPENDENCY_UNAVAILABLE);
        when(history.read(1, date, date)).thenThrow(new IllegalStateException("PRIVATE_PROVIDER_MESSAGE"));
        var result = service.forecast(1, date, date);
        assertEmpty(result, Status.DEPENDENCY_UNAVAILABLE);
        assertThat(result.toString()).doesNotContain("PRIVATE_PROVIDER_MESSAGE");
    }

    @Test
    void staleHistoryIsNotShiftedForwardToPretendItIsCurrent() {
        var rows = ForecastBacktestEvaluatorTest.rows(420, false);
        var original = window(rows);
        var stale = new OccupancyHistoryResult(original.status(), 1, original.fromInclusive(), original.toInclusive(),
                original.asOf().plusSeconds(86400), DataMode.SYNTHETIC, rows);
        var result = forecast(stale, rows.getFirst().date(), rows.getLast().date());
        assertEmpty(result, Status.INSUFFICIENT_COVERAGE);
        assertThat(result.reasonCode()).isEqualTo("STALE_OR_INCOMPLETE_HISTORY_END");
    }

    @Test
    void mismatchedHotelCalendarOrMetadataFailsClosed() {
        var rows = ForecastBacktestEvaluatorTest.rows(300, false);
        var original = window(rows);
        var mismatch = new OccupancyHistoryResult(original.status(), 2, original.fromInclusive(), original.toInclusive(), original.asOf(), DataMode.SYNTHETIC, rows);
        assertEmpty(forecast(mismatch, rows.getFirst().date(), rows.getLast().date()), Status.DEPENDENCY_UNAVAILABLE);
        rows.remove(20);
        assertEmpty(forecast(window(rows), rows.getFirst().date(), rows.getLast().date()), Status.DEPENDENCY_UNAVAILABLE);
    }

    @Test
    void constantAndNearBoundarySeriesRemainFiniteAndRangesClampToZeroOneHundred() {
        for (int occupied : new int[] {0, 50, 100}) {
            var rows = ForecastBacktestEvaluatorTest.rows(420, false);
            rows.replaceAll(row -> new DailyOccupancySample(1, row.date(), occupied, 100, DailyOccupancySample.Coverage.COMPLETE));
            var result = forecast(window(rows), rows.getFirst().date(), rows.getLast().date());
            assertThat(result.status()).isEqualTo(Status.FORECAST_READY);
            assertThat(result.points()).allSatisfy(day -> {
                assertThat(day.occupancyPercent()).isCloseTo(occupied, within(1e-8));
                assertThat(day.lowerPercent()).isBetween(0.0, 100.0);
                assertThat(day.upperPercent()).isBetween(0.0, 100.0);
            });
        }
        assertThatThrownBy(() -> new OccupancyForecastResponse.ForecastDay(LocalDate.now(), Double.NaN, 0, 100)).isInstanceOf(IllegalArgumentException.class);
    }

    private OccupancyHistoryResult window(List<DailyOccupancySample> rows) {
        var end = rows.getLast().date();
        boolean missing = rows.stream().anyMatch(row -> row.coverage() == DailyOccupancySample.Coverage.MISSING);
        return new OccupancyHistoryResult(missing ? OccupancyHistoryResult.Status.PARTIAL : OccupancyHistoryResult.Status.COMPLETE,
                1, rows.getFirst().date(), end, end.plusDays(1).atStartOfDay(ZONE).toInstant(), DataMode.SYNTHETIC, rows);
    }

    private OccupancyForecastResponse forecast(OccupancyHistoryResult result, LocalDate from, LocalDate to) {
        OccupancyHistoryService history = mock(OccupancyHistoryService.class);
        when(history.read(1, from, to)).thenReturn(result);
        return new OccupancyForecastService(history, new ForecastBacktestEvaluator()).forecast(1, from, to);
    }

    private void assertEmpty(OccupancyForecastResponse result, Status status) {
        assertThat(result.status()).isEqualTo(status);
        assertThat(result.points()).isEmpty();
        assertThat(result.model()).isNull();
        assertThat(result.residualRadiusPp()).isNull();
    }
}
