package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real-Redis integration evidence suite for atomic optimistic concurrency (AC-4.6, AC-4.8, §13.3).
 *
 * <p>Execution is strictly gated by {@code T12_REDIS_IT=true}. When enabled, tests verify real
 * multithreaded optimistic concurrency against an exclusive disposable Redis at {@code localhost:6379}.
 * Tests adhere strictly to M2 cleanup constraints: cursor-based SCAN deletion of {@code advisor:*} only,
 * sentinel survival check, and zero usage of FLUSHDB/FLUSHALL/KEYS *.</p>
 */
@EnabledIfEnvironmentVariable(named = "T12_REDIS_IT", matches = "true")
class RedisRoomAdvisorSessionStoreRealRedisTest {

    private static final String REDIS_HOST = resolveHost();
    private static final int REDIS_PORT = resolvePort();
    private static final String SENTINEL_KEY = "test:sentinel:do-not-delete";
    private static final String SENTINEL_VALUE = "SENTINEL_SURVIVAL_CHECK";

    private static String resolveHost() {
        String envHost = System.getenv("T12_REDIS_HOST");
        if (envHost != null && !envHost.isBlank()) {
            return envHost.trim();
        }
        String propHost = System.getProperty("t12.redis.host");
        if (propHost != null && !propHost.isBlank()) {
            return propHost.trim();
        }
        String genericEnvHost = System.getenv("REDIS_HOST");
        if (genericEnvHost != null && !genericEnvHost.isBlank()) {
            return genericEnvHost.trim();
        }
        return "localhost";
    }

    private static int resolvePort() {
        String envPort = System.getenv("T12_REDIS_PORT");
        if (envPort != null && !envPort.isBlank()) {
            return Integer.parseInt(envPort.trim());
        }
        String propPort = System.getProperty("t12.redis.port");
        if (propPort != null && !propPort.isBlank()) {
            return Integer.parseInt(propPort.trim());
        }
        String genericEnvPort = System.getenv("REDIS_PORT");
        if (genericEnvPort != null && !genericEnvPort.isBlank()) {
            return Integer.parseInt(genericEnvPort.trim());
        }
        return 6379;
    }

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisRoomAdvisorSessionStore store;

