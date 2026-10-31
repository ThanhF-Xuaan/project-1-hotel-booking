package vn.edu.utc.hotel_booking.modules.ai.forecast;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.EvaluationStatus;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.Fold;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.Model;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample.Coverage;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.ForecastBacktestEvaluator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ForecastBacktestEvaluatorTest {
    private static final LocalDate START = LocalDate.of(2025, 9, 5);
    private final ForecastBacktestEvaluator evaluator = new ForecastBacktestEvaluator();

    @Test
    void fiveRollingFoldsHaveDisjointThirtyDayTestsAndBoundedPastOnlyTraining() {
        for (int size : new int[] {234, 300, 420}) {
            var result = evaluator.evaluate(rows(size, true));
            assertThat(result.summary().status()).isEqualTo(EvaluationStatus.COMPLETE);
            assertThat(result.summary().folds()).hasSize(5);
            assertThat(result.summary().scoredDays()).isEqualTo(150);
            LocalDate previousTestTo = null;
            for (Fold fold : result.summary().folds()) {
                assertThat(ChronoUnit.DAYS.between(fold.trainFrom(), fold.trainTo()) + 1).isBetween(84L, 180L);
                assertThat(ChronoUnit.DAYS.between(fold.testFrom(), fold.testTo()) + 1).isEqualTo(30);
                assertThat(fold.trainTo().plusDays(1)).isEqualTo(fold.testFrom());
                if (previousTestTo != null) assertThat(fold.testFrom()).isEqualTo(previousTestTo.plusDays(1));
                previousTestTo = fold.testTo();
            }
        }
    }

    @Test
    void lastFoldHeldoutLabelsCannotAffectAnyFoldPrediction() {
        var original = rows(420, true);
        var changed = new ArrayList<>(original);
        for (int i = 390; i < 420; i++) changed.set(i, value(original.get(i).date(), 0));
        var before = evaluator.evaluate(original);
        var after = evaluator.evaluate(changed);
        assertThat(after.traces()).isEqualTo(before.traces());
        assertThat(after.summary().folds().subList(0, 4)).isEqualTo(before.summary().folds().subList(0, 4));
        assertThat(after.summary().folds().getLast().candidateMaePp()).isNotEqualTo(before.summary().folds().getLast().candidateMaePp());
    }

    @Test
    void firstHeldoutFoldCannotLeakIntoItsFitButCanBecomePastTrainingLater() {
        var original = rows(420, true);
        var changed = new ArrayList<>(original);
        for (int i = 270; i < 420; i++) changed.set(i, value(original.get(i).date(), 10));
        var before = evaluator.evaluate(original);
        var after = evaluator.evaluate(changed);
        assertThat(after.traces().getFirst()).isEqualTo(before.traces().getFirst());
        assertThat(after.traces().get(1)).isNotEqualTo(before.traces().get(1));
    }

    @Test
    void scoresOnlyPairedObservedTargetsAndWeightsAggregateByActualDayCount() {
        var rows = rows(420, true);
        for (int i = 270; i < 276; i++) rows.set(i, DailyOccupancySample.missing(1, rows.get(i).date()));
        for (int i = 276; i < 300; i++) rows.set(i, value(rows.get(i).date(), 90));
        var evaluation = evaluator.evaluate(rows);
        assertThat(evaluation.summary().status()).isEqualTo(EvaluationStatus.COMPLETE);
        assertThat(evaluation.summary().scoredDays()).isEqualTo(144);
        assertThat(evaluation.summary().folds().getFirst().scoredDays()).isEqualTo(24);
        double sum = 0;
        int count = 0;
        for (var trace : evaluation.traces()) {
            for (var prediction : trace.predictions()) {
                var row = rows.get((int) ChronoUnit.DAYS.between(START, prediction.date()));
                if (row.occupancyPercent().isEmpty()) continue;
                sum += Math.abs(row.occupancyPercent().orElseThrow() - prediction.baseline()); count++;
            }
        }
        assertThat(evaluation.summary().baselineMaePp()).isCloseTo(sum / count, within(1e-10));
        double unweighted = evaluation.summary().folds().stream().mapToDouble(Fold::baselineMaePp).average().orElseThrow();
        assertThat(evaluation.summary().baselineMaePp()).isNotCloseTo(unweighted, within(1e-5));
    }

    @Test
    void zeroCapacityNeverBecomesAZeroPercentTarget() {
        var original = rows(420, false);
        original.set(273, new DailyOccupancySample(1, original.get(273).date(), 0, 0, Coverage.COMPLETE));
        var evaluation = evaluator.evaluate(original);
        assertThat(evaluation.summary().scoredDays()).isEqualTo(149);
        assertThat(evaluation.summary().folds().getFirst().scoredDays()).isEqualTo(29);
    }

    @Test
    void insufficientFoldCoverageNeverPublishesPartialAggregateOrChoosesCandidate() {
        var insufficient = rows(420, false);
        for (int i = 270; i < 277; i++) insufficient.set(i, DailyOccupancySample.missing(1, insufficient.get(i).date()));
        var evaluation = evaluator.evaluate(insufficient);
        assertThat(evaluation.summary().status()).isEqualTo(EvaluationStatus.INSUFFICIENT_COVERAGE);
        assertThat(evaluation.summary().folds()).isEmpty();
        assertThat(evaluation.summary().baselineMaePp()).isNull();
        assertThat(evaluation.summary().candidateMaePp()).isNull();
        assertThat(evaluation.selectedModel()).isNull();
        assertThat(evaluation.traces()).isEmpty();
        assertThat(evaluator.evaluate(rows(233, false)).summary().status()).isEqualTo(EvaluationStatus.INSUFFICIENT_COVERAGE);
    }

    @Test
    void candidateImprovesTrendButBaselineWinsUnchangingWeekdayPattern() {
        var trend = evaluator.evaluate(rows(420, true));
        assertThat(trend.selectedModel()).isEqualTo(Model.RIDGE_TREND_WEEKDAY);
        assertThat(trend.summary().candidateMaePp()).isLessThan(trend.summary().baselineMaePp());
        var seasonal = evaluator.evaluate(rows(420, false));
        assertThat(seasonal.selectedModel()).isEqualTo(Model.SEASONAL_NAIVE_7);
        assertThat(seasonal.summary().baselineMaePp()).isZero();
        assertThat(seasonal.summary().candidateMaePp()).isGreaterThan(0);
    }

    @Test
    void rangeIsNearestRankP90OfChosenPastOutOfSampleErrors() {
        var rows = rows(420, true);
        var evaluation = evaluator.evaluate(rows);
        List<Double> errors = new ArrayList<>();
        evaluation.traces().forEach(trace -> trace.predictions().forEach(point -> {
            var row = rows.get((int) ChronoUnit.DAYS.between(START, point.date()));
            errors.add(Math.abs(row.occupancyPercent().orElseThrow() - point.candidate()));
        }));
        errors.sort(Double::compareTo);
        assertThat(evaluation.candidateRadius()).isEqualTo(errors.get((int) Math.ceil(0.9 * errors.size()) - 1));
    }

    @Test
    void inputPermutationIsDeterministicAndTracesAreImmutable() {
        var rows = rows(420, true);
        var expected = evaluator.evaluate(rows);
        Collections.shuffle(rows, new Random(42));
        assertThat(evaluator.evaluate(rows)).isEqualTo(expected);
        assertThatThrownBy(() -> expected.traces().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> expected.traces().getFirst().predictions().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    static Stream<Arguments> gateCases() {
        return Stream.of(Arguments.of(1.0, 1.0, 6.0, true), Arguments.of(1.0, 0.9, 6.01, false),
                Arguments.of(1.0, 1.01, 1.0, false), Arguments.of(Double.NaN, 0.0, 1.0, false),
                Arguments.of(1.0, Double.POSITIVE_INFINITY, 1.0, false), Arguments.of(-1.0, 0.0, 1.0, false));
    }

    @ParameterizedTest
    @MethodSource("gateCases")
    void selectionEnforcesBothAggregateAndEveryFoldGate(double baseline, double candidate, double worstFoldCandidate, boolean accepted) {
        var folds = new ArrayList<Fold>();
        for (int i = 0; i < 5; i++) folds.add(new Fold(i + 1, START, START.plusDays(83), START.plusDays(84), START.plusDays(113), 84, 30, 1, worstFoldCandidate));
        assertThat(ForecastBacktestEvaluator.candidatePassesGate(baseline, candidate, folds)).isEqualTo(accepted);
        assertThat(ForecastBacktestEvaluator.candidatePassesGate(baseline, candidate, folds.subList(0, 4))).isFalse();
    }

    @Test
    void invalidCalendarsScopesAndVolumeAreRejectedRatherThanSilentlyTrimmed() {
        assertThatThrownBy(() -> evaluator.evaluate(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> evaluator.evaluate(rows(421, false))).isInstanceOf(IllegalArgumentException.class);
        var duplicate = rows(300, false); duplicate.set(1, duplicate.getFirst());
        assertThatThrownBy(() -> evaluator.evaluate(duplicate)).isInstanceOf(IllegalArgumentException.class);
        var gap = rows(300, false); gap.remove(1);
        assertThatThrownBy(() -> evaluator.evaluate(gap)).isInstanceOf(IllegalArgumentException.class);
        var mixed = rows(300, false); mixed.set(1, new DailyOccupancySample(2, mixed.get(1).date(), 1, 2, Coverage.COMPLETE));
        assertThatThrownBy(() -> evaluator.evaluate(mixed)).isInstanceOf(IllegalArgumentException.class);
    }

    static List<DailyOccupancySample> rows(int size, boolean trend) {
        List<DailyOccupancySample> rows = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            LocalDate date = START.plusDays(i);
            double percent = 20 + date.getDayOfWeek().getValue() * 3 + (trend ? i * 0.1 : 0);
            rows.add(value(date, percent));
        }
        return rows;
    }

    private static DailyOccupancySample value(LocalDate date, double percent) {
        return new DailyOccupancySample(1, date, (int) Math.round(percent * 100), 10000, Coverage.COMPLETE);
    }
}
