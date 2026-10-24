package vn.edu.utc.hotel_booking.modules.ai.advisor.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded, per-JVM-instance rate limiter for the public Room Advisor endpoints (ADR-04).
 *
 * <p>Policy per tracked key:</p>
 * <ul>
 *   <li>anonymous owner key: sustained {@value #ANONYMOUS_REQUESTS_PER_MINUTE} requests per minute
 *       with a burst allowance of {@value #ANONYMOUS_BURST} requests;</li>
 *   <li>authenticated owner key: sustained {@value #AUTHENTICATED_REQUESTS_PER_MINUTE} requests per
 *       minute with a burst allowance of {@value #AUTHENTICATED_BURST} requests;</li>
 *   <li>client IP: {@value #CLIENT_IP_REQUESTS_PER_MINUTE} requests per minute.</li>
 * </ul>
 *
 * <p>One policy is enforced by two complementary mechanisms: a token bucket caps the sustained
 * rate and a {@value #WINDOW_SECONDS}-second sliding window caps how many requests may be admitted
 * inside any single minute (the burst allowance). A request is admitted only when both allow it,
 * so a client may burst up to the burst allowance and is then throttled to the sustained
 * per-minute rate.</p>
 *
 * <p>Bounds: at most {@value #MAX_TRACKED_KEYS} keys are tracked in memory. Buckets idle for longer
 * than {@value #WINDOW_SECONDS} seconds are evicted. Active or non-stale buckets are never evicted
 * to admit new identities (REV-010-F001). When capacity is saturated and no stale buckets can be
 * safely evicted, unseen keys are rejected fail-closed with HTTP 429 and a positive {@code Retry-After} wait.</p>
 *
 * <p>Scope: limits are enforced <strong>per JVM instance</strong>. They do not aggregate across
 * pods; distributed limiting is deferred to a Member 2 decision (ADR-04).</p>
 *
 * <p>Client identity: the only client-derived input is {@code HttpServletRequest.getRemoteAddr()}.
 * No forwarded header and no client-supplied ownership header is ever read; the caller passes the
 * remote address that the container resolved.</p>
 */
@Component
public class RoomAdvisorRateLimiter {

    /** Length of the sliding window and of the idle-bucket eviction horizon. */
    public static final Duration WINDOW = Duration.ofMinutes(1);

    /** Window length in seconds, for documentation and assertions. */
    public static final int WINDOW_SECONDS = (int) WINDOW.toSeconds();

    /** Sustained budget of an anonymous owner key, in requests per minute. */
    public static final int ANONYMOUS_REQUESTS_PER_MINUTE = 10;

    /** Burst allowance of an anonymous owner key, in requests inside one window. */
    public static final int ANONYMOUS_BURST = 15;

    /** Sustained budget of an authenticated owner key, in requests per minute. */
    public static final int AUTHENTICATED_REQUESTS_PER_MINUTE = 30;

    /** Burst allowance of an authenticated owner key, in requests inside one window. */
    public static final int AUTHENTICATED_BURST = 45;

    /** Budget of one client IP, in requests per minute. */
    public static final int CLIENT_IP_REQUESTS_PER_MINUTE = 60;

    /** Burst allowance of one client IP, in requests inside one window. */
    public static final int CLIENT_IP_BURST = 60;

    /** Hard cap on the number of tracked keys kept in memory. */
    public static final int MAX_TRACKED_KEYS = 10_000;

    private static final String OWNER_KEY_PREFIX = "owner:";
    private static final String IP_KEY_PREFIX = "ip:";

    /** Guard so a long idle period refills the bucket completely without overflowing. */
    private static final Duration MAX_REFILL_ELAPSED = Duration.ofDays(1);

    /**
     * Fixed-point scale for exact token calculations without floating-point error (REV-010-F002).
     * One token is represented as {@code SCALE} units, corresponding to the number of
     * nanoseconds in one 60-second minute (60,000,000,000 ns).
     * With sustained rate {@code R} requests per minute, exactly {@code R} units accumulate
     * per nanosecond: in 60s, {@code 60_000_000_000 * R = R * SCALE} units accumulate, which
     * equals exactly {@code R} full tokens.
     */
    private static final long SCALE = 60_000_000_000L;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Object admissionLock = new Object();
    private final Clock clock;

    private Instant lastEvictionAt;
    private Instant earliestPossibleStaleTime;

    public RoomAdvisorRateLimiter() {
        this(Clock.systemUTC());
    }

    public RoomAdvisorRateLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Enforces the owner budget and, independently, the client IP budget.
     *
     * <p>The owner budget is evaluated first so that a caller exhausting its own budget is rejected
     * before the shared IP budget is consumed. When a request is rejected the remaining wait time
     * is reported through {@link RateLimitExceededException}.</p>
     *
     * @param ownerKey      opaque owner key resolved by {@link RoomAdvisorPrincipalExtractor}
     * @param authenticated whether the owner key belongs to an authenticated principal
     * @param clientIp      client address resolved by the container ({@code getRemoteAddr()})
     * @throws RateLimitExceededException when either budget is exhausted
     */
    public void checkAllowed(String ownerKey, boolean authenticated, String clientIp) {
        Objects.requireNonNull(ownerKey, "ownerKey must not be null");
        Policy ownerPolicy = authenticated ? Policy.AUTHENTICATED : Policy.ANONYMOUS;
        String ownerBucketKey = OWNER_KEY_PREFIX + (authenticated ? "auth:" : "anon:") + ownerKey;

        Decision ownerDecision = evaluate(ownerBucketKey, ownerPolicy);
        if (!ownerDecision.allowed()) {
            throw new RateLimitExceededException(ownerDecision.retryAfterSeconds());
        }

        Decision ipDecision = evaluate(IP_KEY_PREFIX + normalizeClientIp(clientIp), Policy.CLIENT_IP);
        if (!ipDecision.allowed()) {
            throw new RateLimitExceededException(ipDecision.retryAfterSeconds());
        }
    }

    /** Number of currently tracked keys; exposed for bounded-memory assertions. */
    public int trackedKeyCount() {
        return buckets.size();
    }

    private Decision evaluate(String key, Policy policy) {
        Instant now = clock.instant();
        tryPeriodicEviction(now);
        Bucket bucket = getOrAdmitBucket(key, policy, now);
        if (bucket == null) {
            int retryAfter = computeCapacityRetryAfter(now);
            return Decision.rejected(retryAfter);
        }
        return bucket.evaluate(now);
    }

    private Bucket getOrAdmitBucket(String key, Policy policy, Instant now) {
        Bucket existing = buckets.get(key);
        if (existing != null) {
            return existing;
        }
        synchronized (admissionLock) {
            existing = buckets.get(key);
            if (existing != null) {
                return existing;
            }
            if (buckets.size() >= MAX_TRACKED_KEYS) {
                // If at capacity, check if any buckets could be stale before rejecting.
                if (earliestPossibleStaleTime == null || !earliestPossibleStaleTime.isAfter(now)) {
                    evictStaleBuckets(now);
                }
            }
            if (buckets.size() >= MAX_TRACKED_KEYS) {
                // Capacity exhausted and no safe eviction is available (fail-closed, REV-010-F001).
                return null;
            }
            Bucket created = new Bucket(policy, now);
            buckets.put(key, created);
            return created;
        }
    }

    private void tryPeriodicEviction(Instant now) {
        Instant last = lastEvictionAt;
        if (last == null || !last.plus(WINDOW).isAfter(now)) {
            synchronized (admissionLock) {
                if (lastEvictionAt == null || !lastEvictionAt.plus(WINDOW).isAfter(now)) {
                    evictStaleBuckets(now);
                }
            }
        }
    }

    private void evictStaleBuckets(Instant now) {
        Instant cutoff = now.minus(WINDOW);
        Instant minRefill = null;

        var iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Bucket b = entry.getValue();
            Instant refill = b.lastRefill();
            if (refill.isBefore(cutoff)) {
                iterator.remove();
            } else {
                if (minRefill == null || refill.isBefore(minRefill)) {
                    minRefill = refill;
                }
            }
        }

        lastEvictionAt = now;
        earliestPossibleStaleTime = (minRefill != null) ? minRefill.plus(WINDOW) : null;
    }

    private int computeCapacityRetryAfter(Instant now) {
        Instant earliest = earliestPossibleStaleTime;
        if (earliest != null && earliest.isAfter(now)) {
            long millis = Duration.between(now, earliest).toMillis();
            long seconds = (millis + 999L) / 1000L;
            return (int) Math.max(1L, Math.min((long) WINDOW_SECONDS, seconds));
        }
        return WINDOW_SECONDS;
    }

    private static String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.trim();
    }

    private static long secondsUntilNextToken(long tokenUnits, Policy policy) {
        if (tokenUnits >= SCALE) {
            return 0L;
        }
        long missingUnits = SCALE - tokenUnits;
        long unitsPerSecond = 1_000_000_000L * (long) policy.requestsPerMinute();
        return (missingUnits + unitsPerSecond - 1) / unitsPerSecond;
    }

    private static long secondsUntilWindowSlot(Deque<Instant> window, Instant now) {
        Instant oldest = window.peekFirst();
        if (oldest == null) {
            return 0L;
        }
        Instant freeAt = oldest.plus(WINDOW);
        if (!freeAt.isAfter(now)) {
            return 0L;
        }
        long millis = Duration.between(now, freeAt).toMillis();
        return Math.max(1L, (millis + 999L) / 1000L);
    }

    /** Limit policy for one tracked key. */
    private record Policy(String name, int requestsPerMinute, int burst) {

        private static final Policy ANONYMOUS = new Policy("anonymous", ANONYMOUS_REQUESTS_PER_MINUTE, ANONYMOUS_BURST);
        private static final Policy AUTHENTICATED =
                new Policy("authenticated", AUTHENTICATED_REQUESTS_PER_MINUTE, AUTHENTICATED_BURST);
        private static final Policy CLIENT_IP = new Policy("client-ip", CLIENT_IP_REQUESTS_PER_MINUTE, CLIENT_IP_BURST);
    }

    /** Admission decision for one tracked key. */
    private record Decision(boolean allowed, int retryAfterSeconds) {

        private static final Decision ALLOWED = new Decision(true, 0);

        static Decision rejected(int retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds);
        }
    }

    /** Per-key state: token bucket plus the sliding window of admitted request timestamps. */
    private static final class Bucket {

        private final Policy policy;
        private final Deque<Instant> window = new ArrayDeque<>();
        private long tokenUnits;
        private volatile Instant lastRefill;

        Bucket(Policy policy, Instant now) {
            this.policy = policy;
            this.tokenUnits = (long) policy.burst() * SCALE;
            this.lastRefill = now;
        }

        Instant lastRefill() {
            return lastRefill;
        }

        synchronized Decision evaluate(Instant now) {
            refill(now);
            boolean windowAllows = window.size() < policy.burst();
            if (tokenUnits >= SCALE && windowAllows) {
                tokenUnits -= SCALE;
                window.addLast(now);
                return Decision.ALLOWED;
            } else {
                long tokenWait = (tokenUnits >= SCALE) ? 0L : secondsUntilNextToken(tokenUnits, policy);
                long windowWait = windowAllows ? 0L : secondsUntilWindowSlot(window, now);
                long retryAfter = Math.max(1L, Math.max(tokenWait, windowWait));
                return Decision.rejected((int) Math.min(Integer.MAX_VALUE, retryAfter));
            }
        }

        private void refill(Instant now) {
            Duration elapsed = Duration.between(lastRefill, now);
            if (elapsed.isNegative()) {
                elapsed = Duration.ZERO;
            }
            long maxUnits = (long) policy.burst() * SCALE;
            if (elapsed.compareTo(MAX_REFILL_ELAPSED) > 0) {
                tokenUnits = maxUnits;
            } else if (!elapsed.isZero()) {
                long elapsedNanos = elapsed.toNanos();
                long addedUnits = elapsedNanos * (long) policy.requestsPerMinute();
                tokenUnits = Math.min(maxUnits, tokenUnits + addedUnits);
            }
            if (!elapsed.isNegative()) {
                lastRefill = now;
            }
            Instant cutoff = now.minus(WINDOW);
            while (!window.isEmpty() && !window.peekFirst().isAfter(cutoff)) {
                window.pollFirst();
            }
        }
    }
}
