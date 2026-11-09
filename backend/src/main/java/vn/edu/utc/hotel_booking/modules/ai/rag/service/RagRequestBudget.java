package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.PiiRedactionUtil;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.RagProviderProperties;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/** Single-JVM demo budget; no IP/session identity or distributed fairness claims. */
@Component
public final class RagRequestBudget {
    private static final Logger LOG = LoggerFactory.getLogger(RagRequestBudget.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ThreadLocal<RequestContext> CURRENT = new ThreadLocal<>();
    private static final ScheduledThreadPoolExecutor DEADLINES = deadlines();
    private static final Pattern CREDENTIAL = Pattern.compile(
            "(?iu)(?:\\b(?:api[_ -]?key|RAG_GEMINI_API_KEY|secret[_ -]?key|password|mật\\s*khẩu)\\s*[:=]\\s*[^\\s,;]+"
                    + "|\\bBearer\\s+[A-Za-z0-9._~-]+|\\bAIza[A-Za-z0-9_-]{35}\\b)");

    private final int concurrency;
    private final int rate;
    private final long windowNanos;
    private final long deadlineNanos;
    private final LongSupplier ticker;
    private final String model;
    private final Deque<Long> admissions = new ArrayDeque<>();
    private final Set<RequestContext> active = new HashSet<>();

    public RagRequestBudget() { this(null); }

    public RagRequestBudget(@Nullable RagProviderProperties properties) {
        this(2, 20, Duration.ofMinutes(1), Duration.ofMillis(properties == null ? 12_000 : properties.getRequestTimeoutMillis()),
                System::nanoTime, properties != null && properties.isEnabled() ? properties.getChatModel() : "local");
        if (properties != null) properties.validate();
    }

    @Autowired
    public RagRequestBudget(@Nullable RagProviderProperties properties, Environment environment) {
        this(properties);
        if (environment.getProperty("hotel.ai.rag.outbound-ledger-required", Boolean.class, false)) {
            RagOutboundLedger.requireConfiguredPolicy();
        }
    }

    /** Also permits deterministic clocks and shorter watchdogs in offline tests. */
    public RagRequestBudget(int concurrency, int rate, Duration window, Duration deadline, LongSupplier ticker) {
        this(concurrency, rate, window, deadline, ticker, "local");
    }

    private RagRequestBudget(int concurrency, int rate, Duration window, Duration deadline, LongSupplier ticker, String model) {
        if (concurrency < 1 || concurrency > 2 || rate < 1 || rate > 20 || window == null || window.isNegative()
                || window.isZero() || window.compareTo(Duration.ofMinutes(1)) > 0 || deadline == null
                || deadline.isNegative() || deadline.isZero() || deadline.compareTo(Duration.ofSeconds(12)) > 0) {
            throw new IllegalArgumentException("Invalid RAG request budget");
        }
        this.concurrency = concurrency;
        this.rate = rate;
        this.windowNanos = window.toNanos();
        this.deadlineNanos = deadline.toNanos();
        this.ticker = Objects.requireNonNull(ticker);
        this.model = safeModel(model);
    }

    /** No Future.get timeout: work stays on the calling thread, including cancellation and cleanup. */
    public <T> T execute(Callable<T> work) {
        Objects.requireNonNull(work);
        if (CURRENT.get() != null) throw new UnavailableException();
        RequestContext context = admit();
        CURRENT.set(context);
        ScheduledFuture<?> alarm = null;
        String outcome = "UNAVAILABLE";
        try {
            alarm = DEADLINES.schedule(context::expire, deadlineNanos, TimeUnit.NANOSECONDS);
            context.check();
            T result = work.call();
            context.check();
            outcome = "SUCCESS";
            return result;
        }
        catch (Exception failure) {
            outcome = context.expired() ? "DEADLINE" : "UNAVAILABLE";
            throw new UnavailableException(); // no provider body, input or cause escapes
        }
        finally {
            // close() and the timer synchronize: a late alarm cannot cancel another request.
            context.close();
            if (alarm != null) alarm.cancel(false);
            CURRENT.remove();
            synchronized (this) { active.remove(context); }
            audit("RAG_REQUEST", context.id, model, context.elapsedMillis(), context.usage(), outcome);
        }
    }

    private synchronized RequestContext admit() {
        long now = ticker.getAsLong();
        while (!admissions.isEmpty() && now - admissions.getFirst() >= windowNanos) admissions.removeFirst();
        long wait = 0;
        if (admissions.size() >= rate) wait = Math.max(1, windowNanos - (now - admissions.getFirst()));
        if (active.size() >= concurrency) {
            long slotWait = active.stream().mapToLong(ctx -> Math.max(1, ctx.remainingNanos())).min().orElse(1);
            wait = Math.max(wait, slotWait);
        }
        if (wait > 0) {
            audit("RAG_REQUEST", UUID.randomUUID().toString(), model, 0, Usage.unknown(), "RATE_LIMITED");
            throw new LimitExceededException(Math.max(1, (wait + 999_999_999L) / 1_000_000_000L));
        }
        var context = new RequestContext(now, deadlineNanos, ticker);
        admissions.addLast(now);
        active.add(context);
        return context;
    }

    public synchronized int activeRequests() { return active.size(); }

    public static String redactQuestion(String question) {
        return PiiRedactionUtil.redact(CREDENTIAL.matcher(RagCorpusGovernanceValidator.canonicalSafetyText(question))
                .replaceAll("[REDACTED_CREDENTIAL]"));
    }

    /** Used only by the dedicated Gemini SDK clients; no URL or arbitrary HTTP executor is exposed. */
    public static OkHttpClient providerHttpClient(String model, int timeoutMillis) {
        String configuredModel = safeModel(model);
        if (timeoutMillis < 100 || timeoutMillis > 7000) throw new IllegalArgumentException("Invalid RAG provider timeout");
        return new OkHttpClient.Builder().retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
                .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS).readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMillis, TimeUnit.MILLISECONDS).callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .addInterceptor(chain -> providerCall(chain, configuredModel, timeoutMillis)).build();
    }

    private static Response providerCall(Interceptor.Chain chain, String model, int timeoutMillis) throws IOException {
        RequestContext context = CURRENT.get();
        String requestId = context == null ? UUID.randomUUID().toString() : context.id;
        long start = System.nanoTime();
        Call call = chain.call();
        if (context != null) {
            context.register(call);
            // SDK may set its own call timeout; this additional deadline covers the remaining whole request.
            call.timeout().deadlineNanoTime(System.nanoTime() + Math.min(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), context.remainingNanos()));
        }
        var finished = new AtomicBoolean();
        Usage[] usage = {Usage.unknown()};
        String[] outcome = {"PROVIDER_ERROR"};
        RagOutboundLedger.Reservation[] reservation = {null};
        Runnable finish = () -> {
            if (finished.compareAndSet(false, true)) {
                try {
                    if (reservation[0] != null) reservation[0].complete(usage[0].input, usage[0].output, usage[0].total);
                } finally {
                    if (context != null) context.unregister(call, usage[0]);
                    audit("RAG_PROVIDER", requestId, model, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), usage[0],
                            context != null && context.expired() ? "DEADLINE" : outcome[0]);
                }
            }
        };
        Response response = null;
        try {
            reservation[0] = RagOutboundLedger.beforeCall(model, chain.request());
            if (context != null) context.check();
            response = chain.proceed(chain.request());
            if (response.isSuccessful()) {
                // Bounded peek, no body retained or logged. Missing/oversized metadata remains UNKNOWN (-1).
                usage[0] = readUsage(response);
                outcome[0] = "SUCCESS";
            }
            ResponseBody body = response.body();
            if (body == null) { finish.run(); return response; }
            BufferedSource guarded = Okio.buffer(new ForwardingSource(body.source()) {
                @Override public long read(okio.Buffer sink, long count) throws IOException {
                    try {
                        long read = super.read(sink, count);
                        if (read == -1) finish.run();
                        return read;
                    }
                    catch (IOException failure) { outcome[0] = "PROVIDER_ERROR"; finish.run(); throw failure; }
                }
                @Override public void close() throws IOException {
                    try { super.close(); } finally { finish.run(); }
                }
            });
            return response.newBuilder().body(new ResponseBody() {
                @Override public MediaType contentType() { return body.contentType(); }
                @Override public long contentLength() { return body.contentLength(); }
                @Override public BufferedSource source() { return guarded; }
            }).build();
        }
        catch (IOException | RuntimeException failure) {
            if (response != null) response.close();
            finish.run();
            throw new IOException("RAG provider transport unavailable");
        }
    }

    private static Usage readUsage(Response response) {
        try {
            JsonNode metadata = JSON.readTree(response.peekBody(262_144).string()).path("usageMetadata");
            return new Usage(token(metadata, "promptTokenCount"), token(metadata, "candidatesTokenCount"), token(metadata, "totalTokenCount"));
        }
        catch (IOException | RuntimeException failure) { return Usage.unknown(); }
    }

    private static long token(JsonNode metadata, String field) {
        JsonNode value = metadata.path(field);
        return value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= 0 && value.longValue() <= 10_000_000
                ? value.longValue() : -1;
    }

    private record Usage(long input, long output, long total) {
        static Usage unknown() { return new Usage(-1, -1, -1); }
        Usage plus(Usage other) {
            return new Usage(sum(input, other.input), sum(output, other.output), sum(total, other.total));
        }
        private static long sum(long a, long b) { return a < 0 || b < 0 ? -1 : a + b; }
    }

    private static String safeModel(String model) {
        if (model == null || !model.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}")) {
            throw new IllegalArgumentException("Invalid RAG audit model identity");
        }
        return model;
    }

    private static void audit(String event, String id, String model, long millis, Usage usage, String outcome) {
        LOG.info("{} requestId={} model={} durationMs={} inputTokens={} outputTokens={} totalTokens={} outcome={}",
                event, id, model, millis, usage.input, usage.output, usage.total, outcome);
    }

    private static ScheduledThreadPoolExecutor deadlines() {
        var executor = new ScheduledThreadPoolExecutor(1, task -> {
            var thread = new Thread(task, "rag-request-deadline"); thread.setDaemon(true); return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    private static final class RequestContext {
        private final String id = UUID.randomUUID().toString();
        private final long start;
        private final long deadline;
        private final LongSupplier ticker;
        private final Set<Call> calls = Collections.newSetFromMap(new IdentityHashMap<>());
        private boolean closed;
        private boolean timedOut;
        private Usage usage = new Usage(0, 0, 0);
        RequestContext(long start, long deadline, LongSupplier ticker) { this.start = start; this.deadline = deadline; this.ticker = ticker; }
        long remainingNanos() { return deadline - (ticker.getAsLong() - start); }
        long elapsedMillis() { return TimeUnit.NANOSECONDS.toMillis(Math.max(0, ticker.getAsLong() - start)); }
        synchronized boolean expired() { return timedOut || remainingNanos() <= 0; }
        synchronized Usage usage() { return usage; }
        synchronized void check() {
            if (closed || expired()) { expire(); throw new UnavailableException(); }
        }
        synchronized void register(Call call) {
            if (closed || expired()) { call.cancel(); throw new UnavailableException(); }
            calls.add(call);
        }
        synchronized void unregister(Call call, Usage used) { calls.remove(call); usage = usage.plus(used); }
        synchronized void expire() {
            if (closed) return;
            timedOut = true;
            calls.forEach(Call::cancel);
        }
        synchronized void close() {
            closed = true;
            calls.forEach(Call::cancel);
            calls.clear();
        }
    }

    public static final class LimitExceededException extends RuntimeException {
        private final long retryAfterSeconds;
        public LimitExceededException(long retryAfterSeconds) {
            super("RAG request limit exceeded"); this.retryAfterSeconds = retryAfterSeconds;
        }
        public long retryAfterSeconds() { return retryAfterSeconds; }
    }
    public static final class UnavailableException extends RuntimeException {
        public UnavailableException() { super("RAG request temporarily unavailable"); }
    }
}
