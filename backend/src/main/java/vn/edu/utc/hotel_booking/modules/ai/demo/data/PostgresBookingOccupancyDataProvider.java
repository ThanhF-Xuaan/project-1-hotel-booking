package vn.edu.utc.hotel_booking.modules.ai.demo.data;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample.Coverage;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.OccupancyHistoryBatch.DataMode;
import vn.edu.utc.hotel_booking.modules.ai.forecast.port.OccupancyHistoryPort;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.Snapshot;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Operational PostgreSQL provider that queries real room capacity from room_instances
 * and daily occupied rooms from bookings and booking_daily_rates.
 */
public final class PostgresBookingOccupancyDataProvider implements DemoDataProvider {
    private final DataSource dataSource;
    private final Clock clock;
    private final FixtureDemoDataProvider fallbackFixture = new FixtureDemoDataProvider();

    public PostgresBookingOccupancyDataProvider(DataSource dataSource) {
        this(dataSource, Clock.system(OccupancyHistoryPort.HOTEL_ZONE));
    }

    public PostgresBookingOccupancyDataProvider(DataSource dataSource, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource required");
        this.clock = Objects.requireNonNull(clock, "clock required");
    }

    private static void requireAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(a -> "ROLE_CHAIN_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Demo requires chain administrator");
        }
    }

    private static int resolveHotelId(String scenario) {
        if ("HOTEL_A".equals(scenario)) return 1;
        if ("HOTEL_B".equals(scenario)) return 2;
        if ("HOTEL_C".equals(scenario)) return 3;
        try {
            int parsed = Integer.parseInt(scenario);
            if (parsed >= 1) return parsed;
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException("Invalid scenario selector: " + scenario);
    }

    @Override
    public <T> T occupancy(String scenario, Function<OccupancyData, T> action) {
        requireAdmin();
        int hotelId = resolveHotelId(scenario);

        Instant reference = clock.instant();
        LocalDate today = reference.atZone(OccupancyHistoryPort.HOTEL_ZONE).toLocalDate();
        LocalDate toInclusive = today.minusDays(1);
        // 240 days window satisfies OccupancyModels.MIN_TRAIN (84) and ForecastBacktestEvaluator (234)
        LocalDate fromInclusive = toInclusive.minusDays(239);

        try (Connection connection = dataSource.getConnection()) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);

            int regionId = 20;
            try (PreparedStatement hotelStmt = connection.prepareStatement(
                    "SELECT region_id FROM hotels WHERE id = ? AND is_deleted = false")) {
                hotelStmt.setInt(1, hotelId);
                try (ResultSet rs = hotelStmt.executeQuery()) {
                    if (rs.next()) {
                        regionId = rs.getInt(1);
                    }
                }
            }

            int capacity = 0;
            try (PreparedStatement capStmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM room_instances WHERE hotel_id = ? AND is_deleted = false")) {
                capStmt.setInt(1, hotelId);
                try (ResultSet rs = capStmt.executeQuery()) {
                    if (rs.next()) {
                        capacity = rs.getInt(1);
                    }
                }
            }
            if (capacity <= 0) {
                capacity = 10;
            }

            Map<LocalDate, Integer> occupiedMap = new HashMap<>();
            String sql = """
                    SELECT bdr.stay_date, COUNT(DISTINCT br.id) AS occupied_rooms
                    FROM booking_daily_rates bdr
                    JOIN booking_rooms br ON bdr.booking_room_id = br.id
                    JOIN booking_details bd ON br.booking_detail_id = bd.id
                    JOIN bookings b ON bd.booking_id = b.id
                    WHERE b.hotel_id = ?
                      AND b.status != 'CANCELLED'
                      AND br.status != 'CANCELLED'
                      AND bdr.stay_date BETWEEN ? AND ?
                    GROUP BY bdr.stay_date
                    """;
            try (PreparedStatement rateStmt = connection.prepareStatement(sql)) {
                rateStmt.setInt(1, hotelId);
                rateStmt.setDate(2, Date.valueOf(fromInclusive));
                rateStmt.setDate(3, Date.valueOf(toInclusive));
                try (ResultSet rs = rateStmt.executeQuery()) {
                    while (rs.next()) {
                        LocalDate d = rs.getDate(1).toLocalDate();
                        int count = rs.getInt(2);
                        occupiedMap.put(d, count);
                    }
                }
            }

            connection.commit();

            List<DailyOccupancySample> samples = new ArrayList<>(240);
            LocalDate cur = fromInclusive;
            while (!cur.isAfter(toInclusive)) {
                int occupied = Math.min(occupiedMap.getOrDefault(cur, 0), capacity);
                samples.add(new DailyOccupancySample(hotelId, cur, occupied, capacity, Coverage.COMPLETE));
                cur = cur.plusDays(1);
            }

            return action.apply(new OccupancyData(hotelId, regionId, reference, samples, DataMode.AUTHORITATIVE));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to query operational occupancy from PostgreSQL", e);
        }
    }

    @Override
    public <T> T group(String sourceId, Function<GroupData, T> action) {
        return fallbackFixture.group(sourceId, action);
    }
}
