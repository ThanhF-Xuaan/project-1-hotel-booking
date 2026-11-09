package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okio.Buffer;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

/** Durable, fail-closed campaign cap. It is a local single-host ledger, not a cloud budget alert. */
public final class RagOutboundLedger {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final String BOOT = ProcessHandle.current().pid() + "-" + ManagementFactory.getRuntimeMXBean().getStartTime();
    private final Path directory;
    private final Path journal;
    private final String policyHash;
    private final JsonNode policy;
    private final Clock clock;
    private final String boot;

    public RagOutboundLedger(Path policyPath, String expectedHash, Clock clock, String bootIdentity) {
        try {
            if (!policyPath.isAbsolute() || Files.isSymbolicLink(policyPath) || Files.size(policyPath) > 65_536
                    || !expectedHash.matches("[a-f0-9]{64}") || !bootIdentity.matches("[a-zA-Z0-9-]{1,100}")) throw unavailable();
            byte[] bytes = Files.readAllBytes(policyPath);
            if (!hash(bytes).equals(expectedHash)) throw unavailable();
            this.policy = JSON.readTree(bytes); this.policyHash = expectedHash; this.clock = clock; this.boot = bootIdentity;
            this.directory = policyPath.getParent().toRealPath();
            String campaign = policy.path("campaignId").asText();
            if (!campaign.matches("[a-f0-9]{32}") || !"rag-gemini-ledger-v1".equals(policy.path("schemaVersion").asText())
                    || !"USD".equals(policy.path("currency").asText()) || !policy.path("allBillableTokensCovered").asBoolean(false)) throw unavailable();
            this.journal = directory.resolve(campaign + ".ledger.jsonl");
            checkPolicy();
        } catch (Exception failure) { throw unavailable(); }
    }

    public static void requireConfiguredPolicy() {
        if (!"true".equals(System.getenv("RAG_GEMINI_LEDGER_REQUIRED"))) throw unavailable();
        configured();
    }

    private static RagOutboundLedger configured() {
        try {
            return new RagOutboundLedger(Path.of(System.getenv("RAG_GEMINI_LEDGER_POLICY")),
                    System.getenv("RAG_GEMINI_LEDGER_POLICY_SHA256"), Clock.systemUTC(), BOOT);
        } catch (Exception failure) { throw unavailable(); }
    }

    public static Reservation beforeCall(String model, Request request) {
        if (!"true".equals(System.getenv("RAG_GEMINI_LEDGER_REQUIRED"))) return Reservation.disabled();
        return configured().reserve(model, request);
    }

    public Reservation reserve(String model, Request request) {
        try {
            checkPolicy();
            if (!"POST".equals(request.method()) || request.body() == null || request.body().isOneShot() || request.body().isDuplex()) throw unavailable();
            Buffer body = new Buffer(); request.body().writeTo(body);
            if (body.size() > 1_048_576) throw unavailable();
            JsonNode data = JSON.readTree(body.readByteArray());
            String path = request.url().encodedPath();
            int inputs = 0, generations = 0; JsonNode price;
            if (path.endsWith(":batchEmbedContents")) {
                if (!data.path("requests").isArray()) throw unavailable();
                inputs = data.path("requests").size();
                if (inputs < 1 || inputs > 64) throw unavailable();
                price = policy.path("embedding");
            } else if (path.endsWith(":embedContent")) {
                inputs = 1; price = policy.path("embedding");
            } else if (path.endsWith(":generateContent")) {
                generations = 1; price = policy.path("generation");
                int requested = data.path("generationConfig").path("maxOutputTokens").asInt(-1);
                if (requested < 1 || requested > price.path("outputTokenBound").asInt()) throw unavailable();
            } else throw unavailable();
            if (!price.path("model").asText().equals(model) || !path.endsWith("/" + model + (inputs > 0
                    ? (path.endsWith(":batchEmbedContents") ? ":batchEmbedContents" : ":embedContent") : ":generateContent"))) throw unavailable();
            long inputBound = Math.multiplyExact(price.path("inputTokenBound").asLong(), inputs > 0 ? inputs : 1);
            long outputBound = generations > 0 ? price.path("outputTokenBound").asLong() : 0;
            BigDecimal reserved = cost(price, inputBound, outputBound);
            String id = UUID.randomUUID().toString();
            int embeddingInputs = inputs, generationCalls = generations;
            transact(state -> {
                if (state.halted) throw unavailable();
                if (state.pending.values().stream().anyMatch(entry -> !boot.equals(entry.path("boot").asText()))) {
                    append(state, Map.of("type", "HALT", "reason", "UNRESOLVED_RESTART")); throw unavailable();
                }
                if (state.inputs + embeddingInputs > limit("maxEmbeddingInputs", 200)
                        || state.generations + generationCalls > limit("maxGenerationCalls", 120)
                        || state.cost.add(reserved).compareTo(money(policy.path("limitUsd"), true)) > 0) throw unavailable();
                append(state, Map.ofEntries(Map.entry("type", "RESERVE"), Map.entry("id", id), Map.entry("boot", boot),
                        Map.entry("inputs", embeddingInputs), Map.entry("generations", generationCalls), Map.entry("maxCost", reserved.toPlainString()),
                        Map.entry("inputBound", inputBound), Map.entry("outputBound", outputBound), Map.entry("model", model)));
                return null;
            });
            return new Reservation(this, id, price, inputBound, outputBound, reserved);
        } catch (Exception failure) { throw unavailable(); }
    }

