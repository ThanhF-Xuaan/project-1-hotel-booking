package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the bounded in-memory rate limiter (TASK-003, ADR-04).
 *
 * <p>A mutable clock makes every window, refill and eviction assertion deterministic. No network,
 * database or Spring context is required.</p>
 */
class RoomAdvisorRateLimiterTest {

    private static final Instant START = Instant.parse("2026-10-20T12:00:00Z");
    private static final String LIMITER_SOURCE =
            "src/main/java/vn/edu/utc/hotel_booking/modules/ai/advisor/security/RoomAdvisorRateLimiter.java";

    @Test
    @DisplayName("AC-3.1: an anonymous owner may burst up to the burst allowance and is then rejected")
    void anonymousOwnerIsRejectedAfterTheBurstAllowance() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        for (int request = 0; request < RoomAdvisorRateLimiter.ANONYMOUS_BURST; request++) {
            limiter.checkAllowed("anon-owner", false, "203.0.113.10");
        }

        assertThatThrownBy(() -> limiter.checkAllowed("anon-owner", false, "203.0.113.10"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds())
                        .isPositive());
    }

    @Test
    @DisplayName("AC-3.1: the sustained anonymous rate is ten requests per minute")
    void sustainedAnonymousRateIsTenPerMinute() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        for (int request = 0; request < RoomAdvisorRateLimiter.ANONYMOUS_BURST; request++) {
            limiter.checkAllowed("anon-owner", false, "203.0.113.11");
        }

        clock.advance(Duration.ofMinutes(1));

        for (int request = 0; request < RoomAdvisorRateLimiter.ANONYMOUS_REQUESTS_PER_MINUTE; request++) {
            limiter.checkAllowed("anon-owner", false, "203.0.113.11");
        }

        assertThatThrownBy(() -> limiter.checkAllowed("anon-owner", false, "203.0.113.11"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds())
                        .isBetween(1, RoomAdvisorRateLimiter.WINDOW_SECONDS));
    }

    @Test
    @DisplayName("AC-3.2: an authenticated owner has its own larger budget")
    void authenticatedOwnerHasItsOwnBudget() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        for (int request = 0; request < RoomAdvisorRateLimiter.AUTHENTICATED_BURST; request++) {
            limiter.checkAllowed("auth:keycloak:subject-1", true, "203.0.113.12");
        }

        assertThatThrownBy(() -> limiter.checkAllowed("auth:keycloak:subject-1", true, "203.0.113.12"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("AC-3.2: an authenticated owner is not charged to the anonymous budget")
    void authenticatedAndAnonymousOwnersAreBudgetedSeparately() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        for (int request = 0; request < RoomAdvisorRateLimiter.ANONYMOUS_BURST; request++) {
            limiter.checkAllowed("anon-owner", false, "203.0.113.13");
        }

        limiter.checkAllowed("auth:keycloak:subject-2", true, "203.0.113.13");
    }

    @Test
    @DisplayName("AC-3.3: the client IP budget applies independently of the owner budget")
    void clientIpBudgetIsIndependentOfTheOwnerBudget() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        for (int request = 0; request < RoomAdvisorRateLimiter.CLIENT_IP_REQUESTS_PER_MINUTE; request++) {
            limiter.checkAllowed("anon-owner-" + request, false, "203.0.113.14");
        }

        assertThatThrownBy(() -> limiter.checkAllowed("anon-owner-extra", false, "203.0.113.14"))
                .isInstanceOf(RateLimitExceededException.class);

        limiter.checkAllowed("anon-owner-extra", false, "203.0.113.15");
    }

    @Test
    @DisplayName("AC-3.3: the limiter never reads a forwarded or client-supplied identity header")
    void limiterNeverReadsForwardedOrClientHeaders() throws IOException {
        String source = Files.readString(Path.of(LIMITER_SOURCE));

        assertThat(source).doesNotContain("X-Forwarded-For");
        assertThat(source).doesNotContain("X-Forwarded");
        assertThat(source).doesNotContain("getHeader");
        assertThat(source).doesNotContain("X-Session-Owner");
        assertThat(source).doesNotContain("X-Real-IP");
    }

    @Test
    @DisplayName("AC-3.4: buckets without activity for one window are evicted")
    void idleBucketsAreEvicted() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        limiter.checkAllowed("anon-owner", false, "203.0.113.16");
        assertThat(limiter.trackedKeyCount()).isEqualTo(2);

        clock.advance(Duration.ofSeconds(RoomAdvisorRateLimiter.WINDOW_SECONDS + 1));
        limiter.checkAllowed("anon-owner-2", false, "203.0.113.16");

        assertThat(limiter.trackedKeyCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("AC-3.4: the number of tracked keys stays capped at ten thousand")
    void trackedKeysAreCappedAtTenThousand() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        for (int request = 0; request < RoomAdvisorRateLimiter.MAX_TRACKED_KEYS + 50; request++) {
            try {
                limiter.checkAllowed("anon-owner-" + request, false, "203.0.113.17");
            } catch (RateLimitExceededException expected) {
                // the client IP budget is exhausted long before the key cap; the owner keys keep
                // being tracked, which is exactly the key-cycling case the cap has to bound
            }
        }

        assertThat(limiter.trackedKeyCount()).isLessThanOrEqualTo(RoomAdvisorRateLimiter.MAX_TRACKED_KEYS);
        assertThat(limiter.trackedKeyCount()).isGreaterThan(RoomAdvisorRateLimiter.MAX_TRACKED_KEYS - 100);
    }

    @Test
    @DisplayName("AC-3.4: concurrent requests cannot exceed the burst allowance of one owner")
    void concurrentRequestsCannotExceedTheBudget() throws InterruptedException {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);
        int threads = 40;
        int attemptsPerThread = 25;

        AtomicInteger allowed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int worker = 0; worker < threads; worker++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int attempt = 0; attempt < attemptsPerThread; attempt++) {
                    try {
                        limiter.checkAllowed("anon-owner", false, "203.0.113.18");
                        allowed.incrementAndGet();
                    } catch (RateLimitExceededException rejected) {
                        // expected once the burst allowance is consumed
                    }
                }
            });
            workers.add(thread);
            thread.start();
        }
        start.countDown();
        for (Thread worker : workers) {
            worker.join();
        }

        assertThat(allowed.get()).isEqualTo(RoomAdvisorRateLimiter.ANONYMOUS_BURST);
    }

    @Test
    @DisplayName("AC-3.1: the rejection reports a positive whole-second wait")
    void rejectionReportsPositiveRetryAfter() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        for (int request = 0; request < RoomAdvisorRateLimiter.ANONYMOUS_BURST; request++) {
            limiter.checkAllowed("anon-owner", false, "203.0.113.19");
        }

        assertThatThrownBy(() -> limiter.checkAllowed("anon-owner", false, "203.0.113.19"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> {
                    int retryAfter = ((RateLimitExceededException) thrown).retryAfterSeconds();
                    assertThat(retryAfter).isPositive();
                    assertThat(retryAfter).isLessThanOrEqualTo(RoomAdvisorRateLimiter.WINDOW_SECONDS);
                });
    }

    @Test
    @DisplayName("AC-3.3: a missing client address is tracked as one shared bucket")
    void missingClientAddressIsTrackedAsOneBucket() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        for (int request = 0; request < RoomAdvisorRateLimiter.CLIENT_IP_REQUESTS_PER_MINUTE; request++) {
            limiter.checkAllowed("anon-owner-" + request, false, null);
        }

        assertThatThrownBy(() -> limiter.checkAllowed("anon-owner-extra", false, "  "))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("REV-010-F001: actively throttled bucket is not evicted under saturation and cannot burst")
    void activelyThrottledBucketIsNotEvictedUnderSaturationAndCannotBurst() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        // Exhaust burst for victim key
        for (int request = 0; request < RoomAdvisorRateLimiter.ANONYMOUS_BURST; request++) {
            limiter.checkAllowed("victim-owner", false, "203.0.113.20");
        }
        assertThatThrownBy(() -> limiter.checkAllowed("victim-owner", false, "203.0.113.20"))
                .isInstanceOf(RateLimitExceededException.class);

        // Saturate the limiter with distinct keys at the same timestamp
        for (int i = 0; i < RoomAdvisorRateLimiter.MAX_TRACKED_KEYS; i++) {
            try {
                limiter.checkAllowed("flood-owner-" + i, false, "198.51.100." + (i % 250));
            } catch (RateLimitExceededException ignored) {
                // capacity or IP limiter might be reached
            }
        }

        // Verify victim owner is STILL throttled and was NOT evicted to grant a fresh burst
        assertThatThrownBy(() -> limiter.checkAllowed("victim-owner", false, "203.0.113.20"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isPositive());
    }

    @Test
    @DisplayName("REV-010-F001: unseen identity is rejected when capacity is occupied, with valid positive Retry-After")
    void unseenIdentityIsRejectedWhenCapacityOccupiedWithValidRetryAfter() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        // Fill up to MAX_TRACKED_KEYS with owner keys (each evaluated and tracked)
        for (int i = 0; i < RoomAdvisorRateLimiter.MAX_TRACKED_KEYS; i++) {
            try {
                limiter.checkAllowed("filler-owner-" + i, false, "203.0.113.30");
            } catch (RateLimitExceededException ignored) {
                // IP limit trips after 60, but filler-owner keys are already tracked in buckets
            }
        }

        assertThat(limiter.trackedKeyCount()).isEqualTo(RoomAdvisorRateLimiter.MAX_TRACKED_KEYS);

        // An unseen key must fail closed with RateLimitExceededException
        assertThatThrownBy(() -> limiter.checkAllowed("unseen-owner", false, "203.0.113.31"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> {
                    int retryAfter = ((RateLimitExceededException) thrown).retryAfterSeconds();
                    assertThat(retryAfter).isPositive();
                    assertThat(retryAfter).isLessThanOrEqualTo(RoomAdvisorRateLimiter.WINDOW_SECONDS);
                });

        // Tracked key count must still not exceed MAX_TRACKED_KEYS
        assertThat(limiter.trackedKeyCount()).isEqualTo(RoomAdvisorRateLimiter.MAX_TRACKED_KEYS);
    }

    @Test
    @DisplayName("REV-010-F001: existing identities continue to use original budget when map is saturated")
    void existingIdentitiesContinueUsingOriginalBudgetUnderSaturation() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        // Legitimate user consumes 5 of 15 burst requests
        for (int i = 0; i < 5; i++) {
            limiter.checkAllowed("legitimate-owner", false, "203.0.113.40");
        }

        // Fill capacity to 10,000 keys
        for (int i = 0; i < RoomAdvisorRateLimiter.MAX_TRACKED_KEYS; i++) {
            try {
                limiter.checkAllowed("other-owner-" + i, false, "203.0.113.41");
            } catch (RateLimitExceededException ignored) {
            }
        }

        // Legitimate user must still be able to consume remaining 10 burst requests
        for (int i = 5; i < RoomAdvisorRateLimiter.ANONYMOUS_BURST; i++) {
            limiter.checkAllowed("legitimate-owner", false, "203.0.113.40");
        }

        // 16th request must be rejected
        assertThatThrownBy(() -> limiter.checkAllowed("legitimate-owner", false, "203.0.113.40"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("REV-010-F001: stale keys are reclaimed after expiration, allowing new admissions")
    void staleKeysAreReclaimedAfterExpiration() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        // Fill capacity to 10,000 keys at T0
        for (int i = 0; i < RoomAdvisorRateLimiter.MAX_TRACKED_KEYS; i++) {
            try {
                limiter.checkAllowed("stale-owner-" + i, false, "203.0.113.50");
            } catch (RateLimitExceededException ignored) {
            }
        }
        assertThat(limiter.trackedKeyCount()).isEqualTo(RoomAdvisorRateLimiter.MAX_TRACKED_KEYS);

        // Advance clock past WINDOW
        clock.advance(Duration.ofSeconds(RoomAdvisorRateLimiter.WINDOW_SECONDS + 1));

        // New request should succeed because all stale buckets can be reclaimed
        limiter.checkAllowed("fresh-owner", false, "203.0.113.51");

        // The stale keys were reclaimed
        assertThat(limiter.trackedKeyCount()).isLessThan(RoomAdvisorRateLimiter.MAX_TRACKED_KEYS);
        assertThat(limiter.trackedKeyCount()).isEqualTo(2); // fresh-owner + IP
    }

    @Test
    @DisplayName("REV-010-F001: concurrent attempts cannot exceed the capacity bound of 10,000 keys")
    void concurrentAttemptsCannotExceedCapacityBound() throws InterruptedException {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        int threadCount = 40;
        int keysPerThread = 300; // 40 * 300 = 12,000 keys total (> 10,000)
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int k = 0; k < keysPerThread; k++) {
                        try {
                            limiter.checkAllowed("concurrent-owner-" + threadId + "-" + k, false, "203.0.113.60");
                        } catch (RateLimitExceededException ignored) {
                            // Expected once capacity or IP budget is reached
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(limiter.trackedKeyCount()).isLessThanOrEqualTo(RoomAdvisorRateLimiter.MAX_TRACKED_KEYS);
    }

    @Test
    @DisplayName("REV-010-F001: saturated requests do not perform repeated full-map scans")
    void saturatedRequestsDoNotPerformRepeatedFullMapScan() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        for (int i = 0; i < RoomAdvisorRateLimiter.MAX_TRACKED_KEYS; i++) {
            try {
                limiter.checkAllowed("init-owner-" + i, false, "203.0.113.70");
            } catch (RateLimitExceededException ignored) {
            }
        }
        assertThat(limiter.trackedKeyCount()).isEqualTo(RoomAdvisorRateLimiter.MAX_TRACKED_KEYS);

        // Now fire 5,000 saturated requests; since all keys are active, none are stale and
        // earliestPossibleStaleTime is in the future. Each rejection must be O(1) without scanning.
        long startNanos = System.nanoTime();
        for (int i = 0; i < 5_000; i++) {
            try {
                limiter.checkAllowed("unseen-flood-" + i, false, "203.0.113.71");
            } catch (RateLimitExceededException expected) {
                // expected
            }
        }
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        // 5,000 O(1) operations in memory take a few milliseconds (well under 500 ms)
        assertThat(durationMillis).isLessThan(500);
    }

    @Test
    @DisplayName("REV-010-F002: request arriving at exact Retry-After deadline is admitted without spurious second 429")
    void exactRetryAfterDeadlineAllowsAnonymousRequestWithoutSpuriousSecond429() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        // Consume 15 burst requests at T0
        for (int i = 0; i < RoomAdvisorRateLimiter.ANONYMOUS_BURST; i++) {
            limiter.checkAllowed("anon-timing-1", false, "203.0.113.80");
        }

        // Advance 60s
        clock.advance(Duration.ofMinutes(1));

        // Consume 10 sustained requests
        for (int i = 0; i < RoomAdvisorRateLimiter.ANONYMOUS_REQUESTS_PER_MINUTE; i++) {
            limiter.checkAllowed("anon-timing-1", false, "203.0.113.80");
        }

        // Request 11 is throttled
        assertThatThrownBy(() -> limiter.checkAllowed("anon-timing-1", false, "203.0.113.80"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(6));

        // Probe after 5 seconds: rejected with Retry-After: 1
        clock.advance(Duration.ofSeconds(5));
        assertThatThrownBy(() -> limiter.checkAllowed("anon-timing-1", false, "203.0.113.80"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(1));

        // Advance EXACTLY 1 second to the advertised Retry-After deadline
        clock.advance(Duration.ofSeconds(1));

        // MUST be admitted without second 429
        limiter.checkAllowed("anon-timing-1", false, "203.0.113.80");

        // Immediately subsequent request is rejected
        assertThatThrownBy(() -> limiter.checkAllowed("anon-timing-1", false, "203.0.113.80"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(6));
    }

    @Test
    @DisplayName("REV-010-F002: one millisecond before deadline is rejected; one millisecond after is admitted")
    void oneMillisecondBeforeAndAfterDeadlineBoundary() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        // Consume 15 burst requests at T0
        for (int i = 0; i < RoomAdvisorRateLimiter.ANONYMOUS_BURST; i++) {
            limiter.checkAllowed("anon-timing-2", false, "203.0.113.81");
        }
        clock.advance(Duration.ofMinutes(1));
        for (int i = 0; i < RoomAdvisorRateLimiter.ANONYMOUS_REQUESTS_PER_MINUTE; i++) {
            limiter.checkAllowed("anon-timing-2", false, "203.0.113.81");
        }

        // Throttled: wait 6 seconds
        assertThatThrownBy(() -> limiter.checkAllowed("anon-timing-2", false, "203.0.113.81"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(6));

        // Advance 5s and probe -> wait 1s
        clock.advance(Duration.ofSeconds(5));
        assertThatThrownBy(() -> limiter.checkAllowed("anon-timing-2", false, "203.0.113.81"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(1));

        // Advance 999 ms (1 ms before deadline) -> MUST BE REJECTED
        clock.advance(Duration.ofMillis(999));
        assertThatThrownBy(() -> limiter.checkAllowed("anon-timing-2", false, "203.0.113.81"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(1));

        // Advance 2 ms (1 ms after deadline) -> MUST BE ADMITTED
        clock.advance(Duration.ofMillis(2));
        limiter.checkAllowed("anon-timing-2", false, "203.0.113.81");
    }

    @Test
    @DisplayName("REV-010-F002: multiple token refill cycles operate deterministically")
    void multipleTokenRefillCyclesAreDeterministic() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        for (int i = 0; i < RoomAdvisorRateLimiter.ANONYMOUS_BURST; i++) {
            limiter.checkAllowed("anon-cycles", false, "203.0.113.82");
        }
        clock.advance(Duration.ofMinutes(1));
        for (int i = 0; i < RoomAdvisorRateLimiter.ANONYMOUS_REQUESTS_PER_MINUTE; i++) {
            limiter.checkAllowed("anon-cycles", false, "203.0.113.82");
        }

        // 3 consecutive refill cycles of 6 seconds each
        for (int cycle = 1; cycle <= 3; cycle++) {
            assertThatThrownBy(() -> limiter.checkAllowed("anon-cycles", false, "203.0.113.82"))
                    .isInstanceOf(RateLimitExceededException.class)
                    .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(6));

            clock.advance(Duration.ofSeconds(6));
            limiter.checkAllowed("anon-cycles", false, "203.0.113.82");
        }
    }

    @Test
    @DisplayName("REV-010-F002: authenticated owner rate limiter boundary behavior")
    void exactRetryAfterDeadlineBoundaryForAuthenticatedOwner() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        // Authenticated burst = 45
        for (int i = 0; i < RoomAdvisorRateLimiter.AUTHENTICATED_BURST; i++) {
            limiter.checkAllowed("auth:sub-boundary", true, "203.0.113.83");
        }
        clock.advance(Duration.ofMinutes(1));

        // Authenticated sustained = 30 (1 token every 2 seconds)
        for (int i = 0; i < RoomAdvisorRateLimiter.AUTHENTICATED_REQUESTS_PER_MINUTE; i++) {
            limiter.checkAllowed("auth:sub-boundary", true, "203.0.113.83");
        }

        // Request 31 is throttled with 2s wait
        assertThatThrownBy(() -> limiter.checkAllowed("auth:sub-boundary", true, "203.0.113.83"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(2));

        // Advance 1s and probe -> 1s wait
        clock.advance(Duration.ofSeconds(1));
        assertThatThrownBy(() -> limiter.checkAllowed("auth:sub-boundary", true, "203.0.113.83"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(1));

        // 1 ms before deadline (999 ms) -> rejected
        clock.advance(Duration.ofMillis(999));
        assertThatThrownBy(() -> limiter.checkAllowed("auth:sub-boundary", true, "203.0.113.83"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(1));

        // 2 ms later (1 ms after deadline) -> admitted
        clock.advance(Duration.ofMillis(2));
        limiter.checkAllowed("auth:sub-boundary", true, "203.0.113.83");
    }

    @Test
    @DisplayName("REV-010-F002: client IP rate limiter boundary behavior")
    void exactRetryAfterDeadlineBoundaryForClientIp() {
        MutableClock clock = new MutableClock(START);
        RoomAdvisorRateLimiter limiter = new RoomAdvisorRateLimiter(clock);

        // Client IP budget = 60/min (burst 60)
        for (int i = 0; i < RoomAdvisorRateLimiter.CLIENT_IP_BURST; i++) {
            limiter.checkAllowed("ip-owner-" + i, false, "203.0.113.84");
        }

        // Request 61 throttled on IP budget (window full: 60s wait)
        assertThatThrownBy(() -> limiter.checkAllowed("ip-owner-61", false, "203.0.113.84"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(60));

        // Advance 59s: oldest slot in window will free in 1 second
        clock.advance(Duration.ofSeconds(59));
        assertThatThrownBy(() -> limiter.checkAllowed("ip-owner-61", false, "203.0.113.84"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(1));

        // 1 ms before 1s deadline (999 ms) -> rejected
        clock.advance(Duration.ofMillis(999));
        assertThatThrownBy(() -> limiter.checkAllowed("ip-owner-61", false, "203.0.113.84"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(thrown -> assertThat(((RateLimitExceededException) thrown).retryAfterSeconds()).isEqualTo(1));

        // 2 ms later (1 ms after 1s deadline) -> admitted
        clock.advance(Duration.ofMillis(2));
        limiter.checkAllowed("ip-owner-61", false, "203.0.113.84");
    }

    /** Minimal mutable clock so window, refill and eviction behaviour stays deterministic. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration amount) {
            instant = instant.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
