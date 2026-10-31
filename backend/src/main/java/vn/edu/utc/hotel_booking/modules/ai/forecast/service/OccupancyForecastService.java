package vn.edu.utc.hotel_booking.modules.ai.forecast.service;

import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.EvaluationStatus;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.ForecastDay;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.Model;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.Status;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryResult;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryPort;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Internal read-only orchestration; production wiring waits for a verified data source. */
public class OccupancyForecastService {
    private final OccupancyHistoryService history;
    private final ForecastBacktestEvaluator evaluator;

    public OccupancyForecastService(OccupancyHistoryService history, ForecastBacktestEvaluator evaluator) {
        this.history = Objects.requireNonNull(history);
        this.evaluator = Objects.requireNonNull(evaluator);
    }

    public OccupancyForecastResponse forecast(Integer hotelId, LocalDate from, LocalDate to) {
        OccupancyHistoryResult window;
        try { window = history.read(hotelId, from, to); }
        catch (RuntimeException exception) { return unavailable(hotelId); }
        if (window == null) return unavailable(hotelId);
        switch (window.status()) {
            case ACCESS_DENIED -> { return empty(Status.ACCESS_DENIED, window, "ACCESS_DENIED", null); }
            case INVALID_REQUEST -> { return empty(Status.INVALID_REQUEST, window, "INVALID_REQUEST", null); }
            case DEPENDENCY_UNAVAILABLE -> { return empty(Status.DEPENDENCY_UNAVAILABLE, window, "DEPENDENCY_UNAVAILABLE", null); }
            case COMPLETE, PARTIAL -> { }
        }
        try {
            List<DailyOccupancySample> rows = OccupancyModels.ordered(window.samples());
            if (!Objects.equals(window.hotelId(), hotelId) || window.hotelId() == null
                    || rows.getFirst().hotelId() != hotelId || !Objects.equals(window.fromInclusive(), from)
                    || !rows.getFirst().date().equals(window.fromInclusive()) || !rows.getLast().date().equals(window.toInclusive())
                    || rows.size() != ChronoUnit.DAYS.between(window.fromInclusive(), window.toInclusive()) + 1
                    || window.dataMode() == null || to == null || window.toInclusive().isAfter(to)) {
                return unavailable(hotelId);
            }
            if (!window.toInclusive().equals(window.asOf().atZone(OccupancyHistoryPort.HOTEL_ZONE).toLocalDate().minusDays(1))) {
                return empty(Status.INSUFFICIENT_COVERAGE, window, "STALE_OR_INCOMPLETE_HISTORY_END", null);
            }
            List<DailyOccupancySample> train = rows.subList(Math.max(0, rows.size() - OccupancyModels.MAX_TRAIN), rows.size());
            if (!OccupancyModels.trainable(train)) return empty(Status.INSUFFICIENT_COVERAGE, window, "INSUFFICIENT_RECENT_HISTORY", null);
            ForecastBacktestEvaluator.Evaluation evaluation = evaluator.evaluate(rows);
            boolean evaluated = evaluation.summary().status() == EvaluationStatus.COMPLETE;
            Model model;
            double radius;
            if (evaluated) {
                model = evaluation.selectedModel();
                radius = model == Model.RIDGE_TREND_WEEKDAY ? evaluation.candidateRadius() : evaluation.baselineRadius();
            } else {
                List<Double> errors = OccupancyModels.weeklyErrors(train);
                if (errors.size() < 28) return empty(Status.INSUFFICIENT_COVERAGE, window, "INSUFFICIENT_CAUSAL_RESIDUALS", evaluation.summary());
                model = Model.SEASONAL_NAIVE_7;
                radius = OccupancyModels.p90(errors);
            }
            OccupancyModels.Fit fit = model == Model.RIDGE_TREND_WEEKDAY ? OccupancyModels.ridge(train) : OccupancyModels.seasonal(train);
            List<ForecastDay> points = new ArrayList<>();
            for (int i = 1; i <= ForecastBacktestEvaluator.HORIZON; i++) {
                LocalDate date = window.toInclusive().plusDays(i);
                double point = fit.predict(date);
                points.add(new ForecastDay(date, point, OccupancyModels.clamp(point - radius), OccupancyModels.clamp(point + radius)));
            }
            return new OccupancyForecastResponse(evaluated ? Status.FORECAST_READY : Status.BASELINE_FALLBACK,
                    hotelId, window.asOf(), window.dataMode(), model,
                    model == Model.RIDGE_TREND_WEEKDAY ? "u22-ridge-weekday-v1" : "u22-seasonal-naive-7-v1",
                    evaluated ? (model == Model.RIDGE_TREND_WEEKDAY ? "RIDGE_PASSED_MAE_GATE" : "BASELINE_SELECTED_BY_MAE_GATE") : "BACKTEST_INSUFFICIENT_COVERAGE",
                    evaluated ? "ROLLING_OUT_OF_SAMPLE_ABS_ERROR_P90" : "CAUSAL_WEEKLY_DIFFERENCE_ABS_ERROR_P90",
                    radius, points, evaluation.summary());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return unavailable(hotelId);
        }
    }

    private OccupancyForecastResponse empty(Status status, OccupancyHistoryResult history, String reason,
                                             OccupancyForecastResponse.BacktestSummary summary) {
        return new OccupancyForecastResponse(status, history.hotelId(), history.asOf(), history.dataMode(),
                null, null, reason, null, null, List.of(), summary);
    }

    private OccupancyForecastResponse unavailable(Integer hotelId) {
        return new OccupancyForecastResponse(Status.DEPENDENCY_UNAVAILABLE, hotelId, null, null,
                null, null, "DEPENDENCY_UNAVAILABLE", null, null, List.of(), null);
    }
}
