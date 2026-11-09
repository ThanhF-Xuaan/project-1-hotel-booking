package vn.edu.utc.hotel_booking.modules.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagOutboundLedger;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagRequestBudget;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/** Synthetic rates and bounds only; these fixtures do not assert current Google prices/model limits. */
class RagOutboundLedgerTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-05T01:00:00Z"), ZoneOffset.UTC);
    @TempDir Path directory;
    private Path policyPath() { return directory.resolve("policy.json"); }
    private Path journal() { return directory.resolve("a".repeat(32) + ".ledger.jsonl"); }

    private ObjectNode policy() throws Exception {
        Files.writeString(directory.resolve("pricing-snapshot.html"), "SYNTHETIC PRICING FIXTURE ONLY");
        return (ObjectNode) JSON.readTree(JSON.writeValueAsString(Map.ofEntries(
                Map.entry("schemaVersion", "rag-gemini-ledger-v1"), Map.entry("campaignId", "a".repeat(32)), Map.entry("currency", "USD"),
                Map.entry("capturedAtUtc", "2026-11-05T00:00:00Z"), Map.entry("expiresAtUtc", "2026-11-06T00:00:00Z"),
                Map.entry("pricingSource", "https://ai.google.dev/gemini-api/docs/pricing"), Map.entry("pricingSnapshotSha256", sha(directory.resolve("pricing-snapshot.html"))),
                Map.entry("allBillableTokensCovered", true), Map.entry("maxEmbeddingInputs", 200), Map.entry("maxGenerationCalls", 120), Map.entry("limitUsd", "5"),
                Map.entry("embedding", Map.of("model", "synthetic-embedding", "inputTokenBound", 8192, "inputUsdPerMillion", "0.15", "outputUsdPerMillion", "0", "boundsAndWorstCaseRatesApproved", true)),
                Map.entry("generation", Map.of("model", "synthetic-chat", "inputTokenBound", 65536, "outputTokenBound", 768, "inputUsdPerMillion", "0.15", "outputUsdPerMillion", "0.60", "boundsAndWorstCaseRatesApproved", true)))));
    }
    private RagOutboundLedger ledger(ObjectNode policy, String boot) throws Exception {
        Files.writeString(policyPath(), JSON.writeValueAsString(policy));
        return new RagOutboundLedger(policyPath(), sha(policyPath()), CLOCK, boot);
    }
    private String sha(Path path) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))); }
    private Request embed(int count) throws Exception {
        return request("synthetic-embedding:batchEmbedContents", JSON.writeValueAsString(Map.of("requests",
                Collections.nCopies(count, Map.of("content", Map.of("parts", List.of(Map.of("text", "SYNTHETIC_PRIVATE_TEXT_NOT_IN_LEDGER"))))))));
    }
    private Request generate() { return request("synthetic-chat:generateContent", "{\"generationConfig\":{\"maxOutputTokens\":768},\"contents\":[]}"); }
    private Request request(String operation, String data) {
        return new Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/" + operation)
                .post(RequestBody.create(data, MediaType.get("application/json"))).build();
    }

    @Test void embeddingInputCapSurvivesRestartAndCountsWholeBatchesBeforeSending() throws Exception {
        var policy = policy(); var ledger = ledger(policy, "boot-A");
        for (int count : new int[]{64,64,64,8}) ledger.reserve("synthetic-embedding", embed(count)).complete(count, -1, count);
        var restarted = ledger(policy, "boot-B");
        assertThatThrownBy(() -> restarted.reserve("synthetic-embedding", embed(1))).isInstanceOf(IllegalStateException.class).hasNoCause();
        assertThat(Files.readString(journal())).doesNotContain("SYNTHETIC_PRIVATE_TEXT_NOT_IN_LEDGER");
    }

    @Test void generationCapSurvivesRestartAndDoesNotDependOnCloudAlerts() throws Exception {
        var policy = policy(); var ledger = ledger(policy, "boot-A");
        for (int i = 0; i < 120; i++) ledger.reserve("synthetic-chat", generate()).complete(7, 13, 20);
        var restarted = ledger(policy, "boot-B");
        assertThatThrownBy(() -> restarted.reserve("synthetic-chat", generate())).isInstanceOf(IllegalStateException.class);
    }

    @Test void r1AndRagGenerationShareOnePersistentCallCap() throws Exception {
        var policy = policy(); policy.put("maxGenerationCalls", 2);
        ((ObjectNode) policy.path("generation")).put("model", "gemini-2.5-flash");
        var ledger = ledger(policy, "boot-A");
        var r1 = request("gemini-2.5-flash:generateContent", "{\"generationConfig\":{\"maxOutputTokens\":512}}");
        var rag = request("gemini-2.5-flash:generateContent", "{\"generationConfig\":{\"maxOutputTokens\":768}}");
        ledger.reserve("gemini-2.5-flash", r1).complete(10, 5, 18);
        ledger.reserve("gemini-2.5-flash", rag).complete(10, 5, 18);
        assertThatThrownBy(() -> ledger(policy, "boot-B").reserve("gemini-2.5-flash", r1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void moneyIsReservedBeforeCallAndOnlyKnownUsageReleasesTheDifference() throws Exception {
        var policy = policy(); policy.put("limitUsd", "1.5");
        ((ObjectNode) policy.path("embedding")).put("inputTokenBound", 1000).put("inputUsdPerMillion", "1000");
        var ledger = ledger(policy, "boot-A");
        var first = ledger.reserve("synthetic-embedding", embed(1)); // reserve $1 before any network work
        assertThat(Files.size(journal())).isPositive();
        assertThatThrownBy(() -> ledger.reserve("synthetic-embedding", embed(1))).isInstanceOf(IllegalStateException.class);
        first.complete(10, 0, 10); // known $0.01
        ledger.reserve("synthetic-embedding", embed(1)).complete(10, 0, 10);
        assertThat(Files.readString(journal())).contains("SETTLE").doesNotContain("SYNTHETIC_PRIVATE_TEXT_NOT_IN_LEDGER");
    }

    @Test void crashWithAnUnresolvedReservationBlocksTheCampaignAfterRestart() throws Exception {
        var policy = policy(); ledger(policy, "boot-A").reserve("synthetic-chat", generate());
        var restarted = ledger(policy, "boot-B");
        assertThatThrownBy(() -> restarted.reserve("synthetic-chat", generate())).isInstanceOf(IllegalStateException.class);
        assertThat(Files.readString(journal())).contains("UNRESOLVED_RESTART", "HALT");
    }

    @ParameterizedTest @ValueSource(strings={"unknown", "over-bound", "inconsistent-total", "unreported-thinking"})
    void unknownOrUnexpectedUsageLocksTheCampaignWithoutRefund(String scenario) throws Exception {
        var policy = policy(); var ledger = ledger(policy, "boot-A"); var reservation = ledger.reserve("synthetic-chat", generate());
        long input = 7, output = 13, total = 20;
        switch(scenario) {
            case "unknown" -> input = -1;
            case "over-bound" -> input = 65537;
            case "inconsistent-total" -> total = 1;
            case "unreported-thinking" -> total = 7 + 769;
        }
        long in = input, out = output, all = total;
        assertThatThrownBy(() -> reservation.complete(in, out, all)).isInstanceOf(IllegalStateException.class).hasNoCause();
        assertThatThrownBy(() -> ledger.reserve("synthetic-chat", generate())).isInstanceOf(IllegalStateException.class);
        assertThat(Files.readString(journal())).contains("HALT");
    }

    @Test void thinkingTokensAreChargedAtTheWorstApprovedOutputRate() throws Exception {
        var policy = policy(); var ledger = ledger(policy, "boot-A");
        ledger.reserve("synthetic-chat", generate()).complete(7, 13, 120);
        String journal = Files.readString(journal());
        assertThat(journal).contains("outputTokens\\\":113", "totalTokens\\\":120");
    }

    @Test void concurrentReservationsCannotOverspendOrExceedTheInputCap() throws Exception {
        var policy = policy(); policy.put("maxEmbeddingInputs", 1); var ledger = ledger(policy, "boot-A");
        var started = new CountDownLatch(1); var reserved = new AtomicInteger();
        try (var workers = Executors.newFixedThreadPool(2)) {
            Callable<Void> attempt = () -> { started.await(); try { ledger.reserve("synthetic-embedding", embed(1)); reserved.incrementAndGet(); } catch (IllegalStateException ignored) { } return null; };
            var a = workers.submit(attempt); var b = workers.submit(attempt); started.countDown();
            a.get(2, TimeUnit.SECONDS); b.get(2, TimeUnit.SECONDS);
        }
        assertThat(reserved.get()).isEqualTo(1);
    }

    @ParameterizedTest @ValueSource(strings={"partial", "tampered", "policy-changed", "snapshot-changed", "expired"})
    void corruptOrStaleInputsFailClosed(String fault) throws Exception {
        var policy = policy(); var ledger = ledger(policy, "boot-A");
        ledger.reserve("synthetic-chat", generate()).complete(7, 13, 20);
        switch (fault) {
            case "partial" -> Files.writeString(journal(), "{", StandardOpenOption.APPEND);
            case "tampered" -> Files.writeString(journal(), Files.readString(journal()).replace("RESERVE", "RESERVX"));
            case "policy-changed" -> { policy.put("limitUsd", "4"); ledger = ledger(policy, "boot-A"); }
            case "snapshot-changed" -> Files.writeString(directory.resolve("pricing-snapshot.html"), "DIFFERENT");
            case "expired" -> { }
        }
        if (fault.equals("expired")) {
            assertThatThrownBy(() -> new RagOutboundLedger(policyPath(), sha(policyPath()), Clock.offset(CLOCK, Duration.ofDays(2)), "boot-A"))
                    .isInstanceOf(IllegalStateException.class); return;
        }
        var checked = ledger;
        assertThatThrownBy(() -> checked.reserve("synthetic-chat", generate())).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest @ValueSource(strings={"countTokens", "streamGenerateContent", "wrongModel"})
    void unknownOperationsAndModelOverridesAreRejectedBeforeReservation(String operation) throws Exception {
        var ledger = ledger(policy(), "boot-A");
        String model = operation.equals("wrongModel") ? "different" : "synthetic-chat";
        String method = operation.equals("wrongModel") ? "generateContent" : operation;
        assertThatThrownBy(() -> ledger.reserve(model, request(model+":"+method,"{\"generationConfig\":{\"maxOutputTokens\":768}}")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(Files.exists(journal())).isFalse();
    }

    @Test void dedicatedProfileCannotStartWithoutTheRequiredProcessLedgerPolicy() {
        var environment = new org.springframework.mock.env.MockEnvironment().withProperty("hotel.ai.rag.outbound-ledger-required", "true");
        assertThatThrownBy(() -> new RagRequestBudget(null, environment)).isInstanceOf(IllegalStateException.class).hasNoCause();
    }

    @Test void realYamlProfileKeepsR1OffIsolatesTheDatabaseAndRequiresTheLedger() throws Exception {
        var sources = new org.springframework.boot.env.YamlPropertySourceLoader().load("reviewed-sandbox-profile",
                new org.springframework.core.io.ClassPathResource("application-rag-gemini-local.yaml"));
        var profile = sources.getFirst();
        assertThat(profile.getProperty("hotel.ai.advisor.provider.gemini-enabled")).isEqualTo(false);
        assertThat(profile.getProperty("spring.liquibase.enabled")).isEqualTo(false);
        assertThat(profile.getProperty("spring.datasource.hikari.read-only")).isEqualTo(true);
        assertThat(profile.getProperty("hotel.ai.rag.outbound-ledger-required")).isEqualTo(true);
        assertThat(profile.getProperty("hotel.ai.rag.store-type")).isEqualTo("pgvector");
        assertThat(profile.getProperty("server.address")).isEqualTo("127.0.0.1");
        assertThat(profile.getProperty("logging.level.org.springframework.ai")).isEqualTo("OFF");
        assertThat(profile.getProperty("hotel.ai.rag.provider.chat-model")).isEqualTo("${RAG_GEMINI_CHAT_MODEL}");
        try (var sdk = org.mockito.Mockito.mockStatic(com.google.genai.Client.class)) {
            new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                    .withUserConfiguration(vn.edu.utc.hotel_booking.modules.ai.rag.config.RagProviderConfiguration.class, RagRequestBudget.class)
                    .withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addFirst(profile))
                    .withPropertyValues("RAG_GEMINI_API_KEY=synthetic-key", "RAG_GEMINI_CHAT_MODEL=synthetic-chat",
                            "RAG_GEMINI_EMBEDDING_MODEL=synthetic-embedding", "RAG_GEMINI_EMBEDDING_DIMENSION=3", "RAG_GEMINI_RETRIEVAL_THRESHOLD=0.30")
                    .run(ctx -> assertThat(ctx).hasFailed().getFailure().hasStackTraceContaining("RAG outbound ledger unavailable"));
            sdk.verifyNoInteractions();
        }
    }

    @ParameterizedTest @ValueSource(strings={"known", "unknown"})
    void actualTransportInAChildProcessCannotBypassTheDurableCap(String usage) throws Exception {
        var policy = policy();
        policy.put("capturedAtUtc", Instant.now().minusSeconds(60).toString());
        policy.put("expiresAtUtc", Instant.now().plusSeconds(3600).toString());
        policy.put("maxGenerationCalls", 1);
        Files.writeString(policyPath(), JSON.writeValueAsString(policy));
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        Path javaBin = ProcessHandle.current().info().command()
                .map(Path::of)
                .filter(Files::isExecutable)
                .orElseGet(() -> Files.isExecutable(bin.resolve("java.exe")) ? bin.resolve("java.exe") : bin.resolve("java"));
        String java = javaBin.toString();
        var process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), Probe.class.getName(), usage)
                .redirectErrorStream(true);
        process.environment().clear();
        for (String name : List.of("SystemRoot", "PATH", "USERPROFILE", "HOME", "USER", "TEMP", "TMP")) {
            if (System.getenv(name) != null) process.environment().put(name, System.getenv(name));
        }
        process.environment().put("RAG_GEMINI_LEDGER_REQUIRED", "true");
        process.environment().put("RAG_GEMINI_LEDGER_POLICY", policyPath().toString());
        process.environment().put("RAG_GEMINI_LEDGER_POLICY_SHA256", sha(policyPath()));
        // This child performs loopback HTTP only; it never receives a real provider key.
        process.environment().remove("RAG_GEMINI_API_KEY"); process.environment().remove("GOOGLE_API_KEY"); process.environment().remove("GEMINI_API_KEY");
        var child = process.start();
        try {
            assertThat(child.waitFor(12, TimeUnit.SECONDS)).isTrue();
            String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(child.exitValue()).withFailMessage("Offline ledger probe failed: %s", output).isZero();
            assertThat(output).contains("PASS_CALLS_1").doesNotContain("SYNTHETIC_PRIVATE_TEXT_NOT_IN_LEDGER");
            assertThat(Files.readString(journal())).contains(usage.equals("known") ? "SETTLE" : "HALT");
        } finally { if (child.isAlive()) child.destroyForcibly(); }
    }

    public static final class Probe {
        public static void main(String[] args) throws Exception {
            var calls = new AtomicInteger();
            var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                calls.incrementAndGet(); exchange.getRequestBody().readAllBytes();
                String body = args[0].equals("known") ? "{\"usageMetadata\":{\"promptTokenCount\":7,\"candidatesTokenCount\":13,\"totalTokenCount\":20}}" : "{}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(200, bytes.length);
                try (var output = exchange.getResponseBody()) { output.write(bytes); } exchange.close();
            });
            server.start();
            var client = RagRequestBudget.providerHttpClient("synthetic-chat", 3000).newBuilder().addInterceptor(chain -> {
                if (!"127.0.0.1".equals(chain.request().url().host()) || chain.request().url().port() != server.getAddress().getPort())
                    throw new java.io.IOException("Offline probe blocked external destination");
                return chain.proceed(chain.request());
            }).build();
            var budget = new RagRequestBudget();
            try {
                for (int i=0; i<2; i++) {
                    try {
                        budget.execute(() -> {
                            var request = new Request.Builder().url("http://127.0.0.1:"+server.getAddress().getPort()+"/v1beta/models/synthetic-chat:generateContent")
                                    .post(RequestBody.create("{\"generationConfig\":{\"maxOutputTokens\":768}}", MediaType.get("application/json"))).build();
                            try (var response = client.newCall(request).execute()) { return response.body().string(); }
                        });
                    } catch (IllegalStateException | RagRequestBudget.UnavailableException ignored) { }
                }
                if (calls.get()!=1 || budget.activeRequests()!=0) throw new AssertionError("Transport cap or slot leak");
                System.out.println("PASS_CALLS_1");
            } finally { server.stop(0); client.connectionPool().evictAll(); client.dispatcher().executorService().shutdownNow(); }
        }
    }
}
