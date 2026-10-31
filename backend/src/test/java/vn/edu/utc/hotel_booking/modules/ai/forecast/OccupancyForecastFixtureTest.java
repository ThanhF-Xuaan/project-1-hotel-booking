package vn.edu.utc.hotel_booking.modules.ai.forecast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.EvaluationStatus;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.Model;
import vn.edu.utc.hotel_booking.modules.ai.forecast.dto.OccupancyForecastResponse.Status;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch.DataMode;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OccupancyForecastFixtureTest {
    private static final String FIXTURE = "/ai/u22/occupancy-history-420-days.json";
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void realHistoryBoundaryAndForecastEvaluateFrozenSyntheticCorpus(int hotelId) throws Exception {
        JsonNode fixture = fixture();
        var rows = rows(fixture.get("hotels").get(hotelId - 1));
        int region = fixture.get("hotels").get(hotelId - 1).get("regionId").asInt();
        AtomicInteger authorizationReads = new AtomicInteger();
        var history = new OccupancyHistoryService(id -> {
            authorizationReads.incrementAndGet();
            return id == hotelId ? Optional.of(new AuthorizedHotelScope(id, region)) : Optional.empty();
        }, query -> {
            assertThat(query.scope()).isEqualTo(new AuthorizedHotelScope(hotelId, region));
            return new OccupancyHistoryBatch(DataMode.SYNTHETIC, rows);
        }, Clock.fixed(Instant.parse(fixture.get("referenceInstant").asText()), ZoneOffset.UTC));
        var service = new OccupancyForecastService(history, new ForecastBacktestEvaluator());
        var response = service.forecast(hotelId, rows.getFirst().date(), rows.getLast().date());
        assertThat(response.status()).isEqualTo(Status.FORECAST_READY);
        assertThat(response.dataMode()).isEqualTo(DataMode.SYNTHETIC);
        assertThat(response.hotelId()).isEqualTo(hotelId);
        assertThat(response.points()).hasSize(30);
        assertThat(response.points().getFirst().date()).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(response.points().getLast().date()).isEqualTo(LocalDate.of(2026, 11, 28));
        assertThat(response.backtest().status()).isEqualTo(EvaluationStatus.COMPLETE);
        assertThat(response.backtest().scoredDays()).isEqualTo(149);
        assertThat(response.backtest().folds()).hasSize(5);
        boolean candidatePassed = ForecastBacktestEvaluator.candidatePassesGate(response.backtest().baselineMaePp(),
                response.backtest().candidateMaePp(), response.backtest().folds());
        assertThat(response.model()).isEqualTo(candidatePassed ? Model.RIDGE_TREND_WEEKDAY : Model.SEASONAL_NAIVE_7);
        assertThat(authorizationReads).hasValue(2);
        var denied = service.forecast(hotelId == 1 ? 2 : 1, rows.getFirst().date(), rows.getLast().date());
        assertThat(denied.status()).isEqualTo(Status.ACCESS_DENIED);
        assertThat(denied.points()).isEmpty();
        System.out.println("U22_SYNTHETIC_REPORT " + mapper.writeValueAsString(Map.of("hotelId", hotelId, "response", response)));
    }

    @Test
    void revocationDuringHistoryReadPreventsAnyForecast() throws Exception {
        JsonNode fixture = fixture();
        var rows = rows(fixture.get("hotels").get(0));
        AtomicInteger reads = new AtomicInteger();
        var history = new OccupancyHistoryService(id -> reads.getAndIncrement() == 0
                ? Optional.of(new AuthorizedHotelScope(1, 10)) : Optional.empty(),
                query -> new OccupancyHistoryBatch(DataMode.SYNTHETIC, rows),
                Clock.fixed(Instant.parse(fixture.get("referenceInstant").asText()), ZoneOffset.UTC));
        var response = new OccupancyForecastService(history, new ForecastBacktestEvaluator())
                .forecast(1, rows.getFirst().date(), rows.getLast().date());
        assertThat(response.status()).isEqualTo(Status.ACCESS_DENIED);
        assertThat(response.points()).isEmpty();
        assertThat(response.dataMode()).isNull();
    }

    private JsonNode fixture() throws Exception {
        try (var input = getClass().getResourceAsStream(FIXTURE)) {
            assertThat(input).isNotNull();
            byte[] bytes = input.readAllBytes();
            assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
                    .isEqualTo("8c1f3e81019238948be47a61c49ae4821b92752381a7d99d70216e8437651c20");
            return mapper.readTree(bytes);
        }
    }

    private List<DailyOccupancySample> rows(JsonNode hotel) {
        List<DailyOccupancySample> rows = new ArrayList<>();
        for (JsonNode row : hotel.get("samples")) {
            rows.add(new DailyOccupancySample(row.get("hotelId").asInt(), LocalDate.parse(row.get("date").asText()),
                    row.get("occupiedRooms").isNull() ? null : row.get("occupiedRooms").asInt(),
                    row.get("capacityRooms").isNull() ? null : row.get("capacityRooms").asInt(),
                    DailyOccupancySample.Coverage.valueOf(row.get("coverage").asText())));
        }
        return rows;
    }
}
