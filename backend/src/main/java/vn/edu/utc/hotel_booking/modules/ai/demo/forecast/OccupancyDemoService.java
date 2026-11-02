package vn.edu.utc.hotel_booking.modules.ai.demo.forecast;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryResult;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryAuthorizationPort.AuthorizedHotelScope;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.ForecastBacktestEvaluator;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.OccupancyForecastService;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.OccupancyHistoryService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/** Isolated fixed-clock replay. Never a fallback or an authoritative history provider. */
public final class OccupancyDemoService {
    private final vn.edu.utc.hotel_booking.modules.ai.demo.data.DemoDataProvider provider;
    public OccupancyDemoService() { this(new vn.edu.utc.hotel_booking.modules.ai.demo.data.FixtureDemoDataProvider()); }
    public OccupancyDemoService(vn.edu.utc.hotel_booking.modules.ai.demo.data.DemoDataProvider provider) { this.provider=provider; }

    public DemoResponse replay(String scenario) {
        requireAdmin();
        if (!"HOTEL_A".equals(scenario) && !"HOTEL_B".equals(scenario)) throw new IllegalArgumentException("Invalid demo scenario");
        return provider.occupancy(scenario, data -> replayData(scenario,data));
    }
    private DemoResponse replayData(String scenario, vn.edu.utc.hotel_booking.modules.ai.demo.data.DemoDataProvider.OccupancyData data) {
        var reference=data.reference();
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