    private void checkPolicy() throws Exception {
        Instant now = clock.instant(), captured = Instant.parse(policy.path("capturedAtUtc").asText());
        Instant expires = Instant.parse(policy.path("expiresAtUtc").asText());
        if (captured.isAfter(now) || !expires.isAfter(now) || expires.isAfter(captured.plusSeconds(86_400))
                || !"https://ai.google.dev/gemini-api/docs/pricing".equals(policy.path("pricingSource").asText())
                || money(policy.path("limitUsd"), true).compareTo(new BigDecimal("5")) > 0) throw unavailable();
        Path snapshot = directory.resolve("pricing-snapshot.html");
        if (Files.isSymbolicLink(snapshot) || Files.size(snapshot) > 8_388_608
                || !hash(Files.readAllBytes(snapshot)).equals(policy.path("pricingSnapshotSha256").asText())) throw unavailable();
        limit("maxEmbeddingInputs", 200); limit("maxGenerationCalls", 120);
        for (String type : List.of("embedding", "generation")) {
            JsonNode price = policy.path(type);
            if (!price.path("model").asText().matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}")
                    || !price.path("inputTokenBound").canConvertToLong() || price.path("inputTokenBound").asLong() < 1
                    || price.path("inputTokenBound").asLong() > 10_000_000
                    || !price.path("boundsAndWorstCaseRatesApproved").asBoolean(false)) throw unavailable();
            money(price.path("inputUsdPerMillion"), true);
            money(price.path("outputUsdPerMillion"), "generation".equals(type));
        }
        long output = policy.path("generation").path("outputTokenBound").asLong(-1);
        if (output != 768) throw unavailable();
    }

    private int limit(String field, int hardMaximum) {
        JsonNode value = policy.path(field);
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 1 || value.intValue() > hardMaximum) throw unavailable();
        return value.intValue();
    }

    private static BigDecimal money(JsonNode value, boolean positive) {
        if (!value.isTextual() || !value.asText().matches("[0-9]{1,7}(?:\\.[0-9]{1,12})?")) throw unavailable();
        BigDecimal amount = new BigDecimal(value.asText());
        if (amount.signum() < 0 || (positive && amount.signum() == 0)) throw unavailable();
        return amount;
    }

    private static BigDecimal cost(JsonNode price, long input, long output) {
        return money(price.path("inputUsdPerMillion"), true).multiply(BigDecimal.valueOf(input))
                .add(money(price.path("outputUsdPerMillion"), false).multiply(BigDecimal.valueOf(output)))
                .divide(MILLION, 12, RoundingMode.CEILING);
    }

    private interface Work<T> { T run(State state) throws Exception; }
    private <T> T transact(Work<T> work) throws Exception {
        if (Files.isSymbolicLink(journal)) throw unavailable();
        try (var channel = FileChannel.open(journal, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw unavailable(); // no unbounded wait while holding an ask concurrency slot
            if (channel.size() > 8_388_608) throw unavailable();
            ByteBuffer bytes = ByteBuffer.allocate((int) channel.size());
            while (bytes.hasRemaining()) if (channel.read(bytes) < 0) throw unavailable();
            String content = new String(bytes.array(), StandardCharsets.UTF_8);
            if (!content.isEmpty() && !content.endsWith("\n")) throw unavailable();
            State state = new State(channel);
            for (String line : content.split("\n")) {
                if (line.isEmpty()) continue;
                JsonNode envelope = JSON.readTree(line); String event = envelope.path("event").asText();
                if (!envelope.path("previous").asText().equals(state.previous)
                        || !envelope.path("hash").asText().equals(hash((state.previous + "\n" + event).getBytes(StandardCharsets.UTF_8)))) throw unavailable();
                JsonNode row = JSON.readTree(event);
                if (!row.path("policySha256").asText().equals(policyHash)) throw unavailable();
                apply(state, row); state.previous = envelope.path("hash").asText();
            }
            return work.run(state);
        }
    }

    private void append(State state, Map<String, Object> event) throws Exception {
        Map<String, Object> values = new TreeMap<>(event); values.put("policySha256", policyHash); values.put("atUtc", clock.instant().toString());
        String row = JSON.writeValueAsString(values), digest = hash((state.previous + "\n" + row).getBytes(StandardCharsets.UTF_8));
        byte[] line = (JSON.writeValueAsString(Map.of("previous", state.previous, "event", row, "hash", digest)) + "\n").getBytes(StandardCharsets.UTF_8);
        state.channel.position(state.channel.size()); ByteBuffer buffer = ByteBuffer.wrap(line);
        while (buffer.hasRemaining()) state.channel.write(buffer);
        state.channel.force(true); apply(state, JSON.readTree(row)); state.previous = digest;
    }

    private static void apply(State state, JsonNode row) {
        switch (row.path("type").asText()) {
            case "RESERVE" -> {
                String id = row.path("id").asText();
                if (state.pending.putIfAbsent(id, row) != null) throw unavailable();
                state.inputs += row.path("inputs").asInt(); state.generations += row.path("generations").asInt();
                state.cost = state.cost.add(new BigDecimal(row.path("maxCost").asText()));
            }
            case "SETTLE" -> {
                JsonNode reservation = state.pending.remove(row.path("id").asText()); if (reservation == null) throw unavailable();
                state.cost = state.cost.subtract(new BigDecimal(reservation.path("maxCost").asText())).add(new BigDecimal(row.path("actualCost").asText()));
            }
            case "HALT" -> state.halted = true;
            default -> throw unavailable();
        }
    }

    private static final class State {
        final FileChannel channel;
        String previous = "0".repeat(64);
        int inputs, generations;
        BigDecimal cost = BigDecimal.ZERO;
        boolean halted;
        final Map<String, JsonNode> pending = new HashMap<>();
        State(FileChannel channel) { this.channel = channel; }
    }

    public static final class Reservation {
        private final RagOutboundLedger ledger;
        private final String id;
        private final JsonNode price;
        private final long inputBound, outputBound;
        private final BigDecimal maximum;
        private boolean completed;
        private Reservation(RagOutboundLedger ledger, String id, JsonNode price, long input, long output, BigDecimal maximum) {
            this.ledger = ledger; this.id = id; this.price = price; this.inputBound = input; this.outputBound = output; this.maximum = maximum;
        }
        static Reservation disabled() { return new Reservation(null, null, null, 0, 0, BigDecimal.ZERO); }
        public synchronized void complete(long input, long output, long total) {
            if (ledger == null || completed) return;
            completed = true;
            try {
                ledger.transact(state -> {
                    if (!state.pending.containsKey(id)) throw unavailable();
                    // Embedding has no billable output; missing that field is permitted, missing input/total is not.
                    long billedOutput = outputBound == 0 ? 0 : output;
                    if (input < 0 || billedOutput < 0 || total < input + billedOutput || input > inputBound
                            || billedOutput > outputBound || total > inputBound + outputBound || (outputBound == 0 && total != input)) {
                        ledger.append(state, Map.of("type", "HALT", "reason", "UNKNOWN_OR_OUT_OF_BOUND_USAGE", "id", id));
                        throw unavailable();
                    }
                    // Account all reported billable tokens, including thinking/other output, at the worst output rate.
                    billedOutput = outputBound == 0 ? 0 : total - input;
                    BigDecimal actual = cost(price, input, billedOutput);
                    if (billedOutput > outputBound || actual.compareTo(maximum) > 0) {
                        ledger.append(state, Map.of("type", "HALT", "reason", "RESERVATION_EXCEEDED", "id", id)); throw unavailable();
                    }
                    ledger.append(state, Map.of("type", "SETTLE", "id", id, "inputTokens", input,
                            "outputTokens", billedOutput, "totalTokens", total, "actualCost", actual.toPlainString()));
                    return null;
                });
            } catch (Exception failure) { throw unavailable(); }
        }
    }

    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static IllegalStateException unavailable() { return new IllegalStateException("RAG outbound ledger unavailable or campaign limit reached"); }
}
