package vn.edu.utc.hotel_booking.modules.ai.forecast.service;

import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryPort;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class OccupancyModels {
    static final int MIN_TRAIN = 84;
    static final int MAX_TRAIN = 180;

    private OccupancyModels() { }

    interface Fit { double predict(LocalDate date); }

    static List<DailyOccupancySample> ordered(List<DailyOccupancySample> input) {
        if (input == null || input.isEmpty() || input.size() > OccupancyHistoryPort.MAX_HISTORY_DAYS) {
            throw new IllegalArgumentException("Invalid occupancy series size");
        }
        List<DailyOccupancySample> rows = new ArrayList<>(input);
        if (rows.stream().anyMatch(row -> row == null)) throw new IllegalArgumentException("Invalid occupancy row");
        rows.sort(Comparator.comparing(DailyOccupancySample::date));
        int hotel = rows.getFirst().hotelId();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).hotelId() != hotel || (i > 0 && !rows.get(i - 1).date().plusDays(1).equals(rows.get(i).date()))) {
                throw new IllegalArgumentException("Invalid occupancy calendar or scope");
            }
        }
        return List.copyOf(rows);
    }

    static double target(DailyOccupancySample row) { return row.occupancyPercent().orElse(Double.NaN); }
    static int usable(List<DailyOccupancySample> rows) { return (int) rows.stream().filter(row -> Double.isFinite(target(row))).count(); }

    static boolean trainable(List<DailyOccupancySample> rows) {
        return rows.size() >= MIN_TRAIN && usable(rows) >= MIN_TRAIN
                && rows.subList(rows.size() - 7, rows.size()).stream().allMatch(row -> Double.isFinite(target(row)));
    }

    static Fit seasonal(List<DailyOccupancySample> train) {
        if (!trainable(train)) throw new IllegalArgumentException("Insufficient occupancy training coverage");
        double[] weekday = new double[7];
        for (DailyOccupancySample row : train.subList(train.size() - 7, train.size())) {
            weekday[row.date().getDayOfWeek().getValue() - 1] = target(row);
        }
        return date -> weekday[date.getDayOfWeek().getValue() - 1];
    }

    static Fit ridge(List<DailyOccupancySample> train) {
        if (!trainable(train)) throw new IllegalArgumentException("Insufficient occupancy training coverage");
        LocalDate start = train.getFirst().date();
        double[][] normal = new double[8][9];
        for (DailyOccupancySample row : train) {
            double y = target(row);
            if (!Double.isFinite(y)) continue;
            double[] x = features(start, row.date());
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) normal[i][j] += x[i] * x[j];
                normal[i][8] += x[i] * y;
            }
        }
        for (int i = 1; i < 8; i++) normal[i][i] += 1.0; // lambda=1, intercept unpenalized.
        double[] coefficients = solve(normal);
        return date -> {
            double[] x = features(start, date);
            double result = 0;
            for (int i = 0; i < 8; i++) result += coefficients[i] * x[i];
            return clamp(result);
        };
    }

    private static double[] features(LocalDate start, LocalDate date) {
        double[] x = new double[8];
        x[0] = 1;
        x[1] = ChronoUnit.DAYS.between(start, date) / 180.0;
        int weekday = date.getDayOfWeek().getValue() - 1;
        if (weekday > 0) x[weekday + 1] = 1;
        return x;
    }

    private static double[] solve(double[][] a) {
        for (int column = 0; column < 8; column++) {
            int pivot = column;
            for (int row = column + 1; row < 8; row++) {
                if (Math.abs(a[row][column]) > Math.abs(a[pivot][column])) pivot = row;
            }
            if (Math.abs(a[pivot][column]) < 1e-10) throw new IllegalArgumentException("Unstable occupancy fit");
            double[] swap = a[column]; a[column] = a[pivot]; a[pivot] = swap;
            double divisor = a[column][column];
            for (int j = column; j <= 8; j++) a[column][j] /= divisor;
            for (int row = 0; row < 8; row++) {
                if (row == column) continue;
                double factor = a[row][column];
                for (int j = column; j <= 8; j++) a[row][j] -= factor * a[column][j];
            }
        }
        double[] result = new double[8];
        for (int i = 0; i < 8; i++) {
            result[i] = a[i][8];
            if (!Double.isFinite(result[i])) throw new IllegalArgumentException("Invalid occupancy fit");
        }
        return result;
    }

    static double p90(List<Double> absoluteErrors) {
        if (absoluteErrors.isEmpty() || absoluteErrors.stream().anyMatch(value -> !Double.isFinite(value) || value < 0)) {
            throw new IllegalArgumentException("Invalid occupancy residuals");
        }
        List<Double> sorted = absoluteErrors.stream().sorted().toList();
        return sorted.get((int) Math.ceil(0.9 * sorted.size()) - 1);
    }

    static List<Double> weeklyErrors(List<DailyOccupancySample> rows) {
        List<Double> errors = new ArrayList<>();
        for (int i = 7; i < rows.size(); i++) {
            double actual = target(rows.get(i));
            double previous = target(rows.get(i - 7));
            if (Double.isFinite(actual) && Double.isFinite(previous)) errors.add(Math.abs(actual - previous));
        }
        return errors;
    }

    static double clamp(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid occupancy prediction");
        return Math.max(0, Math.min(100, value));
    }
}
