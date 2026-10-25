package vn.edu.utc.hotel_booking.modules.operation.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingExportRequest;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.export.LodgingWorkbookWriter;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.LodgingExportServiceImpl;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Opt-in only: real JPA transactions against the exclusive synthetic acceptance database. */
@EnabledIfEnvironmentVariable(named = "T05_EXPORT_IT", matches = "true")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LodgingExportPostgresTest.IntegrationConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LodgingExportPostgresTest {
    @Autowired private LodgingExportService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FaultControl faults;
    private Map<String, Object> originalQueue;

    @BeforeEach
    void requireSyntheticFixtureAndPreparePendingRow() {
        faults.reset();
        authenticateAdmin();
        assertThat(jdbc.queryForObject("SELECT name FROM hotels WHERE id=1", String.class))
                .isEqualTo("Acceptance Luxury Hanoi (Hà Nội)");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lodging_queues WHERE hotel_id=1 AND is_deleted=false", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT stay_guest_id FROM lodging_queues WHERE id=1 AND hotel_id=1", Long.class))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT nationality FROM stay_guests WHERE id=1", String.class)).isEqualTo("Vietnam");
        originalQueue = jdbc.queryForMap("SELECT status,error_message,exported_at,batch_reference,updated_at FROM lodging_queues WHERE id=1");
        jdbc.update("UPDATE lodging_queues SET status='PENDING',error_message=NULL,exported_at=NULL,batch_reference=NULL WHERE id=1");
    }

    @AfterEach
    void restoreFixtureAndAuthentication() {
        faults.releaseFirst.countDown();
        if (originalQueue != null) {
            jdbc.update("UPDATE lodging_queues SET status=?,error_message=?,exported_at=?,batch_reference=?,updated_at=? WHERE id=1",
                    originalQueue.get("status"), originalQueue.get("error_message"), originalQueue.get("exported_at"),
                    originalQueue.get("batch_reference"), originalQueue.get("updated_at"));
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void overlappingExportsWaitForRowLockAndOnlyOneCreatesABatch() throws Exception {
        faults.holdFirstSelection = true;
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<ExportOutcome> first = workers.submit(this::exportInWorker);
            assertThat(faults.firstSelected.await(10, TimeUnit.SECONDS)).isTrue();
            Future<ExportOutcome> second = workers.submit(this::exportInWorker);
            assertThat(faults.secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
            boolean secondWaitedOnPostgres = awaitSecondQueryDisposition();
            faults.releaseFirst.countDown();
            List<ExportOutcome> outcomes = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertThat(outcomes.stream().filter(result -> result.bytes() != null).count()).isEqualTo(1);
            assertThat(outcomes.stream().filter(result -> result.error() == ErrorCode.LODGING_EXPORT_EMPTY).count()).isEqualTo(1);
            assertThat(secondWaitedOnPostgres).as("Second transaction actually waited for the PostgreSQL lock").isTrue();
            assertExported();
        } finally {
            faults.releaseFirst.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void generationIOExceptionLeavesPendingMetadataAndAllowsRetry() {
        faults.failGeneration.set(true);
        assertThatThrownBy(() -> service.exportBcaLodgingReport(request()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION));
        assertPendingWithoutMetadata();
        faults.failGeneration.set(false);
        assertThat(service.exportBcaLodgingReport(request())).hasSizeGreaterThan(4);
        assertExported();
    }

    @Test
    void failureAfterRealDatabaseFlushRollsBackAndAllowsRetry() {
        faults.failAfterFlush.set(true);
        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> service.exportBcaLodgingReport(request()));
        assertThat(error.getMostSpecificCause()).isInstanceOf(SQLException.class);
        assertThat(((SQLException) error.getMostSpecificCause()).getSQLState()).isEqualTo("23514");
        assertThat(faults.observedFlush).isTrue();
        assertPendingWithoutMetadata();
        faults.failAfterFlush.set(false);
        assertThat(service.exportBcaLodgingReport(request())).hasSizeGreaterThan(4);
        assertExported();
    }

    private ExportOutcome exportInWorker() {
        authenticateAdmin();
        try {
            return new ExportOutcome(service.exportBcaLodgingReport(request()), null);
        } catch (AppException error) {
            return new ExportOutcome(null, error.getErrorCode());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean awaitSecondQueryDisposition() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() "
                    + "AND application_name='t05-export-it' AND wait_event_type='Lock' AND query LIKE '%lodging_queues%'", Integer.class);
            if (waiting != null && waiting > 0) return true;
            if (faults.secondReturned.getCount() == 0) return false;
            Thread.sleep(20);
        }
        throw new AssertionError("Second export neither returned nor waited on a PostgreSQL lock");
    }

    private void assertPendingWithoutMetadata() {
        Map<String, Object> row = jdbc.queryForMap("SELECT status,exported_at,batch_reference FROM lodging_queues WHERE id=1");
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("exported_at")).isNull();
        assertThat(row.get("batch_reference")).isNull();
    }

    private void assertExported() {
        Map<String, Object> row = jdbc.queryForMap("SELECT status,exported_at,batch_reference FROM lodging_queues WHERE id=1");
        assertThat(row.get("status")).isEqualTo("EXPORTED");
        assertThat(row.get("exported_at")).isNotNull();
        assertThat(row.get("batch_reference").toString()).startsWith("BCA-");
    }

    private LodgingExportRequest request() {
        return LodgingExportRequest.builder().hotelId((short) 1).date(LocalDate.now().plusDays(1)).build();
    }

    private static void authenticateAdmin() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "synthetic-export-admin", null, AuthorityUtils.createAuthorityList("ROLE_CHAIN_ADMIN")));
    }

    private record ExportOutcome(byte[] bytes, ErrorCode error) {}

    static class FaultControl {
        final AtomicBoolean failGeneration = new AtomicBoolean();
        final AtomicBoolean failAfterFlush = new AtomicBoolean();
        final AtomicBoolean observedFlush = new AtomicBoolean();
        final AtomicInteger selections = new AtomicInteger();
        volatile boolean holdFirstSelection;
        CountDownLatch firstSelected, releaseFirst, secondStarted, secondReturned;

        void reset() {
            failGeneration.set(false);
            failAfterFlush.set(false);
            observedFlush.set(false);
            selections.set(0);
            holdFirstSelection = false;
            firstSelected = new CountDownLatch(1);
            releaseFirst = new CountDownLatch(1);
            secondStarted = new CountDownLatch(1);
            secondReturned = new CountDownLatch(1);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    @EnableJpaRepositories(basePackageClasses = LodgingQueueRepository.class)
    static class IntegrationConfiguration {
        @Bean
        DataSource dataSource() {
            if (!"true".equals(System.getenv("T05_EXPORT_IT_EXCLUSIVE_VERIFIED"))) {
                throw new IllegalStateException("An exclusive synthetic acceptance database must be verified first");
            }
            String host = System.getenv("T05_EXPORT_IT_DB_HOST");
            String port = System.getenv("T05_EXPORT_IT_DB_PORT");
            String name = System.getenv("T05_EXPORT_IT_DB_NAME");
            String user = System.getenv("T05_EXPORT_IT_DB_USER");
            String password = System.getenv("T05_EXPORT_IT_DB_PASSWORD");
            if (!("127.0.0.1".equals(host) || "localhost".equals(host)) || !"15432".equals(port)
                    || !"hotel_acceptance_db".equals(name) || !"postgres".equals(user)
                    || password == null || password.isBlank()) {
                throw new IllegalStateException("Only localhost:15432/hotel_acceptance_db with explicit test credentials is allowed");
            }
            DriverManagerDataSource source = new DriverManagerDataSource("jdbc:postgresql://" + host + ":" + port + "/" + name, user, password);
            Properties properties = new Properties();
            properties.setProperty("ApplicationName", "t05-export-it");
            properties.setProperty("connectTimeout", "5");
            properties.setProperty("socketTimeout", "30");
            source.setConnectionProperties(properties);
            return source;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source);
            factory.setPackagesToScan("vn.edu.utc.hotel_booking");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            Properties properties = new Properties();
            properties.setProperty("hibernate.hbm2ddl.auto", "none");
            properties.setProperty("hibernate.show_sql", "false");
            factory.setJpaProperties(properties);
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean JdbcTemplate jdbcTemplate(DataSource source) { return new JdbcTemplate(source); }
        @Bean FaultControl faultControl() { return new FaultControl(); }
        @Bean StaffRepository staffRepository() { return mock(StaffRepository.class); }

        @Bean
        LodgingWorkbookWriter workbookWriter(FaultControl faults) throws IOException {
            LodgingWorkbookWriter writer = spy(new LodgingWorkbookWriter());
            doAnswer(invocation -> {
                if (faults.failGeneration.get()) throw new IOException("Synthetic workbook generation failure");
                return invocation.callRealMethod();
            }).when(writer).write(anyList(), anyBoolean(), any(OutputStream.class));
            return writer;
        }

        @Bean
        @Primary
        LodgingQueueRepository faultRepository(@Qualifier("lodgingQueueRepository") LodgingQueueRepository actual,
                                               EntityManagerFactory factory, FaultControl faults, JdbcTemplate jdbc) {
            EntityManager entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
            LodgingQueueRepository repository = mock(LodgingQueueRepository.class, delegatesTo(actual));
            doAnswer(invocation -> {
                int call = faults.selections.incrementAndGet();
                if (faults.holdFirstSelection && call == 2) faults.secondStarted.countDown();
                List<LodgingQueue> rows = actual.findPendingForExport(invocation.getArgument(0),
                        invocation.getArgument(1), invocation.getArgument(2));
                if (faults.holdFirstSelection && call == 1) {
                    faults.firstSelected.countDown();
                    if (!faults.releaseFirst.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Synthetic barrier timeout");
                }
                if (faults.holdFirstSelection && call == 2) faults.secondReturned.countDown();
                return rows;
            }).when(repository).findPendingForExport(anyShort(), any(), anyBoolean());
            doAnswer(invocation -> {
                Iterable<LodgingQueue> rows = invocation.getArgument(0);
                List<LodgingQueue> saved = actual.saveAll(rows);
                if (faults.failAfterFlush.get()) {
                    entityManager.flush();
                    faults.observedFlush.set(true);
                    jdbc.update("UPDATE lodging_queues SET status='TEST_INVALID_STATUS' WHERE id=1");
                    throw new AssertionError("Acceptance schema must reject an invalid queue status");
                }
                return saved;
            }).when(repository).saveAll(any());
            return repository;
        }

        @Bean
        LodgingExportService exportService(@Qualifier("faultRepository") LodgingQueueRepository repository,
                                           StaffRepository staffRepository, LodgingWorkbookWriter writer) {
            return new LodgingExportServiceImpl(repository, staffRepository, writer);
        }
    }
}
