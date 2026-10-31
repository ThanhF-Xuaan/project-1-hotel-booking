package vn.edu.utc.hotel_booking.modules.ai.forecast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample.Coverage;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch.DataMode;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryResult.Status;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryAuthorizationPort.AuthorizedHotelScope;
import vn.edu.utc.hotel_booking.modules.ai.forecast.service.OccupancyHistoryService;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class OccupancyHistoryFixtureTest {
    private static final String RESOURCE = "/ai/u22/occupancy-history-420-days.json";
    private static final String FROZEN_SHA256 = "8c1f3e81019238948be47a61c49ae4821b92752381a7d99d70216e8437651c20";

    @Test
    void frozenFixtureIsExplicitlySyntheticAndContainsTwoScoped420DaySeries() throws Exception {
        byte[] bytes;
        try (var input = getClass().getResourceAsStream(RESOURCE)) {
            assertThat(input).isNotNull();
            bytes = input.readAllBytes();
        }
        assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))).isEqualTo(FROZEN_SHA256);
        JsonNode root = new ObjectMapper().readTree(bytes);
        assertThat(root.get("schemaVersion").asText()).isEqualTo("u22-occupancy-local-v1");
        assertThat(root.get("dataMode").asText()).isEqualTo("SYNTHETIC");
        assertThat(root.get("description").asText()).contains("No real hotels, guests or bookings");
        assertThat(root.get("hotels")).hasSize(2);
        for (JsonNode hotel : root.get("hotels")) {
            assertThat(hotel.get("samples")).hasSize(420);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void fixturePortPreservesPerDayCapacityGapsAndProvenanceWithoutCrossHotelData(int hotelId) throws Exception {
        JsonNode root = fixture();
        JsonNode hotel = root.get("hotels").get(hotelId - 1);
        List<DailyOccupancySample> original = samples(hotel);
        List<DailyOccupancySample> shuffled = new ArrayList<>(original);
        Collections.shuffle(shuffled, new Random(20261030));
        int region = hotel.get("regionId").asInt();
        Instant now = Instant.parse(root.get("referenceInstant").asText());
        var service = new OccupancyHistoryService(
                id -> id == hotelId ? Optional.of(new AuthorizedHotelScope(id, region)) : Optional.empty(),
                query -> {
                    assertThat(query.scope()).isEqualTo(new AuthorizedHotelScope(hotelId, region));
                    assertThat(query.toUtcExclusive()).isEqualTo(now);
                    return new OccupancyHistoryBatch(DataMode.SYNTHETIC, shuffled);
                }, Clock.fixed(now, ZoneOffset.UTC));
        var result = service.read(hotelId, LocalDate.parse(root.get("fromInclusive").asText()),
                LocalDate.parse(root.get("toInclusive").asText()));
        assertThat(result.status()).isEqualTo(Status.PARTIAL);
        assertThat(result.dataMode()).isEqualTo(DataMode.SYNTHETIC);
        assertThat(result.samples()).containsExactlyElementsOf(original);
        assertThat(result.samples()).allSatisfy(sample -> assertThat(sample.hotelId()).isEqualTo(hotelId));
        assertThat(result.completeDays()).isEqualTo(418);
        assertThat(result.usableDays()).isEqualTo(417);
        assertThat(result.samples().get(59).capacityRooms()).isZero();
        assertThat(result.samples().get(59).occupancyPercent()).isEmpty();
        assertThat(result.samples().get(123).coverage()).isEqualTo(Coverage.MISSING);
        assertThat(result.samples().get(321).coverage()).isEqualTo(Coverage.MISSING);
        assertThat(result.samples().get(139).capacityRooms()).isNotEqualTo(result.samples().get(140).capacityRooms());
        assertThat(result.samples().get(279).capacityRooms()).isNotEqualTo(result.samples().get(280).capacityRooms());
        var denied = service.read(hotelId == 1 ? 2 : 1, result.fromInclusive(), result.toInclusive());
        assertThat(denied.status()).isEqualTo(Status.ACCESS_DENIED);
        assertThat(denied.samples()).isEmpty();
    }

    private JsonNode fixture() throws Exception {
        try (var input = getClass().getResourceAsStream(RESOURCE)) {
            assertThat(input).isNotNull();
            return new ObjectMapper().readTree(input);
        }
    }

    private List<DailyOccupancySample> samples(JsonNode hotel) {
        List<DailyOccupancySample> result = new ArrayList<>();
        for (JsonNode row : hotel.get("samples")) {
            result.add(new DailyOccupancySample(row.get("hotelId").asInt(), LocalDate.parse(row.get("date").asText()),
                    row.get("occupiedRooms").isNull() ? null : row.get("occupiedRooms").asInt(),
                    row.get("capacityRooms").isNull() ? null : row.get("capacityRooms").asInt(),
                    Coverage.valueOf(row.get("coverage").asText())));
        }
        return result;
    }
}
