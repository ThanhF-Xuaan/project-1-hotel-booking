package vn.edu.utc.hotel_booking.modules.identity.personalization.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * PostgreSQL-backed U14 consent checks. Run only against a disposable PostgreSQL database by setting
 * U14_I02_IT=true and DB_NAME with the u14_i02_ prefix.
 */
@EnabledIfEnvironmentVariable(named = "U14_I02_IT", matches = "true")
@SpringBootTest
@TestPropertySource(properties = {
        "spring.liquibase.enabled=true",
        "spring.liquibase.change-log=classpath:db/db-changelog-root.xml",
        "spring.liquibase.contexts=master-data,dev-seed",
        "hotel.ai.personalization.local-demo-enabled=true",
        "hotel.scheduling.enabled=false"
})
class PersonalizationConsentPostgresIT {

    private static final String ISSUER = "http://localhost:18081/realms/hotel-acceptance";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String subject;
    private Short hotelId;
    private Long bindingId;
    private Long guestId;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @BeforeEach
    void createSyntheticBinding() {
        String databaseName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        if (databaseName == null || !databaseName.startsWith("u14_i02_")) {
            throw new IllegalStateException("Refusing to run U14 I02 integration test outside a disposable database");
        }
        hotelId = jdbcTemplate.queryForObject(
                "SELECT id FROM hotels WHERE is_deleted = false AND UPPER(status) = 'ACTIVE' ORDER BY id LIMIT 1",
                Short.class);
        if (hotelId == null) {
            throw new IllegalStateException("Disposable database has no active seeded hotel");
        }

        subject = UUID.randomUUID().toString();
        UUID guestPublicId = UUID.randomUUID();
        String phone = "u14" + UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        guestId = jdbcTemplate.queryForObject("""
                INSERT INTO booking_guests (public_id, full_name, phone, email, nationality, status, is_deleted)
                VALUES (?, 'Synthetic U14 IT Guest', ?, 'u14-it@acceptance.local', 'Synthetic', 'ACTIVE', false)
                RETURNING id
                """, Long.class, guestPublicId, phone);
        bindingId = jdbcTemplate.queryForObject("""
                INSERT INTO personalization_account_bindings
                    (issuer, subject, guest_id, binding_version, source, status)
                VALUES (?, ?, ?, 1, 'SYNTHETIC_FIXTURE', 'ACTIVE')
                RETURNING id
                """, Long.class, ISSUER, subject, guestId);
    }

    @AfterEach
    void deleteSyntheticBinding() {
        if (bindingId != null) {
            jdbcTemplate.update("DELETE FROM personalization_consent_events WHERE binding_id = ?", bindingId);
            jdbcTemplate.update("DELETE FROM personalization_consents WHERE binding_id = ?", bindingId);
            jdbcTemplate.update("DELETE FROM personalization_account_bindings WHERE id = ?", bindingId);
        }
        if (guestId != null) {
            jdbcTemplate.update("DELETE FROM booking_guests WHERE id = ?", guestId);
        }
    }

    @Test
    void grantThenRevokePersistsAtomicStateAndAppendOnlyEvents() throws Exception {
        MvcResult grant = update(true, 0, true);
        assertThat(grant.getResponse().getStatus()).isEqualTo(200);
        assertThat(grant.getResponse().getContentAsString()).contains("\"status\":\"GRANTED\"");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM personalization_consents WHERE binding_id = ? AND granted = true AND revision = 1",
                Integer.class, bindingId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM personalization_consent_events WHERE binding_id = ? AND granted = true AND revision = 1",
                Integer.class, bindingId)).isEqualTo(1);

        MvcResult revoke = update(false, 1, false);
        assertThat(revoke.getResponse().getStatus()).isEqualTo(200);
        assertThat(revoke.getResponse().getContentAsString()).contains("\"status\":\"NOT_GRANTED\"");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT revision FROM personalization_consents WHERE binding_id = ?", Long.class, bindingId))
                .isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT policy_version FROM personalization_consents WHERE binding_id = ?", String.class, bindingId))
                .isEqualTo("u14-local-room-affinity-v1");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM personalization_consent_events WHERE binding_id = ?", Integer.class, bindingId))
                .isEqualTo(2);
    }

    @Test
    void concurrentWritersWithSameRevisionProduceOneCommitAndOneConflict() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> responses = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                responses.add(executor.submit(() -> {
                    start.await();
                    return update(true, 0, true).getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = List.of(responses.get(0).get(), responses.get(1).get());
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM personalization_consents WHERE binding_id = ?", Integer.class, bindingId))
                    .isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM personalization_consent_events WHERE binding_id = ?", Integer.class, bindingId))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void eventInsertFailureRollsBackConsentState() throws Exception {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION u14_i02_test_reject_consent_event() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'synthetic U14 event failure'; END;
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER trg_u14_i02_test_reject_consent_event
                BEFORE INSERT ON personalization_consent_events
                FOR EACH ROW EXECUTE FUNCTION u14_i02_test_reject_consent_event()
                """);
        try {
            MvcResult response = update(true, 0, true);
            assertThat(response.getResponse().getStatus()).isEqualTo(503);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM personalization_consents WHERE binding_id = ?", Integer.class, bindingId))
                    .isZero();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM personalization_consent_events WHERE binding_id = ?", Integer.class, bindingId))
                    .isZero();
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_u14_i02_test_reject_consent_event ON personalization_consent_events");
            jdbcTemplate.execute("DROP FUNCTION IF EXISTS u14_i02_test_reject_consent_event()");
        }
    }

    private MvcResult update(boolean granted, long revision, boolean includePolicy) throws Exception {
        String policyField = includePolicy ? ",\"policyVersion\":\"u14-local-room-affinity-v1\"" : "";
        String body = "{\"hotelId\":" + hotelId
                + ",\"granted\":" + granted
                + ",\"expectedRevision\":" + revision
                + ",\"expectedBindingVersion\":1"
                + policyField + "}";
        return mockMvc.perform(put("/api/v1/me/personalization-consent")
                        .with(jwt().jwt(jwt -> jwt.issuer(ISSUER).subject(subject))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }
}
