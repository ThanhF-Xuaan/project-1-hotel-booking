package vn.edu.utc.hotel_booking.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class UtilityMigrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg16-trixie")
                    .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    private int hotelId;
    private int staffId;

    @BeforeEach
    void setupFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int regionId = jdbc.queryForObject(
                "INSERT INTO regions(code, name) VALUES (?, ?) RETURNING id",
                Integer.class, "R" + suffix, "Region " + suffix);
        hotelId = jdbc.queryForObject("""
                INSERT INTO hotels(region_id, name, address, service_fee_percent)
                VALUES (?, ?, ?, 5.00) RETURNING id
                """, Integer.class, regionId, "Hotel " + suffix, "Address " + suffix);

        staffId = jdbc.queryForObject("""
                INSERT INTO staffs(keycloak_id, username, first_name, last_name, full_name, scope_type)
                VALUES (?, ?, 'Test', 'Staff', 'Test Staff', 'CHAIN') RETURNING id
                """, Integer.class, UUID.randomUUID(), "staff_" + suffix);
    }

    @Test
    void utilityMetersSchemaAndConstraints() {
        String meterCode = "MTR-001";

        // 1. Insert valid meter
        Integer meterId = jdbc.queryForObject("""
                INSERT INTO utility_meters(hotel_id, meter_code, meter_type, location_label)
                VALUES (?, ?, 'ELECTRICITY', 'Main Panel Floor 1') RETURNING id
                """, Integer.class, hotelId, meterCode);
        assertNotNull(meterId);

        // Verify default timestamps are populated
        Timestamp createdAt = jdbc.queryForObject(
                "SELECT created_at FROM utility_meters WHERE id = ?",
                Timestamp.class, meterId);
        assertNotNull(createdAt);

        // 2. Duplicate active meter code in the same hotel must fail
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO utility_meters(hotel_id, meter_code, meter_type, location_label)
                VALUES (?, ?, 'WATER', 'Basement')
                """, hotelId, meterCode));

        // 3. Soft-delete meter allows creating new meter with the same meter_code
        jdbc.update("UPDATE utility_meters SET is_deleted = TRUE WHERE id = ?", meterId);

        Integer newMeterId = jdbc.queryForObject("""
                INSERT INTO utility_meters(hotel_id, meter_code, meter_type, location_label)
                VALUES (?, ?, 'WATER', 'Basement Replacement') RETURNING id
                """, Integer.class, hotelId, meterCode);
        assertNotNull(newMeterId);

        // 4. Invalid meter type check constraint
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO utility_meters(hotel_id, meter_code, meter_type, location_label)
                VALUES (?, 'MTR-GAS', 'GAS', 'Kitchen')
                """, hotelId));
    }

    @Test
    void utilityReadingsSchemaAndConstraints() {
        Integer meterId = jdbc.queryForObject("""
                INSERT INTO utility_meters(hotel_id, meter_code, meter_type, location_label)
                VALUES (?, 'MTR-READING-TEST', 'ELECTRICITY', 'Generator') RETURNING id
                """, Integer.class, hotelId);

        LocalDate readingDate = LocalDate.of(2026, 10, 1);

        // 1. Insert valid reading
        Long readingId = jdbc.queryForObject("""
                INSERT INTO utility_readings(meter_id, reading_date, reading_value, is_meter_reset, recorded_by)
                VALUES (?, ?, ?, FALSE, ?) RETURNING id
                """, Long.class, meterId, Date.valueOf(readingDate), new BigDecimal("1250.500"), staffId);
        assertNotNull(readingId);

        // 2. One reading per meter per day constraint
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO utility_readings(meter_id, reading_date, reading_value, is_meter_reset, recorded_by)
                VALUES (?, ?, ?, FALSE, ?)
                """, meterId, Date.valueOf(readingDate), new BigDecimal("1260.000"), staffId));

        // 3. Negative reading value violates check constraint
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO utility_readings(meter_id, reading_date, reading_value, is_meter_reset, recorded_by)
                VALUES (?, ?, ?, FALSE, ?)
                """, meterId, Date.valueOf(readingDate.plusDays(1)), new BigDecimal("-1.000"), staffId));

        // 4. Non-existent staff id violates foreign key
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO utility_readings(meter_id, reading_date, reading_value, is_meter_reset, recorded_by)
                VALUES (?, ?, ?, FALSE, 999999)
                """, meterId, Date.valueOf(readingDate.plusDays(1)), new BigDecimal("1300.000")));

        // 5. Update reading with updated_by referencing staffs(id)
        jdbc.update("UPDATE utility_readings SET reading_value = ?, updated_by = ? WHERE id = ?",
                new BigDecimal("1255.000"), staffId, readingId);
        Integer updatedBy = jdbc.queryForObject(
                "SELECT updated_by FROM utility_readings WHERE id = ?",
                Integer.class, readingId);
        assertEquals(staffId, updatedBy);
    }

    @Test
    void utilityPermissionsAndRoleAssignments() {
        // 1. Verify permissions inserted in V004
        List<String> actions = jdbc.queryForList("""
                SELECT action FROM permissions WHERE resource = 'UTILITY' ORDER BY action
                """, String.class);
        assertEquals(List.of("CREATE", "UPDATE", "VIEW"), actions);

        // 2. Seed roles to test role_permissions mapping (matching 10-seed-utility-permissions.sql logic)
        jdbc.update("""
                INSERT INTO roles(code, name) VALUES
                    ('CHAIN_ADMIN', 'Chain Admin'),
                    ('PROPERTY_MANAGER', 'Property Manager'),
                    ('REGION_MANAGER', 'Region Manager')
                ON CONFLICT (code) DO NOTHING
                """);

        jdbc.update("""
                INSERT INTO role_permissions(role_id, permission_id)
                SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
                WHERE r.code IN ('CHAIN_ADMIN', 'PROPERTY_MANAGER')
                  AND p.resource = 'UTILITY'
                  AND p.action IN ('VIEW', 'CREATE', 'UPDATE')
                ON CONFLICT DO NOTHING
                """);

        jdbc.update("""
                INSERT INTO role_permissions(role_id, permission_id)
                SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
                WHERE r.code = 'REGION_MANAGER'
                  AND p.resource = 'UTILITY' AND p.action = 'VIEW'
                ON CONFLICT DO NOTHING
                """);

        // Verify CHAIN_ADMIN has 3 permissions
        Integer chainAdminPerms = jdbc.queryForObject("""
                SELECT count(*) FROM role_permissions rp
                JOIN roles r ON rp.role_id = r.id
                JOIN permissions p ON rp.permission_id = p.id
                WHERE r.code = 'CHAIN_ADMIN' AND p.resource = 'UTILITY'
                """, Integer.class);
        assertEquals(3, chainAdminPerms);

        // Verify PROPERTY_MANAGER has 3 permissions
        Integer propManagerPerms = jdbc.queryForObject("""
                SELECT count(*) FROM role_permissions rp
                JOIN roles r ON rp.role_id = r.id
                JOIN permissions p ON rp.permission_id = p.id
                WHERE r.code = 'PROPERTY_MANAGER' AND p.resource = 'UTILITY'
                """, Integer.class);
        assertEquals(3, propManagerPerms);

        // Verify REGION_MANAGER has only 1 permission (VIEW)
        List<String> regionPerms = jdbc.queryForList("""
                SELECT p.action FROM role_permissions rp
                JOIN roles r ON rp.role_id = r.id
                JOIN permissions p ON rp.permission_id = p.id
                WHERE r.code = 'REGION_MANAGER' AND p.resource = 'UTILITY'
                """, String.class);
        assertEquals(List.of("VIEW"), regionPerms);
    }
}
