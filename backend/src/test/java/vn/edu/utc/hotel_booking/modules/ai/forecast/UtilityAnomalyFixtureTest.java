package vn.edu.utc.hotel_booking.modules.ai.forecast;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.UtilityAnomalyResponse.PointStatus;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample.*;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingAuthorizationPort.Scope;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingHistoryPort.*;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.UtilityAnomalyDetectionService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class UtilityAnomalyFixtureTest {
    record Row(String series, int hotel, int meter, Unit unit, String epoch, LocalDate date,
               BigDecimal raw, Marker marker, String expected) { }

    @Test
    void lockedSyntheticLabelsMeetRecallFalsePositiveAndQualityCoverageGates() throws Exception {
        byte[] bytes;
        try (var input = getClass().getResourceAsStream("/ai/u22/utility-readings-42-days.csv")) {
            assertThat(input).isNotNull(); bytes = input.readAllBytes();
        }
        assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
                .isEqualTo("48078f6afa5d34ac6c3bf0292813486d33bf10cdc15397085ddd8223b5bb9638");
        List<String> lines = new String(bytes, StandardCharsets.UTF_8).lines().toList();
        assertThat(lines.getFirst()).isEqualTo("seriesId,hotelId,meterId,unit,counterEpoch,date,cumulativeValue,marker,expected");
        Map<String, List<Row>> groups = new TreeMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] c = line.split(",", -1); assertThat(c).hasSize(9);
            Row row = new Row(c[0], Integer.parseInt(c[1]), Integer.parseInt(c[2]), Unit.valueOf(c[3]), c[4], LocalDate.parse(c[5]),
                    c[6].isEmpty() ? null : new BigDecimal(c[6]), Marker.valueOf(c[7]), c[8]);
            groups.computeIfAbsent(row.series(), ignored -> new ArrayList<>()).add(row);
        }
        assertThat(groups).hasSize(12);
        int tp = 0, fn = 0, fp = 0, tn = 0, unevaluatedNormals = 0, unevaluatedAnomalies = 0, warmup = 0, excluded = 0, anchors = 0, qualityErrors = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        for (var entry : groups.entrySet()) {
            List<Row> rows = entry.getValue(); assertThat(rows).hasSize(43);
            Row first = rows.getFirst();
            Scope scope = new Scope(first.hotel(), first.hotel(), first.meter(), first.unit());
            var samples = rows.stream().filter(row -> row.raw() != null).map(row -> new UtilityReadingSample(row.hotel(), row.meter(), row.unit(),
                    row.date(), row.raw(), row.epoch(), row.marker())).toList();
            var service = new UtilityAnomalyDetectionService((hotel, meter) -> hotel == scope.hotelId() && meter == scope.meterId() ? Optional.of(scope) : Optional.empty(),
                    query -> new Batch(scope, "SYNTHETIC", "SYNTHETIC_CUMULATIVE_DAILY_V1", Coverage.COMPLETE, samples),
                    Clock.fixed(Instant.parse("2026-10-30T17:00:00Z"), ZoneOffset.UTC));
            var response = service.analyze(scope.hotelId(), scope.meterId(), rows.get(1).date(), rows.getLast().date());
            assertThat(response.dataMode()).isEqualTo("SYNTHETIC"); assertThat(response.points()).hasSize(42);
            assertThat(response.unit()).isEqualTo(scope.unit());
            for (Row row : rows) {
                if (row.expected().equals("ANCHOR")) { anchors++; continue; }
                var point = response.points().stream().filter(p -> p.date().equals(row.date())).findFirst().orElseThrow();
                boolean scored = point.status() == PointStatus.NORMAL || point.status() == PointStatus.NEEDS_REVIEW;
                switch (row.expected()) {
                    case "ANOMALY" -> { if (point.status() == PointStatus.NEEDS_REVIEW) tp++; else fn++; if (!scored) unevaluatedAnomalies++; }
                    case "NORMAL" -> { if (!scored) unevaluatedNormals++; else if (point.status() == PointStatus.NEEDS_REVIEW) fp++; else tn++; }
                    case "WARMUP" -> { warmup++; if (point.status() != PointStatus.INSUFFICIENT_COVERAGE) qualityErrors++; }
                    case "EXCLUDED" -> { excluded++; if (point.status() != PointStatus.EXCLUDED || point.usage() != null) qualityErrors++; }
                    default -> throw new AssertionError("Unknown frozen label");
                }
            }
            details.add(Map.of("seriesId", entry.getKey(), "unit", scope.unit().name(), "status", response.status().name(),
                    "valid", response.validPoints(), "evaluated", response.evaluatedPoints(), "excluded", response.excludedPoints(), "insufficient", response.insufficientPoints()));
        }
        assertThat(tp + fn).isGreaterThan(0); assertThat(fp + tn).isGreaterThan(0);
        double recall = (double) tp / (tp + fn), fpr = (double) fp / (fp + tn);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("dataMode", "SYNTHETIC"); metrics.put("tp", tp); metrics.put("fn", fn); metrics.put("fp", fp); metrics.put("tn", tn);
        metrics.put("recall", recall); metrics.put("falsePositiveRate", fpr); metrics.put("unevaluatedNormals", unevaluatedNormals);
        metrics.put("unevaluatedAnomalies", unevaluatedAnomalies); metrics.put("qualityErrors", qualityErrors);
        metrics.put("warmup", warmup); metrics.put("excluded", excluded); metrics.put("anchors", anchors); metrics.put("series", details);
        System.out.println("U22_UTILITY_SYNTHETIC_EVALUATION " + new ObjectMapper().writeValueAsString(metrics));
        assertThat(recall).isGreaterThanOrEqualTo(0.90); assertThat(fpr).isLessThanOrEqualTo(0.05);
        assertThat(unevaluatedNormals).isZero(); assertThat(qualityErrors).isZero();
        assertThat(warmup).isEqualTo(348); assertThat(excluded).isEqualTo(28); assertThat(anchors).isEqualTo(12);
    }
}
