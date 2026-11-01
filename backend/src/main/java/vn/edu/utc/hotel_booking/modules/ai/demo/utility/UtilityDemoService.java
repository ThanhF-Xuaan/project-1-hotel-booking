package vn.edu.utc.hotel_booking.modules.ai.demo.utility;

import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.UtilityAnomalyResponse;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.UtilityAnomalyResponse.PointStatus;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.UtilityAnomalyResponse.Status;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample.Marker;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.UtilityReadingSample.Unit;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingAuthorizationPort.Scope;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingHistoryPort.Batch;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.UtilityReadingHistoryPort.Coverage;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.UtilityAnomalyDetectionService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Fixed-clock synthetic replay only; never a live T04 adapter or business-data fallback. */
public final class UtilityDemoService {
    private static final int MAX_BYTES = 128_000;
    private static final Instant REFERENCE = Instant.parse("2026-10-30T17:00:00Z");
    private static final LocalDate FROM = LocalDate.of(2026, 9, 19), TO = LocalDate.of(2026, 10, 30);
    private record Dataset(Scope scope, List<UtilityReadingSample> readings) { }
    private final Map<Scenario, Dataset> datasets;

    private enum Scenario {
        ELECTRIC_FLAT("Điện: ổn định", Unit.KWH), WATER_FLAT("Nước: ổn định", Unit.M3),
        ELECTRIC_SPIKES("Điện: tăng và giảm đột biến", Unit.KWH), WATER_SPIKES("Nước: tăng và giảm đột biến", Unit.M3),
        ZERO_BOUNDARY("Điện: ngưỡng tiêu thụ gần 0", Unit.KWH), MAD_NOISE("Điện: dao động nền", Unit.KWH),
        GAP("Nước: thiếu ngày ghi chỉ số", Unit.M3), RESET("Điện: đặt lại đồng hồ", Unit.KWH),
        REPLACEMENT("Nước: thay đồng hồ", Unit.M3), NEGATIVE_DROP("Điện: chỉ số giảm bất thường", Unit.KWH),
        LOW_COVERAGE("Nước: chưa đủ dữ liệu", Unit.M3), SLOW_VARIATION("Điện: thay đổi chậm", Unit.KWH);
        final String label;
        final Unit unit;
        Scenario(String label, Unit unit) { this.label = label; this.unit = unit; }
    }

    public UtilityDemoService() { this(readCorpus()); }

    UtilityDemoService(byte[] raw) {
        try {
            if (raw == null || raw.length > MAX_BYTES || !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw))
                    .equals("f851a52a1fa293c5dfb4860edb0becf55b0b1479f5eec9a9d83c1ff6e11e8dca")) throw invalidCorpus();
            List<String> lines = new String(raw, StandardCharsets.UTF_8).lines().toList();
            if (lines.size() != 517 || !lines.getFirst().equals("seriesId,hotelId,meterId,unit,counterEpoch,date,cumulativeValue,marker")) throw invalidCorpus();
            Map<Scenario, List<String[]>> groups = new EnumMap<>(Scenario.class);
            for (String line : lines.subList(1, lines.size())) {
                String[] cells = line.split(",", -1);
                if (cells.length != 8) throw invalidCorpus();
                groups.computeIfAbsent(Scenario.valueOf(cells[0].toUpperCase(Locale.ROOT)), ignored -> new ArrayList<>()).add(cells);
            }
            Map<Scenario, Dataset> parsed = new EnumMap<>(Scenario.class);
            for (Scenario scenario : Scenario.values()) {
                List<String[]> rows = groups.get(scenario);
                if (rows == null || rows.size() != 43) throw invalidCorpus();
                var first = rows.getFirst();
                int hotel = Integer.parseInt(first[1]), meter = Integer.parseInt(first[2]);
                var scope = new Scope(hotel, hotel, meter, scenario.unit);
                var readings = new ArrayList<UtilityReadingSample>();
                for (int i = 0; i < rows.size(); i++) {
                    var cells = rows.get(i);
                    LocalDate date = LocalDate.parse(cells[5]);
                    Marker marker = Marker.valueOf(cells[7]);
                    if (Integer.parseInt(cells[1]) != hotel || Integer.parseInt(cells[2]) != meter
                            || Unit.valueOf(cells[3]) != scenario.unit || !date.equals(FROM.minusDays(1).plusDays(i))) throw invalidCorpus();
                    if (!cells[6].isEmpty()) readings.add(new UtilityReadingSample(hotel, meter, scenario.unit, date,
                            new BigDecimal(cells[6]), cells[4], marker));
                }
                parsed.put(scenario, new Dataset(scope, List.copyOf(readings)));
            }
            datasets = Map.copyOf(parsed);
        } catch (Exception exception) { throw invalidCorpus(); }
    }

    private static byte[] readCorpus() {
        try (var input = new ClassPathResource("ai/demo/utility-readings-v1.csv").getInputStream()) {
            return input.readNBytes(MAX_BYTES + 1);
        } catch (Exception exception) { throw invalidCorpus(); }
    }
    private static IllegalStateException invalidCorpus() { return new IllegalStateException("Synthetic utility demo corpus unavailable"); }

    public DemoResponse replay(String id) {
        requireAdmin();
        Scenario scenario;
        try { scenario = Scenario.valueOf(id == null ? "" : id); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Invalid demo scenario"); }
        Dataset data = datasets.get(scenario);
        var engine = new UtilityAnomalyDetectionService((hotel, meter) -> isAdmin() && hotel == data.scope().hotelId() && meter == data.scope().meterId()
                ? Optional.of(data.scope()) : Optional.empty(), query -> {
                    if (!query.scope().equals(data.scope()) || !query.fromInclusive().equals(FROM) || !query.toInclusive().equals(TO)
                            || !query.asOf().equals(REFERENCE)) throw new IllegalStateException("Invalid demo scope");
                    return new Batch(data.scope(), "SYNTHETIC", "SYNTHETIC_CUMULATIVE_DAILY_V1", Coverage.COMPLETE, data.readings());
                }, Clock.fixed(REFERENCE, ZoneOffset.UTC));
        UtilityAnomalyResponse result = engine.analyze(data.scope().hotelId(), data.scope().meterId(), FROM, TO);
        requireAdmin();
        if (result.status() != Status.ANALYZED && result.status() != Status.INSUFFICIENT_COVERAGE) throw new IllegalStateException("Demo analysis unavailable");
        var counts = new Counts(result.validPoints(), result.evaluatedPoints(),
                result.points().stream().filter(point -> point.status() == PointStatus.NEEDS_REVIEW).count(), result.excludedPoints(), result.insufficientPoints());
        List<ScenarioView> choices = Arrays.stream(Scenario.values()).map(value -> new ScenarioView(value.name(), value.label, value.unit)).toList();
        return new DemoResponse("SYNTHETIC", false, scenario.name(),
                "Dữ liệu mô phỏng theo mốc cố định; nhãn Cần kiểm tra không phải kết luận gian lận hoặc cảnh báo của đồng hồ thật.",
                REFERENCE, choices, result, counts);
    }

    private boolean isAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CHAIN_ADMIN".equals(authority.getAuthority()));
    }
    private void requireAdmin() { if (!isAdmin()) throw new AccessDeniedException("Demo requires chain administrator"); }
    public record ScenarioView(String id, String label, Unit unit) { }
    public record Counts(long valid, long evaluated, long needsReview, long excluded, long insufficient) { }
    public record DemoResponse(String dataMode, boolean businessData, String scenario, String notice, Instant referenceInstant,
                               List<ScenarioView> scenarios, UtilityAnomalyResponse analysis, Counts counts) { }
}
