package vn.edu.utc.hotel_booking.modules.ai.forecast.dto;

import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch.DataMode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Internal result contract; no HTTP endpoint or production fixture wiring is registered. */
public record OccupancyForecastResponse(
        Status status, Integer hotelId, Instant asOf, DataMode dataMode,
        Model model, String modelVersion, String reasonCode,
        String rangeMethod, Double residualRadiusPp,
        List<ForecastDay> points, BacktestSummary backtest
) {
    public enum Status { FORECAST_READY, BASELINE_FALLBACK, INSUFFICIENT_COVERAGE, INVALID_REQUEST, ACCESS_DENIED, DEPENDENCY_UNAVAILABLE }
    public enum Model { SEASONAL_NAIVE_7, RIDGE_TREND_WEEKDAY }
    public enum EvaluationStatus { COMPLETE, INSUFFICIENT_COVERAGE }

    public OccupancyForecastResponse {
        Objects.requireNonNull(status);
        points = List.copyOf(points);
        if (status == Status.FORECAST_READY || status == Status.BASELINE_FALLBACK) {
            Objects.requireNonNull(asOf);
            Objects.requireNonNull(dataMode);
            Objects.requireNonNull(model);
            Objects.requireNonNull(modelVersion);
            Objects.requireNonNull(rangeMethod);
            if (points.size() != 30 || residualRadiusPp == null || !Double.isFinite(residualRadiusPp) || residualRadiusPp < 0) {
                throw new IllegalArgumentException("Invalid occupancy forecast output");
            }
        } else if (!points.isEmpty() || model != null || residualRadiusPp != null) {
            throw new IllegalArgumentException("Unavailable forecast must not contain predictions");
        }
    }

    public record ForecastDay(LocalDate date, double occupancyPercent, double lowerPercent, double upperPercent) {
        public ForecastDay {
            Objects.requireNonNull(date);
            if (!Double.isFinite(occupancyPercent) || !Double.isFinite(lowerPercent) || !Double.isFinite(upperPercent)
                    || lowerPercent < 0 || upperPercent > 100 || lowerPercent > occupancyPercent || occupancyPercent > upperPercent) {
                throw new IllegalArgumentException("Invalid occupancy prediction range");
            }
        }
    }

    public record Fold(int fold, LocalDate trainFrom, LocalDate trainTo, LocalDate testFrom, LocalDate testTo,
                       int trainingUsableDays, int scoredDays, double baselineMaePp, double candidateMaePp) { }

    public record BacktestSummary(EvaluationStatus status, List<Fold> folds, int scoredDays,
                                  Double baselineMaePp, Double candidateMaePp) {
        public BacktestSummary { folds = List.copyOf(folds); }
    }
}
