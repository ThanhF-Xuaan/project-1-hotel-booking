package vn.edu.utc.hotel_booking.modules.ai.demo.forecast;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryResult;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryAuthorizationPort.AuthorizedHotelScope;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.ForecastBacktestEvaluator;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.OccupancyForecastService;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.OccupancyHistoryService;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Isolated fixed-clock replay. Never a fallback or an authoritative history provider. */
public final class OccupancyDemoService {
    private record Dataset(int hotelId, int regionId, List<DailyOccupancySample> rows) { }
    private final Map<String, Dataset> scenarios;
    private final Instant reference;

    public OccupancyDemoService() {
        try (var input = new ClassPathResource("ai/demo/occupancy-history-v1.json").getInputStream()) {
            byte[] raw = input.readNBytes(512_001);
            if (raw.length > 512_000 || !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw))
                    .equals("8c1f3e81019238948be47a61c49ae4821b92752381a7d99d70216e8437651c20")) {
                throw new IllegalStateException("Invalid synthetic demo corpus");
            }
            var json = new ObjectMapper().readTree(raw);
            if (!"SYNTHETIC".equals(json.path("dataMode").asText()) || json.path("hotels").size() != 2) {
                throw new IllegalStateException("Invalid synthetic demo corpus");
            }
            reference = Instant.parse(json.path("referenceInstant").asText());
            var values = new ArrayList<Dataset>();
            for (var hotel : json.path("hotels")) {
                var rows = new ArrayList<DailyOccupancySample>();
                for (var row : hotel.path("samples")) {
                    rows.add(new DailyOccupancySample(row.path("hotelId").asInt(), LocalDate.parse(row.path("date").asText()),
                            row.path("occupiedRooms").isNull() ? null : row.path("occupiedRooms").asInt(),
                            row.path("capacityRooms").isNull() ? null : row.path("capacityRooms").asInt(),
                            DailyOccupancySample.Coverage.valueOf(row.path("coverage").asText())));
                }
                values.add(new Dataset(hotel.path("hotelId").asInt(), hotel.path("regionId").asInt(), List.copyOf(rows)));
            }
            scenarios = Map.of("HOTEL_A", values.get(0), "HOTEL_B", values.get(1));
        } catch (Exception exception) {
            throw new IllegalStateException("Synthetic occupancy demo corpus unavailable");
        }
    }

    public DemoResponse replay(String scenario) {
        requireAdmin();
        Dataset data = scenarios.get(scenario == null ? "" : scenario);
        if (data == null) throw new IllegalArgumentException("Invalid demo scenario");
        var scope = new AuthorizedHotelScope(data.hotelId(), data.regionId());
        var history = new OccupancyHistoryService(id -> isAdmin() && id == data.hotelId() ? Optional.of(scope) : Optional.empty(),
                query -> {
                    if (!query.scope().equals(scope)) throw new IllegalArgumentException("Invalid demo scope");
                    return new OccupancyHistoryBatch(OccupancyHistoryBatch.DataMode.SYNTHETIC, data.rows());
                }, Clock.fixed(reference, ZoneOffset.UTC));
        var from = data.rows().getFirst().date();
        var to = data.rows().getLast().date();
        var observed = history.read(data.hotelId(), from, to);
        var forecast = new OccupancyForecastService(history, new ForecastBacktestEvaluator()).forecast(data.hotelId(), from, to);
        requireAdmin();
        return new DemoResponse("SYNTHETIC", false, scenario,
                "Dữ liệu mô phỏng theo mốc cố định; không phải lịch sử hoặc dự báo của khách sạn thật.", reference, observed, forecast);
    }

    private boolean isAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CHAIN_ADMIN".equals(authority.getAuthority()));
    }

    private void requireAdmin() {
        if (!isAdmin()) throw new AccessDeniedException("Demo requires chain administrator");
    }

    public record DemoResponse(String dataMode, boolean businessData, String scenario, String notice,
                               Instant referenceInstant, OccupancyHistoryResult history, OccupancyForecastResponse forecast) { }
}
