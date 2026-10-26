package vn.edu.utc.hotel_booking.modules.booking.personalization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.data.jpa.repository.Query;
import vn.edu.utc.hotel_booking.modules.booking.repository.PersonalizationHistoryQueryRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.PersonalizationCatalogQueryRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Executes the production native SQL against a disposable PostgreSQL schema. */
@EnabledIfEnvironmentVariable(named = "U14_I03_IT", matches = "true")
class PersonalizationProviderPostgresIT {

    private String jdbcUrl;
    private String username;
    private String password;
    private String schema;
    private JdbcTemplate jdbc;
    private NamedParameterJdbcTemplate namedJdbc;

    @BeforeEach
    void createDisposableSchema() throws Exception {
        jdbcUrl = requiredEnvironmentValue("U14_I03_JDBC_URL");
        username = requiredEnvironmentValue("U14_I03_DB_USER");
        password = requiredEnvironmentValue("U14_I03_DB_PASSWORD");
        if (!jdbcUrl.matches("(?i)^jdbc:postgresql:.*?/u14_i03_[a-z0-9_]+(?:\\?.*)?$")) {
            throw new IllegalStateException("Refusing U14 I03 integration test outside a disposable u14_i03_* database");
        }

        schema = "u14_i03_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }

        DataSource dataSource = new DriverManagerDataSource(urlWithSchema(jdbcUrl, schema), username, password);
        jdbc = new JdbcTemplate(dataSource);
        namedJdbc = new NamedParameterJdbcTemplate(dataSource);
        createMinimalContractSchema();
    }

