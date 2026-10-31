package vn.edu.utc.hotel_booking.modules.ai.forecast.service;

import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.BacktestSummary;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.EvaluationStatus;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.Fold;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.Model;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ForecastBacktestEvaluator {
    public static final int FOLDS = 5;
    public static final int HORIZON = 30;
    private static final double EPSILON = 1e-9;

    public record ModelPrediction(LocalDate date, double baseline, double candidate) { }
    public record FoldTrace(int fold, List<ModelPrediction> predictions) {
        public FoldTrace { predictions = List.copyOf(predictions); }
    }
    public record Evaluation(BacktestSummary summary, Model selectedModel, Double baselineRadius,
                             Double candidateRadius, List<FoldTrace> traces) {
        public Evaluation { traces = List.copyOf(traces); }
    }

    public Evaluation evaluate(List<DailyOccupancySample> input) {
        List<DailyOccupancySample> rows = OccupancyModels.ordered(input);
        if (rows.size() < OccupancyModels.MIN_TRAIN + FOLDS * HORIZON) return insufficient();
        List<Fold> folds = new ArrayList<>();
        List<FoldTrace> traces = new ArrayList<>();
        List<Double> baselineErrors = new ArrayList<>();
        List<Double> candidateErrors = new ArrayList<>();
        for (int fold = 0; fold < FOLDS; fold++) {
            int origin = rows.size() - FOLDS * HORIZON + fold * HORIZON;
            List<DailyOccupancySample> train = rows.subList(Math.max(0, origin - OccupancyModels.MAX_TRAIN), origin);
            List<DailyOccupancySample> test = rows.subList(origin, origin + HORIZON);
            if (!OccupancyModels.trainable(train) || OccupancyModels.usable(test) < 24) return insufficient();
            OccupancyModels.Fit baseline = OccupancyModels.seasonal(train);
            OccupancyModels.Fit candidate = OccupancyModels.ridge(train);
            List<ModelPrediction> predictions = new ArrayList<>();
            double baselineSum = 0, candidateSum = 0;
            int count = 0;
            for (DailyOccupancySample row : test) {
                double basePrediction = baseline.predict(row.date());
                double candidatePrediction = candidate.predict(row.date());
                predictions.add(new ModelPrediction(row.date(), basePrediction, candidatePrediction));
                double actual = OccupancyModels.target(row);
                if (!Double.isFinite(actual)) continue;
                double baselineError = Math.abs(actual - basePrediction);
                double candidateError = Math.abs(actual - candidatePrediction);
                baselineErrors.add(baselineError); candidateErrors.add(candidateError);
                baselineSum += baselineError; candidateSum += candidateError; count++;
            }
            folds.add(new Fold(fold + 1, train.getFirst().date(), train.getLast().date(), test.getFirst().date(),
                    test.getLast().date(), OccupancyModels.usable(train), count, baselineSum / count, candidateSum / count));
            traces.add(new FoldTrace(fold + 1, predictions));
        }
        double baselineMae = baselineErrors.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double candidateMae = candidateErrors.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        Model selected = candidatePassesGate(baselineMae, candidateMae, folds) ? Model.RIDGE_TREND_WEEKDAY : Model.SEASONAL_NAIVE_7;
        return new Evaluation(new BacktestSummary(EvaluationStatus.COMPLETE, folds, baselineErrors.size(), baselineMae, candidateMae),
                selected, OccupancyModels.p90(baselineErrors), OccupancyModels.p90(candidateErrors), traces);
    }

    public static boolean candidatePassesGate(double baselineMae, double candidateMae, List<Fold> folds) {
        return Double.isFinite(baselineMae) && Double.isFinite(candidateMae) && baselineMae >= 0 && candidateMae >= 0
                && folds != null && folds.size() == FOLDS && candidateMae <= baselineMae + EPSILON
                && folds.stream().allMatch(fold -> fold != null && Double.isFinite(fold.baselineMaePp())
                    && Double.isFinite(fold.candidateMaePp()) && fold.baselineMaePp() >= 0 && fold.candidateMaePp() >= 0
                    && fold.candidateMaePp() <= fold.baselineMaePp() + 5.0 + EPSILON);
    }

    private Evaluation insufficient() {
        return new Evaluation(new BacktestSummary(EvaluationStatus.INSUFFICIENT_COVERAGE, List.of(), 0, null, null),
                null, null, null, List.of());
    }
}