    @BeforeAll
    static void initRedis() {
        // Exclusivity preflight check (§13.3)
        String exclusiveVerified = System.getenv("T12_REDIS_EXCLUSIVE_VERIFIED");
        if (!"true".equalsIgnoreCase(exclusiveVerified)
                && !"true".equalsIgnoreCase(System.getProperty("t12.redis.exclusive.verified"))) {
            throw new IllegalStateException(
                    "M2-SEC-03 Safety Gate (§13.3): Redis at " + REDIS_HOST + ":" + REDIS_PORT
                            + " has not been certified as an exclusive, isolated disposable test instance. "
                            + "Set T12_REDIS_EXCLUSIVE_VERIFIED=true only after confirming port " + REDIS_PORT + " is not shared/foreign."
            );
        }

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT);
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);

        // Verify connectivity
        String pingResult = redisTemplate.getConnectionFactory().getConnection().ping();
        if (!"PONG".equalsIgnoreCase(pingResult)) {
            throw new IllegalStateException("Redis ping failed at " + REDIS_HOST + ":" + REDIS_PORT);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        store = new RedisRoomAdvisorSessionStore(redisTemplate, objectMapper);

        // Seed synthetic non-advisor sentinel to prove cleanup isolation (AC-4.8)
        redisTemplate.opsForValue().set(SENTINEL_KEY, SENTINEL_VALUE, Duration.ofHours(1));
    }

    @AfterAll
    static void tearDownAll() {
        if (redisTemplate != null) {
            // Remove only the suite-owned sentinel
            redisTemplate.delete(SENTINEL_KEY);
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @AfterEach
    void cleanAdvisorKeys() {
        if (redisTemplate == null) {
            return;
        }

        // AC-4.8: Cursor-based SCAN MATCH advisor:* in bounded batches, delete only advisor keys
        ScanOptions scanOptions = ScanOptions.scanOptions().match("advisor:*").count(100).build();
        List<String> advisorKeys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                advisorKeys.add(cursor.next());
            }
        }

        if (!advisorKeys.isEmpty()) {
            redisTemplate.delete(advisorKeys);
        }

        // Rescan to assert zero residual advisor keys
        List<String> residualKeys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                residualKeys.add(cursor.next());
            }
        }
        assertThat(residualKeys).as("No residual advisor keys must remain after test").isEmpty();

        // AC-4.8: Verify non-advisor sentinel survived unchanged
        String sentinelVal = redisTemplate.opsForValue().get(SENTINEL_KEY);
        assertThat(sentinelVal)
                .as("Sentinel key must survive advisor cleanup unaltered")
                .isEqualTo(SENTINEL_VALUE);
    }

    @Test
    @DisplayName("AC-4.6: Concurrent writers targeting the same session/version: exactly one winner, losers receive 409")
    void concurrentWritersShouldHaveExactlyOneWinnerAndZeroLostUpdates() throws Exception {
        UUID sid = UUID.randomUUID();
        String ownerKey = "owner:real-redis-user";

        RoomAdvisorSession created = store.getOrCreateSession(sid, ownerKey);
        assertThat(created.getSessionVersion()).isEqualTo(1L);

        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedExceptions = new CopyOnWriteArrayList<>();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int threadIdx = i;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    store.updateSession(
                            sid,
                            ownerKey,
                            1L, // expected version 1L
                            RoomSearchCriteriaDto.builder().locationKeyword("City-" + threadIdx).build(),
                            null,
                            AdvisorResultDto.builder().reply("Reply from " + threadIdx).build()
                    );
                    successCount.incrementAndGet();
                } catch (SessionVersionConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedExceptions.add(t);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(unexpectedExceptions).isEmpty();
        assertThat(successCount.get()).as("Exactly one writer must succeed").isEqualTo(1);
        assertThat(conflictCount.get()).as("All losing writers must receive conflict").isEqualTo(threadCount - 1);

        // Verify no lost updates: session is now at version 2L
        Optional<RoomAdvisorSession> finalSession = store.getSession(sid, ownerKey);
        assertThat(finalSession).isPresent();
        assertThat(finalSession.get().getSessionVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("AC-4.3: Session key TTL is bounded to <= 1800s")
    void sessionKeyTtlMustBeBounded() {
        UUID sid = UUID.randomUUID();
        String ownerKey = "owner:ttl-check";

        store.getOrCreateSession(sid, ownerKey);

        String key = "advisor:session:" + sid;
        Long expireSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(expireSeconds).isNotNull().isPositive().isLessThanOrEqualTo(1800L);
    }

    @Test
    @DisplayName("Owner isolation: cross-owner access rejected with SessionSecurityException")
    void crossOwnerAccessMustBeRejected() {
        UUID sid = UUID.randomUUID();
        store.getOrCreateSession(sid, "owner:legit");

        assertThatThrownBy(() -> store.getSession(sid, "owner:intruder"))
                .isInstanceOf(SessionSecurityException.class)
                .hasMessageContaining("owner mismatch");

        assertThatThrownBy(() -> store.updateSession(sid, "owner:intruder", 1L, null, null, null))
                .isInstanceOf(SessionSecurityException.class)
                .hasMessageContaining("owner mismatch");
    }

    @Test
    @DisplayName("Missing session on update throws SessionNotFoundException")
    void missingSessionUpdateThrowsNotFound() {
        UUID nonExistentSid = UUID.randomUUID();

        assertThatThrownBy(() -> store.updateSession(nonExistentSid, "owner:any", 1L, null, null, null))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Session not found or expired");
    }
}