    @AfterEach
    void dropDisposableSchema() throws Exception {
        if (schema == null) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void historyQueryAppliesGuestHotelAndEligibilityFiltersBeforeLimit() {
        jdbc.update("INSERT INTO booking_guests VALUES (1, false), (2, false), (3, true)");
        jdbc.update("INSERT INTO hotel_room_types VALUES (101, 1), (102, 2)");
        insertValidStay(10, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00");
        insertValidStay(11, 2, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-01T16:00:00+07:00", "2026-10-04T12:00:00+07:00");
        insertValidStay(12, 3, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-01T16:00:00+07:00", "2026-10-05T12:00:00+07:00");
        insertValidStay(13, 1, 2, 102, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-01T16:00:00+07:00", "2026-10-06T12:00:00+07:00");
        insertValidStay(14, 1, 1, 101, "GIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-01T16:00:00+07:00", "2026-10-07T12:00:00+07:00");
        insertValidStay(15, 1, 1, 101, "FIT", "NO_SHOW", "CHECKED_OUT",
                "2026-10-01T16:00:00+07:00", "2026-10-08T12:00:00+07:00");
        insertValidStay(16, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-01T16:00:00+07:00", "2026-10-09T12:00:00+07:00");
        jdbc.update("INSERT INTO booking_details VALUES (16002, 16, 101, 1)");
        insertValidStay(17, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-01T16:00:00+07:00", "2026-10-10T12:00:00+07:00", (short) 2);
        insertValidStay(18, 1, 1, 101, "FIT", "CONFIRMED", "EXPECTED",
                "2026-10-01T16:00:00+07:00", "2026-10-11T12:00:00+07:00");
        insertValidStay(19, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-01T00:10:00+07:00", "2026-10-01T23:55:00+07:00");
        insertValidStay(20, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2025-10-25T23:59:59+07:00", "2025-10-26T00:00:00+07:00");
        insertValidStay(21, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2025-10-25T23:59:00+07:00", "2025-10-25T23:59:59+07:00");
        insertValidStay(22, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-25T00:00:00+07:00", "2026-10-26T13:00:00+07:00");
        insertValidStay(23, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-25T00:00:00+07:00", "2026-10-26T13:00:01+07:00");
        insertValidStay(24, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-25T00:00:00+07:00", "2026-10-26T12:00:00+07:00");
        jdbc.update("UPDATE booking_rooms SET actual_check_in_at = NULL WHERE id = 20024");
        insertValidStay(25, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-25T00:00:00+07:00", "2026-10-26T12:00:00+07:00");
        jdbc.update("UPDATE booking_rooms SET actual_check_out_at = NULL WHERE id = 20025");
        insertValidStay(26, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-25T00:00:00+07:00", "2026-10-26T12:00:00+07:00");
        jdbc.update("INSERT INTO booking_rooms VALUES (30026, 10026, 'CHECKED_OUT', " +
                "'2026-10-25T00:00:00+07:00'::timestamptz, '2026-10-26T12:00:00+07:00'::timestamptz)");
        insertValidStay(27, 1, 1, 102, "FIT", "CONFIRMED", "CHECKED_OUT",
                "2026-10-25T00:00:00+07:00", "2026-10-26T12:00:00+07:00");

        List<Map<String, Object>> result = queryHistory(1L, 1,
                "2025-10-26T00:00:00+07:00", "2026-10-26T13:00:00+07:00");

        assertThat(result).extracting(row -> row.get("bookingRoomId")).containsExactly(20022L, 20010L, 20020L);
        assertThat(result).allSatisfy(row -> {
            assertThat(((Number) row.get("hotelId")).intValue()).isEqualTo(1);
            assertThat(row.get("hotelRoomTypeId")).isEqualTo(101);
        });
    }

    @Test
    void historyQueryReturnsTheLatestHundredMatchingRowsAfterScopeFiltering() {
        jdbc.update("INSERT INTO booking_guests VALUES (1, false), (2, false)");
        jdbc.update("INSERT INTO hotel_room_types VALUES (101, 1)");
        for (int i = 0; i < 100; i++) {
            insertValidStay(1000 + i, 2, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                    "2026-10-01T16:00:00+07:00", "2026-10-02T12:00:00+07:00");
        }
        for (int i = 0; i < 105; i++) {
            insertValidStay(2000 + i, 1, 1, 101, "FIT", "CONFIRMED", "CHECKED_OUT",
                    "2026-10-01T16:00:00+07:00", "2026-10-02T12:00:00+07:00");
        }

        List<Map<String, Object>> result = queryHistory(1L, 1,
                "2025-10-26T00:00:00+07:00", "2026-10-26T13:00:00+07:00");

        assertThat(result).hasSize(100);
        assertThat(result.getFirst().get("bookingRoomId")).isEqualTo(22104L);
        assertThat(result.getLast().get("bookingRoomId")).isEqualTo(22005L);
        assertThat(result).allSatisfy(row -> assertThat(((Number) row.get("hotelId")).intValue()).isEqualTo(1));
    }

    @Test
    void catalogQueryExcludesInactiveAndDeletedRowsAndKeepsHotelEligibilityDistinct() {
        jdbc.update("INSERT INTO hotels VALUES (1, 'ACTIVE', false), (2, 'ACTIVE', false), (3, 'INACTIVE', false), (4, 'ACTIVE', true)");
        jdbc.update("INSERT INTO room_types VALUES (11, 'DELUXE', 'Deluxe', 'ACTIVE', false), " +
                "(12, 'HIDDEN', 'Hidden', 'INACTIVE', false), (13, 'DELETED', 'Deleted', 'ACTIVE', true)");
        jdbc.update("INSERT INTO hotel_room_types VALUES " +
                "(101, 1, 11, 'ACTIVE', false), (102, 1, 12, 'ACTIVE', false), " +
                "(103, 1, 11, 'INACTIVE', false), (104, 1, 11, 'ACTIVE', true)");

        List<Map<String, Object>> active = queryCatalog(1);
        List<Map<String, Object>> emptyCatalog = queryCatalog(2);

        assertThat(active).hasSize(1);
        assertThat(active.getFirst()).containsEntry("hotelRoomTypeId", 101)
                .containsEntry("roomTypeCode", "DELUXE")
                .containsEntry("roomTypeName", "Deluxe");
        assertThat(emptyCatalog).hasSize(1);
        assertThat(emptyCatalog.getFirst().get("hotelRoomTypeId")).isNull();
        assertThat(queryCatalog(3)).isEmpty();
        assertThat(queryCatalog(4)).isEmpty();
        assertThat(queryCatalog(99)).isEmpty();
    }

    private List<Map<String, Object>> queryHistory(
            long guestId,
            int hotelId,
            String fromInclusive,
            String toInclusive
    ) {
        return namedJdbc.queryForList(
                querySql(PersonalizationHistoryQueryRepository.class, "findEligibleStays"),
                new MapSqlParameterSource()
                        .addValue("guestId", guestId)
                        .addValue("hotelId", hotelId)
                        .addValue("fromInclusive", OffsetDateTime.parse(fromInclusive))
                        .addValue("toInclusive", OffsetDateTime.parse(toInclusive))
        );
    }

    private List<Map<String, Object>> queryCatalog(int hotelId) {
        return namedJdbc.queryForList(
                querySql(PersonalizationCatalogQueryRepository.class, "findActiveCatalog"),
                Map.of("hotelId", hotelId)
        );
    }

    private String querySql(Class<?> repositoryType, String methodName) {
        return List.of(repositoryType.getMethods()).stream()
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .map(method -> method.getAnnotation(Query.class))
                .map(Query::value)
                .orElseThrow(() -> new IllegalStateException("Expected production query annotation is missing"));
    }

    private void insertValidStay(
            long id,
            long guestId,
            int hotelId,
            int hotelRoomTypeId,
            String bookingType,
            String bookingStatus,
            String roomStatus,
            String checkIn,
            String checkOut
    ) {
        insertValidStay(id, guestId, hotelId, hotelRoomTypeId, bookingType, bookingStatus, roomStatus, checkIn, checkOut, (short) 1);
    }

    private void insertValidStay(
            long id,
            long guestId,
            int hotelId,
            int hotelRoomTypeId,
            String bookingType,
            String bookingStatus,
            String roomStatus,
            String checkIn,
            String checkOut,
            short quantity
    ) {
        jdbc.update("INSERT INTO bookings VALUES (?, ?, ?, ?, ?)", id, hotelId, guestId, bookingType, bookingStatus);
        jdbc.update("INSERT INTO booking_details VALUES (?, ?, ?, ?)", id + 10000, id, hotelRoomTypeId, quantity);
        jdbc.update("INSERT INTO booking_rooms VALUES (?, ?, ?, ?::timestamptz, ?::timestamptz)",
                id + 20000, id + 10000, roomStatus, checkIn, checkOut);
    }

    private void createMinimalContractSchema() {
        jdbc.execute("CREATE TABLE booking_guests (id BIGINT PRIMARY KEY, is_deleted BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE bookings (id BIGINT PRIMARY KEY, hotel_id SMALLINT NOT NULL, guest_id BIGINT NOT NULL, booking_type VARCHAR(20) NOT NULL, status VARCHAR(50) NOT NULL)");
        jdbc.execute("CREATE TABLE hotel_room_types (id INTEGER PRIMARY KEY, hotel_id SMALLINT NOT NULL, room_type_id SMALLINT, status VARCHAR(50), is_deleted BOOLEAN)");
        jdbc.execute("CREATE TABLE booking_details (id BIGINT PRIMARY KEY, booking_id BIGINT NOT NULL, hotel_room_type_id INTEGER NOT NULL, quantity SMALLINT NOT NULL)");
        jdbc.execute("CREATE TABLE booking_rooms (id BIGINT PRIMARY KEY, booking_detail_id BIGINT NOT NULL, status VARCHAR(50) NOT NULL, actual_check_in_at TIMESTAMPTZ, actual_check_out_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE hotels (id SMALLINT PRIMARY KEY, status VARCHAR(50) NOT NULL, is_deleted BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE room_types (id SMALLINT PRIMARY KEY, code VARCHAR(50) NOT NULL, name VARCHAR(150) NOT NULL, status VARCHAR(50) NOT NULL, is_deleted BOOLEAN NOT NULL)");
    }

    private String urlWithSchema(String baseUrl, String targetSchema) {
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + targetSchema;
    }

    private String requiredEnvironmentValue(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set for the disposable U14 I03 integration test");
        }
        return value;
    }
}
