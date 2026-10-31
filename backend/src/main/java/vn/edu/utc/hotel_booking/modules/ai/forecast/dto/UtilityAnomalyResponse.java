package vn.edu.utc.hotel_booking.modules.ai.forecast.dto;

import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample.Unit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record UtilityAnomalyResponse(Status status, String reasonCode, Integer hotelId, Integer meterId, Unit unit,
                                     String dataMode, String modelVersion, Instant asOf, LocalDate fromInclusive,
                                     LocalDate toInclusive, List<Point> points) {
    public enum Status { ANALYZED, INSUFFICIENT_COVERAGE, INVALID_REQUEST, ACCESS_DENIED, DEPENDENCY_UNAVAILABLE }
    public enum PointStatus { NORMAL, NEEDS_REVIEW, EXCLUDED, INSUFFICIENT_COVERAGE }
    public record Point(LocalDate date, PointStatus status, String reasonCode, BigDecimal usage, int referenceCount,
                        BigDecimal median, BigDecimal mad, BigDecimal threshold, String label) { }

    public UtilityAnomalyResponse {
        points = List.copyOf(points);
        if (status != Status.ANALYZED && status != Status.INSUFFICIENT_COVERAGE && !points.isEmpty()) {
            throw new IllegalArgumentException("Failed anomaly response must not contain points");
        }
    }
    public long evaluatedPoints() { return points.stream().filter(p -> p.status() == PointStatus.NORMAL || p.status() == PointStatus.NEEDS_REVIEW).count(); }
    public long validPoints() { return points.stream().filter(p -> p.usage() != null).count(); }
    public long excludedPoints() { return points.stream().filter(p -> p.status() == PointStatus.EXCLUDED).count(); }
    public long insufficientPoints() { return points.stream().filter(p -> p.status() == PointStatus.INSUFFICIENT_COVERAGE).count(); }
}
