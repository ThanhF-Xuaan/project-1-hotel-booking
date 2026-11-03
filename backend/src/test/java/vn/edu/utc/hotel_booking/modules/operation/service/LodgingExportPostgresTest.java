package vn.edu.utc.hotel_booking.modules.operation.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapper;
import vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapperImpl;
import vn.edu.utc.hotel_booking.modules.operation.mapper.LodgingQueueMapper;
import vn.edu.utc.hotel_booking.modules.operation.mapper.LodgingQueueMapperImpl;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.StayGuestServiceImpl;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.LodgingQueueServiceImpl;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestUpdateRequest;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayInputStream;
import vn.edu.utc.hotel_booking.modules.finance.service.PayrollSummaryService;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.PayrollSummaryServiceImpl;
import vn.edu.utc.hotel_booking.modules.finance.repository.PayrollSummaryRepository;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
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
    @Autowired private StayGuestService guests;
    @Autowired private LodgingQueueService queues;
    @Autowired private PayrollSummaryService payroll;
    @Autowired private StaffRepository staffRepository;
    @Autowired private LodgingQueueRepository queueRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    private Map<String, Object> originalQueue;
    private String originalName;
    private Map<String, Object> originalGuest;

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
        originalName = jdbc.queryForObject("SELECT full_name FROM stay_guests WHERE id=1", String.class);
        originalGuest = jdbc.queryForMap("SELECT nationality,check_in_time,updated_at FROM stay_guests WHERE id=1");
        jdbc.update("UPDATE lodging_queues SET status='PENDING',error_message=NULL,exported_at=NULL,batch_reference=NULL WHERE id=1");
    }

    @AfterEach
    void restoreFixtureAndAuthentication() {
        faults.releaseFirst.countDown();
        faults.releaseMutation.countDown();
        if (originalName != null) jdbc.update("UPDATE stay_guests SET full_name=? WHERE id=1", originalName);
        if (originalGuest != null) jdbc.update("UPDATE stay_guests SET nationality=?,check_in_time=?,updated_at=? WHERE id=1",
                originalGuest.get("nationality"), originalGuest.get("check_in_time"), originalGuest.get("updated_at"));
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

    @ParameterizedTest @ValueSource(booleans={false,true})
    void exportFirstSerializesGuestUpdateAndRetry(boolean retry) throws Exception {
        faults.holdFirstSelection = true;
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<ExportOutcome> export = workers.submit(this::exportInWorker);
            assertThat(faults.firstSelected.await(10, TimeUnit.SECONDS)).isTrue();
            Future<ErrorCode> mutation = workers.submit(() -> mutateInWorker(retry));
            assertThat(awaitSecondQueryDisposition()).isTrue();
            faults.releaseFirst.countDown();
            assertThat(export.get(20,TimeUnit.SECONDS).bytes()).isNotNull();
            assertThat(mutation.get(20,TimeUnit.SECONDS)).isEqualTo(retry ? null : ErrorCode.LODGING_EXPORTED_RECORD_LOCKED);
            assertThat(jdbc.queryForObject("SELECT full_name FROM stay_guests WHERE id=1", String.class)).isEqualTo(originalName);
            assertExported();
        } finally {
            faults.releaseFirst.countDown(); workers.shutdownNow();
            assertThat(workers.awaitTermination(20,TimeUnit.SECONDS)).isTrue();
        }
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void mutationFirstIsVisibleToSubsequentExport(boolean retry) throws Exception {
        faults.holdFirstMutation = true;
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<ErrorCode> mutation = workers.submit(() -> mutateInWorker(retry));
            assertThat(faults.mutationLocked.await(10,TimeUnit.SECONDS)).isTrue();
            Future<ExportOutcome> export = workers.submit(this::exportInWorker);
            assertThat(awaitSecondQueryDisposition()).isTrue();
            faults.releaseMutation.countDown();
            assertThat(mutation.get(20,TimeUnit.SECONDS)).isNull();
            byte[] bytes = export.get(20,TimeUnit.SECONDS).bytes();
            assertThat(bytes).isNotNull();
            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue())
                        .isEqualTo(retry ? originalName.toUpperCase(java.util.Locale.ROOT) : "SYNTHETIC_CHANGED");
            }
            assertExported();
        } finally {
            faults.releaseMutation.countDown(); workers.shutdownNow();
            assertThat(workers.awaitTermination(20,TimeUnit.SECONDS)).isTrue();
        }
    }

    @ParameterizedTest @ValueSource(strings={"cutoff","category"})
    void changedReportScopeWhileExportWaitsDoesNotExportStaleSelection(String field) throws Exception {
        faults.holdFirstMutation = true;
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> mutation = workers.submit(() -> {
                authenticateAdmin();
                try {
                    if (field.equals("cutoff")) {
                        // Check-in time is not part of the update DTO. Exercise the same queue-first
                        // transaction protocol directly; the nationality case uses the real service.
                        new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                                .executeWithoutResult(status -> {
                                    queueRepository.findByStayGuestIdAndIsDeletedFalse(1L).orElseThrow();
                                    jdbc.update("UPDATE stay_guests SET check_in_time=? WHERE id=1",
                                            request().getDate().plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.ofHours(7)));
                                });
                    } else guests.update(1L, StayGuestUpdateRequest.builder().nationality("Japan").build());
                } finally { SecurityContextHolder.clearContext(); }
            });
            assertThat(faults.mutationLocked.await(10,TimeUnit.SECONDS)).isTrue();
            Future<ExportOutcome> export = workers.submit(this::exportInWorker);
            assertThat(awaitSecondQueryDisposition()).isTrue();
            faults.releaseMutation.countDown();
            mutation.get(20,TimeUnit.SECONDS);
            var result = export.get(20,TimeUnit.SECONDS);
            assertThat(result.bytes()).isNull();
            assertThat(result.error()).isEqualTo(ErrorCode.LODGING_EXPORT_EMPTY);
            assertPendingWithoutMetadata();
        } finally {
            faults.releaseMutation.countDown(); workers.shutdownNow();
            assertThat(workers.awaitTermination(20,TimeUnit.SECONDS)).isTrue();
        }
    }

    private ErrorCode mutateInWorker(boolean retry) {
        authenticateAdmin();
        try {
            if (retry) queues.retryValidate(1L);
            else guests.update(1L, StayGuestUpdateRequest.builder().fullName("SYNTHETIC_CHANGED").build());
            return null;
        } catch (AppException error) { return error.getErrorCode(); }
        finally { SecurityContextHolder.clearContext(); }
    }

    @Test void payrollRoundTripPreservesExactEquationAndRejectsLossyValues() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payroll_summary WHERE staff_id=1 AND period_year=2099 AND period_month IN (11,12)",Integer.class)).isZero();
        when(staffRepository.findAllById(any())).thenReturn(List.of(Staff.builder().id(1).build()));
        try {
            String header = "staff,month,year,base,bonus,deductions,net\n";
            var valid = new MockMultipartFile("file","synthetic.csv","text/csv",
                    (header+"1,11,2099,9999999999999.99,0,0,9999999999999.99\n").getBytes(StandardCharsets.UTF_8));
            assertThat(payroll.importCsv(valid)).isEmpty();
            var row = jdbc.queryForMap("SELECT base_salary,bonus,deductions,net_salary FROM payroll_summary WHERE staff_id=1 AND period_year=2099 AND period_month=11");
            BigDecimal base = (BigDecimal)row.get("base_salary"), bonus = (BigDecimal)row.get("bonus");
            assertThat((BigDecimal)row.get("net_salary")).isEqualByComparingTo(base.add(bonus).subtract((BigDecimal)row.get("deductions")));
            assertThat(base).isEqualByComparingTo("9999999999999.99");
            var invalid = new MockMultipartFile("file","synthetic.csv","text/csv",
                    (header+"1,12,2099,100.005,0.005,0,100.010\n").getBytes(StandardCharsets.UTF_8));
            assertThat(payroll.importCsv(invalid)).hasSize(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM payroll_summary WHERE staff_id=1 AND period_year=2099 AND period_month=12",Integer.class)).isZero();
        } finally {
            jdbc.update("DELETE FROM payroll_summary WHERE staff_id=1 AND period_year=2099 AND period_month IN (11,12)");
            reset(staffRepository);
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
        volatile boolean holdFirstMutation;
        CountDownLatch mutationLocked, releaseMutation;
        CountDownLatch firstSelected, releaseFirst, secondStarted, secondReturned;

        void reset() {
            failGeneration.set(false);
            failAfterFlush.set(false);
            observedFlush.set(false);
            selections.set(0);
            holdFirstSelection = false;
            holdFirstMutation = false;
            mutationLocked = new CountDownLatch(1); releaseMutation = new CountDownLatch(1);
            firstSelected = new CountDownLatch(1);
            releaseFirst = new CountDownLatch(1);
            secondStarted = new CountDownLatch(1);
            secondReturned = new CountDownLatch(1);
        }
        void awaitMutation() throws InterruptedException {
            if (holdFirstMutation) {
                mutationLocked.countDown();
                if (!releaseMutation.await(15,TimeUnit.SECONDS)) throw new IllegalStateException("Synthetic mutation barrier timeout");
            }
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
        @Bean StayGuestRepository guestRepository(EntityManagerFactory factory) {
            return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory)).getRepository(StayGuestRepository.class);
        }
        @Bean PayrollSummaryService payrollService(EntityManagerFactory factory, StaffRepository staff) {
            var repository = new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory)).getRepository(PayrollSummaryRepository.class);
            return new PayrollSummaryServiceImpl(repository,staff);
        }
        @Bean StayGuestMapper guestMapper() { return new StayGuestMapperImpl(); }
        @Bean LodgingQueueMapper queueMapper() { return new LodgingQueueMapperImpl(); }
        @Bean StayGuestService guestService(StayGuestRepository guests, LodgingQueueRepository queues,
                                          StaffRepository staff, StayGuestMapper mapper) {
            return new StayGuestServiceImpl(guests, queues, mock(HotelRepository.class), mock(BookingLookupService.class),
                    mock(RoomInstanceRepository.class), staff, mapper);
        }
        @Bean LodgingQueueService queueService(LodgingQueueRepository queues, StaffRepository staff, LodgingQueueMapper mapper) {
            return new LodgingQueueServiceImpl(queues, staff, mapper);
        }

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
                var row = actual.findByStayGuestIdAndIsDeletedFalse(invocation.getArgument(0));
                faults.awaitMutation(); return row;
            }).when(repository).findByStayGuestIdAndIsDeletedFalse(anyLong());
            doAnswer(invocation -> {
                var row = actual.findByIdForUpdate(invocation.getArgument(0));
                faults.awaitMutation(); return row;
            }).when(repository).findByIdForUpdate(anyLong());
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
