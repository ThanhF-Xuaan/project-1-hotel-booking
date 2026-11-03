package vn.edu.utc.hotel_booking.modules.operation.service;

import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;

/** Explicitly selected ITs only. Refuse any target other than the owned synthetic acceptance DB. */
abstract class OperationPostgresSupport {
    static LocalContainerEntityManagerFactoryBean factory;
    static EntityManager em;
    static JpaRepositoryFactory repositories;
    static JdbcTemplate jdbc;
    static TransactionTemplate tx;
    @BeforeAll static void openDatabase() {
        if (!"true".equals(System.getenv("M1_OPERATION_IT")) || !"true".equals(System.getenv("T05_EXPORT_IT_EXCLUSIVE_VERIFIED"))) {
            throw new IllegalStateException("Explicit exclusive synthetic operation IT authorization required");
        }
        var source = new LodgingExportPostgresTest.IntegrationConfiguration().dataSource();
        jdbc = new JdbcTemplate(source);
        assertThat(jdbc.queryForObject("SELECT current_database()",String.class)).isEqualTo("hotel_acceptance_db");
        assertThat(jdbc.queryForObject("SELECT name FROM hotels WHERE id=1",String.class)).isEqualTo("Acceptance Luxury Hanoi (Hà Nội)");
        factory = new LocalContainerEntityManagerFactoryBean();factory.setDataSource(source);
        factory.setPackagesToScan("vn.edu.utc.hotel_booking");factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        var properties = new Properties();properties.setProperty("hibernate.hbm2ddl.auto","none");
        factory.setJpaProperties(properties);factory.afterPropertiesSet();
        em=SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
        repositories=new JpaRepositoryFactory(em);
        tx=new TransactionTemplate(new JpaTransactionManager(factory.getObject()));tx.setTimeout(20);
    }
    @AfterAll static void closeDatabase() { if (factory != null) factory.destroy(); }
    static <T> T run(Supplier<T> work) {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "synthetic-operation-it","",AuthorityUtils.createAuthorityList("ROLE_CHAIN_ADMIN")));
        try { return tx.execute(status -> work.get()); } finally { SecurityContextHolder.clearContext(); }
    }
    static void awaitLockWait(int count) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime()<deadline) {
            Integer waiting=jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'",Integer.class);
            if (waiting>=count) return;
            Thread.sleep(25);
        }
        throw new AssertionError("No observed PostgreSQL lock wait for concurrent writers");
    }
}
