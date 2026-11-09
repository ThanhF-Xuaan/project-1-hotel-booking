package vn.edu.utc.hotel_booking.modules.ai.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.RagProviderProperties;
import vn.edu.utc.hotel_booking.modules.ai.rag.controller.PublicRagKnowledgeController;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.*;
import vn.edu.utc.hotel_booking.modules.organization.service.PublicHotelEligibilityReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeminiRagRetrievalTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 20);
    private static final String MODEL = "synthetic-gemini-unit-fixture";
    private static final List<Double> GRID = List.of(0.30, 0.40, 0.50, 0.60);
    private final InMemoryScopedVectorStore store = spy(new InMemoryScopedVectorStore(3));
    private final RagEmbeddingPort port = mock(RagEmbeddingPort.class);

    GeminiRagRetrievalTest() {
        when(port.isExternalProvider()).thenReturn(true);
        when(port.getDimension()).thenReturn(3);
        when(port.getModelId()).thenReturn(MODEL);
        when(port.embedQuery(anyString())).thenReturn(new float[]{1, 0, 0});
    }

    private static float[] vector(double similarity) {
        return new float[]{(float) similarity, (float) Math.sqrt(1 - similarity * similarity), 0};
    }

    private static RagQueryRequestDto query(Integer hotel, String question, LocalDate date) {
        return RagQueryRequestDto.builder().hotelId(hotel).question(question).asOfDate(date).topK(5).build();
    }

    private static RagProviderProperties threshold(double value) {
        var properties = new RagProviderProperties(); properties.setRetrievalThreshold(value); return properties;
    }

    private ScopedRagRetrievalService retrieval(double value) { return new ScopedRagRetrievalService(store, port, threshold(value)); }

    private KnowledgeDocumentVersion publish(String source, Integer hotel, int version, LocalDate from, LocalDate to,
                                             RagVersionStatus status, float[]... vectors) {
        UUID docId = UUID.nameUUIDFromBytes((source + ":" + hotel + ":" + version).getBytes(StandardCharsets.UTF_8));
        var scope = hotel == null ? RagScopeType.CHAIN_PUBLIC : RagScopeType.HOTEL_SCOPED;
        var doc = KnowledgeDocumentVersion.builder().documentId(docId).sourceId(source).hotelId(hotel).scopeType(scope)
                .version(version).category(RagDocumentCategory.POLICY).embeddingModelId(MODEL).embeddingDimension(3)
                .effectiveFrom(from).effectiveTo(to).chunkingVersion("paragraph-v1").title("Synthetic policy")
                .rawContent("Synthetic public policy.").build();
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (int i = 0; i < vectors.length; i++) chunks.add(KnowledgeChunk.builder().documentId(docId)
                .chunkId(UUID.nameUUIDFromBytes((docId + ":" + i).getBytes(StandardCharsets.UTF_8)))
                .sourceId(source).hotelId(hotel).scopeType(scope).version(version).category(doc.getCategory())
                .chunkIndex(i).embedding(vectors[i]).content(doc.getRawContent()).sanitizedContent(doc.getRawContent())
                .effectiveFrom(from).effectiveTo(to).build());
        assertThat(store.stageDocument(doc, chunks).getStatus()).isEqualTo(RagVersionStatus.STAGED);
        if (status != RagVersionStatus.STAGED) {
            assertThat(store.activateVersion(scope, hotel, source, version)).isTrue();
            if (status == RagVersionStatus.SUPERSEDED) { doc.setStatus(status); chunks.forEach(chunk -> chunk.setStatus(status)); }
        }
        return doc;
    }

    @Test void providerThresholdIsAppliedInclusivelyAndNeverRelaxedOnEmptyResults() {
        publish("MATCH", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.50));
        assertThat(retrieval(.50).retrieve(query(1, "Synthetic policy", DATE))).hasSize(1);
        assertThat(retrieval(.60).retrieve(query(1, "Synthetic policy", DATE))).isEmpty();
        assertThat(retrieval(.60).retrieve(query(1, "Synthetic policy", DATE))).isEmpty();
    }

    @Test void configurationIsSnapshottedRatherThanMutatingThresholdMidRequest() {
        publish("MATCH", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.50));
        var properties = threshold(.60);
        var service = new ScopedRagRetrievalService(store, port, properties);
        properties.setRetrievalThreshold(.30);
        assertThat(service.retrieve(query(1, "Synthetic policy", DATE))).isEmpty();
    }

    @ParameterizedTest @ValueSource(doubles = {0, .29, .35, .61, 1, Double.NaN, Double.POSITIVE_INFINITY})
    void invalidThresholdCannotDisableFiltering(double value) {
        assertThatThrownBy(() -> retrieval(value)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest @ValueSource(strings = {"Mấy giờ tôi có thể nhận phòng?", "Lúc nào được làm thủ tục vào ở?", "Xin khung giờ check-in của cơ sở."})
    void vietnameseParaphrasesUseTheProvidedQueryEmbeddingWithoutKeywordRewriting(String question) {
        // Synthetic equal vectors prove routing/ranking only, never Gemini semantic quality.
        publish("CHECKIN", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.70));
        assertThat(retrieval(.60).retrieve(query(1, question, DATE))).hasSize(1);
        verify(port).embedQuery(question);
    }

    @Test void hotelPrecedesChainBeforeSimilarityAndKeepsEveryWinningVersionChunk() {
        publish("POLICY", null, 9, null, null, RagVersionStatus.ACTIVE, vector(.99));
        publish("POLICY", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.70), vector(.80));
        var result = retrieval(.60).retrieve(query(1, "Synthetic policy", DATE));
        assertThat(result).hasSize(2).allSatisfy(item -> {
            assertThat(item.getChunk().getHotelId()).isEqualTo(1); assertThat(item.getChunk().getVersion()).isEqualTo(1);
        });
        verify(store, times(1)).getDocument(RagScopeType.HOTEL_SCOPED, 1, "POLICY", 1);
        verify(store, never()).getDocument(RagScopeType.CHAIN_PUBLIC, null, "POLICY", 9);
    }

    @Test void stagedFutureExpiredAndReplacedVersionsRespectExistingDateAndStatusRules() {
        publish("POLICY", 1, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 14), RagVersionStatus.ACTIVE, vector(.80));
        publish("POLICY", 1, 2, LocalDate.of(2026, 11, 15), null, RagVersionStatus.ACTIVE, vector(.70));
        publish("STAGED", 1, 1, null, null, RagVersionStatus.STAGED, vector(.99));
        publish("FUTURE", 1, 1, DATE.plusDays(1), null, RagVersionStatus.ACTIVE, vector(.99));
        publish("EXPIRED", 1, 1, null, DATE.minusDays(1), RagVersionStatus.ACTIVE, vector(.99));
        var current = retrieval(.60).retrieve(query(1, "Synthetic policy", DATE));
        assertThat(current).hasSize(1); assertThat(current.getFirst().getChunk().getVersion()).isEqualTo(2);
        var historical = retrieval(.60).retrieve(query(1, "Synthetic policy", LocalDate.of(2026, 11, 14)));
        assertThat(historical.stream().filter(c -> c.getChunk().getSourceId().equals("POLICY")).findFirst().orElseThrow()
                .getChunk().getVersion()).isEqualTo(1);
    }

    @Test void activePrecedesSupersededThenLaterEffectiveDateAndHigherVersionWin() {
        publish("STATUS", 1, 8, DATE.minusDays(2), null, RagVersionStatus.SUPERSEDED, vector(.99));
        publish("STATUS", 1, 2, DATE.minusDays(5), null, RagVersionStatus.ACTIVE, vector(.70));
        publish("DATE", 1, 1, DATE.minusDays(2), null, RagVersionStatus.SUPERSEDED, vector(.80));
        publish("DATE", 1, 9, DATE.minusDays(5), null, RagVersionStatus.SUPERSEDED, vector(.99));
        publish("VERSION", 1, 1, DATE.minusDays(2), null, RagVersionStatus.SUPERSEDED, vector(.99));
        publish("VERSION", 1, 2, DATE.minusDays(2), null, RagVersionStatus.SUPERSEDED, vector(.70));
        var result = retrieval(.60).retrieve(query(1, "Synthetic policy", DATE));
        assertThat(result.stream().collect(java.util.stream.Collectors.toMap(x -> x.getChunk().getSourceId(), x -> x.getChunk().getVersion())))
                .containsEntry("STATUS", 2).containsEntry("DATE", 1).containsEntry("VERSION", 2);
    }

    @Test void foreignChunksAreFilteredEvenWhenStoreReturnsPoisonedCandidates() {
        publish("FOREIGN", 2, 1, null, null, RagVersionStatus.ACTIVE, vector(.99));
        var foreign = store.findCandidateChunks(null, 2, DATE);
        doReturn(foreign).when(store).findCandidateChunks(null, 1, DATE);
        assertThat(retrieval(.30).retrieve(query(1, "Synthetic policy", DATE))).isEmpty();
        verify(port, never()).embedQuery(anyString());
        verify(store, never()).getDocument(RagScopeType.HOTEL_SCOPED, 2, "FOREIGN", 1);
    }

    @Test void nullHotelOnlyReturnsChainPublicEvidence() {
        publish("HOTEL", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.99));
        publish("CHAIN", null, 1, null, null, RagVersionStatus.ACTIVE, vector(.70));
        assertThat(retrieval(.60).retrieve(query(null, "Synthetic policy", DATE)))
                .hasSize(1).allSatisfy(x -> assertThat(x.getChunk().getScopeType()).isEqualTo(RagScopeType.CHAIN_PUBLIC));
    }

    @Test void storeDimensionMismatchIsAFailureEvenWhenTheCorpusIsEmpty() {
        doReturn(768).when(store).getExpectedDimension();
        assertThatThrownBy(() -> retrieval(.60).retrieve(query(1, "Synthetic policy", DATE)))
                .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class);
        verify(port, never()).embedQuery(anyString());
    }

    @Test void topKAndTieBreakRemainStableIndependentOfCandidateOrder() {
        publish("B", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.80));
        publish("A", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.80), vector(.80));
        var request = query(1, "Synthetic policy", DATE); request.setTopK(2);
        var service = retrieval(.60); var before = service.retrieve(request);
        var shuffled = new ArrayList<>(store.findCandidateChunks(null, 1, DATE)); Collections.reverse(shuffled);
        doReturn(shuffled).when(store).findCandidateChunks(null, 1, DATE);
        assertThat(service.retrieve(request).stream().map(x -> x.getChunk().getChunkId()).toList())
                .containsExactlyElementsOf(before.stream().map(x -> x.getChunk().getChunkId()).toList());
        assertThat(before.stream().map(x -> x.getChunk().getSourceId()).toList()).containsExactly("A", "A");
        assertThat(before.stream().map(x -> x.getChunk().getChunkIndex()).toList()).containsExactly(0, 1);
    }

    @Test void uuidTieBreakIsDeterministicForAStoreFaultWithDuplicateChunkPositions() {
        publish("TIE", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.80), vector(.80));
        var chunks = store.findCandidateChunks(null, 1, DATE); chunks.forEach(row -> row.setChunkIndex(0));
        var expected = chunks.stream().map(KnowledgeChunk::getChunkId).sorted().toList();
        var reversed = new ArrayList<>(chunks); Collections.reverse(reversed);
        doReturn(reversed).when(store).findCandidateChunks(null, 1, DATE);
        assertThat(retrieval(.60).retrieve(query(1, "Synthetic policy", DATE)).stream()
                .map(row -> row.getChunk().getChunkId()).toList()).containsExactlyElementsOf(expected);
    }

    @Test void capAcceptsOneThousandAndRejectsOneThousandAndOneWithoutQuerySpend() {
        for (int i = 0; i < 16; i++) {
            float[][] vectors = new float[Math.min(64, 1000 - i * 64)][];
            Arrays.setAll(vectors, j -> vector(.80));
            publish("CAP-" + i, 1, 1, null, null, RagVersionStatus.ACTIVE, vectors);
        }
        var request = query(1, "Synthetic policy", DATE); request.setTopK(Integer.MAX_VALUE);
        assertThat(retrieval(.60).retrieve(request)).hasSize(5);
        var candidates = new ArrayList<>(store.findCandidateChunks(null, 1, DATE)); candidates.add(candidates.getFirst());
        doReturn(candidates).when(store).findCandidateChunks(null, 1, DATE); clearInvocations(port);
        assertThatThrownBy(() -> retrieval(.60).retrieve(request)).isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class);
        verify(port, never()).embedQuery(anyString());
    }

    static Stream<float[]> invalidVectors() {
        return Stream.of(new float[]{1, 0}, new float[]{0, 0, 0}, new float[]{Float.NaN, 0, 1},
                new float[]{Float.POSITIVE_INFINITY, 0, 0}, new float[]{2, 0, 0});
    }

    @ParameterizedTest @MethodSource("invalidVectors")
    void malformedSelectedVectorsFailEvenBelowThreshold(float[] invalid) {
        publish("BAD", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.10));
        // Store admission already rejects wrong dimensions. Poison only the read boundary.
        store.findCandidateChunks(null, 1, DATE).forEach(chunk -> chunk.setEmbedding(invalid));
        assertThatThrownBy(() -> retrieval(.60).retrieve(query(1, "Synthetic policy", DATE)))
                .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class);
        verify(port, never()).embedQuery(anyString());
    }

    @ParameterizedTest @MethodSource("invalidVectors")
    void malformedQueryVectorsAreNeverReportedAsNoEvidence(float[] invalid) {
        publish("POLICY", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.80));
        when(port.embedQuery(anyString())).thenReturn(invalid);
        assertThatThrownBy(() -> retrieval(.60).retrieve(query(1, "Synthetic policy", DATE)))
                .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class);
    }

    @ParameterizedTest @ValueSource(strings = {"model", "dimension", "id", "missing"})
    void documentIdentityMismatchFailsBeforeQueryEmbedding(String corruption) {
        var doc = publish("POLICY", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.10));
        switch (corruption) {
            case "model" -> doc.setEmbeddingModelId("different-model-same-dimension");
            case "dimension" -> doc.setEmbeddingDimension(256);
            case "id" -> doc.setDocumentId(UUID.randomUUID());
            case "missing" -> doReturn(Optional.empty()).when(store).getDocument(RagScopeType.HOTEL_SCOPED, 1, "POLICY", 1);
        }
        assertThatThrownBy(() -> retrieval(.60).retrieve(query(1, "Synthetic policy", DATE)))
                .isInstanceOf(RagEmbeddingPort.EmbeddingUnavailableException.class);
        verify(port, never()).embedQuery(anyString());
    }

    @Test void dependencyMismatchUsesPublic503RatherThanSuccessfulInsufficientEvidence() {
        var doc = publish("POLICY", 1, 1, null, null, RagVersionStatus.ACTIVE, vector(.10)); doc.setEmbeddingModelId("wrong-model");
        var eligibility = mock(PublicHotelEligibilityReader.class); when(eligibility.isActivePublicHotel((short) 1)).thenReturn(true);
        var answer = new RagGroundedAnswerService(retrieval(.60), new RagCorpusGovernanceValidator());
        var response = new PublicRagKnowledgeController(answer, eligibility).ask(query(1, "Giờ nhận phòng?", DATE));
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().getMessage()).startsWith("RAG_TEMP_UNAVAILABLE");
        assertThat(response.getBody().getResult().getCitations()).isEmpty();
    }

    @Test void gridSelectionChoosesHighestPassingRecallAndCannotInventAThreshold() {
        assertThat(selectThreshold(Map.of(.30, 1.0, .40, .95, .50, .90, .60, .80))).isEqualTo(.50);
        assertThat(selectThreshold(Map.of(.30, 1.0, .40, 1.0, .50, 1.0, .60, 1.0))).isEqualTo(.60);
        assertThatThrownBy(() -> selectThreshold(Map.of(.30, .80, .40, .70, .50, .60, .60, .50)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> selectThreshold(Map.of(.30, 1.0))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> selectThreshold(Map.of(.30, 1.0, .40, Double.NaN, .50, 1.0, .60, 1.0)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static double selectThreshold(Map<Double, Double> recalls) {
        if (!recalls.keySet().equals(new HashSet<>(GRID)) || recalls.values().stream()
                .anyMatch(value -> value == null || !Double.isFinite(value) || value < 0 || value > 1)) {
            throw new IllegalStateException("Incomplete or invalid calibration measurements");
        }
        return GRID.stream().filter(t -> recalls.get(t) >= .90).mapToDouble(Double::doubleValue).max()
                .orElseThrow(() -> new IllegalStateException("No threshold achieved recall >= 90%; review required"));
    }

    /**
     * Offline replay only, NEVER an SDK caller. Requires a separately approved snapshot of
     * actual normalized Gemini adapter outputs: target/rag-gemini-dev-snapshot.json.
     * Snapshot schema: {embeddingModel,dimension,corpusSha256,devSha256,documents:{documentRef:[float...]},
     * queries:{DEV-id:[float...]}}. Documents must be the 13 single-paragraph TASK-02 versions;
     * queries must contain exactly the 15 development cases marked retrievalEligible.
     * Approval is an operator gate, not a human signature fabricated by this test.
     * T12_RAG_RETRIEVAL_CALIBRATION=true, T12_RAG_CALIBRATION_SNAPSHOT_APPROVED=true,
     * T12_RAG_CALIBRATION_SNAPSHOT_SHA256=<approved exact bytes hash>.
     * No snapshot has been acquired/approved in TASK-05. Default skip means NOT RUN.
     */
    @Test @EnabledIfEnvironmentVariable(named = "T12_RAG_RETRIEVAL_CALIBRATION", matches = "true")
    void calibrateApprovedDevSnapshotAndLockResultBeforeAcceptance() throws Exception {
        assertThat(System.getenv("T12_RAG_CALIBRATION_SNAPSHOT_APPROVED")).isEqualTo("true");
        var mapper = new ObjectMapper(); var path = Path.of("target/rag-gemini-dev-snapshot.json");
        assertThat(Files.size(path)).isLessThan(8_000_000L);
        byte[] bytes = Files.readAllBytes(path);
        assertThat(hash(bytes)).isEqualTo(System.getenv("T12_RAG_CALIBRATION_SNAPSHOT_SHA256"));
        JsonNode snapshot = mapper.readTree(bytes);
        String model = snapshot.path("embeddingModel").asText(); int dimension = snapshot.path("dimension").asInt();
        assertThat(model).matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}"); assertThat(dimension).isBetween(1, 3072);
        Path artifacts = Path.of("..", "eval", "t12", "r2", "gemini-v1");
        byte[] corpusBytes = Files.readAllBytes(artifacts.resolve("corpus.json"));
        byte[] devBytes = Files.readAllBytes(artifacts.resolve("dev-cases.jsonl"));
        assertThat(snapshot.path("corpusSha256").asText()).isEqualTo(hash(corpusBytes));
        assertThat(snapshot.path("devSha256").asText()).isEqualTo(hash(devBytes));
        var manifest = mapper.readTree(artifacts.resolve("manifest.json").toFile());
        assertThat(hash(corpusBytes)).isEqualTo(manifest.path("fileHashes").path("corpus.json").path("sha256").asText());
        assertThat(hash(devBytes)).isEqualTo(manifest.path("fileHashes").path("dev-cases.jsonl").path("sha256").asText());
        List<JsonNode> cases = new ArrayList<>();
        for (String line : new String(devBytes, StandardCharsets.UTF_8).lines().toList()) {
            var item = mapper.readTree(line); if (item.path("scoring").path("retrievalEligible").asBoolean()) cases.add(item);
        }
        assertThat(cases).hasSize(15); assertThat(snapshot.path("queries").size()).isEqualTo(15);
        var memory = new InMemoryScopedVectorStore(dimension);
        var documents = mapper.readTree(corpusBytes).path("documents"); assertThat(documents).hasSize(13);
        assertThat(snapshot.path("documents").size()).isEqualTo(13);
        Map<UUID, String> refs = new HashMap<>();
        for (JsonNode item : documents) {
            String ref = item.path("documentRef").asText(); UUID id = UUID.nameUUIDFromBytes(ref.getBytes(StandardCharsets.UTF_8));
            refs.put(id, ref); float[] vector = readVector(snapshot.path("documents").path(ref), dimension);
            var scope = RagScopeType.valueOf(item.path("scopeType").asText());
            Integer hotel = item.path("hotelId").isNull() ? null : item.path("hotelId").asInt();
            LocalDate from = LocalDate.parse(item.path("effectiveFrom").asText());
            LocalDate to = item.path("effectiveTo").isNull() ? null : LocalDate.parse(item.path("effectiveTo").asText());
            String source = item.path("sourceId").asText(); int version = item.path("version").asInt();
            var category = RagDocumentCategory.valueOf(item.path("category").asText());
            String content = item.path("content").asText(); assertThat(content).doesNotContain("\n\n");
            var doc = KnowledgeDocumentVersion.builder().documentId(id).sourceId(source).scopeType(scope).hotelId(hotel)
                    .version(version).category(category).embeddingModelId(model).embeddingDimension(dimension)
                    .effectiveFrom(from).effectiveTo(to).rawContent(content).build();
            var chunk = KnowledgeChunk.builder().documentId(id).chunkId(UUID.nameUUIDFromBytes((ref + ":0").getBytes(StandardCharsets.UTF_8)))
                    .sourceId(source).scopeType(scope).hotelId(hotel).version(version).category(category).chunkIndex(0)
                    .effectiveFrom(from).effectiveTo(to).content(content).sanitizedContent(content).embedding(vector).build();
            assertThat(memory.stageDocument(doc, List.of(chunk)).getStatus()).isEqualTo(RagVersionStatus.STAGED);
            assertThat(memory.activateVersion(scope, hotel, source, version)).isTrue();
        }
        Map<Double, Double> recalls = new LinkedHashMap<>();
        for (double threshold : GRID) {
            double total = 0;
            for (JsonNode item : cases) {
                float[] queryVector = readVector(snapshot.path("queries").path(item.path("id").asText()), dimension);
                RagEmbeddingPort replay = new RagEmbeddingPort() {
                    public float[] embedText(String text) { throw new UnsupportedOperationException("Read-only query replay"); }
                    public List<float[]> embedBatch(List<String> texts) { throw new UnsupportedOperationException("Read-only query replay"); }
                    public float[] embedQuery(String text) { return queryVector.clone(); }
                    public int getDimension() { return dimension; }
                    public String getModelId() { return model; }
                    public boolean isExternalProvider() { return true; }
                };
                JsonNode request = item.path("request");
                var result = new ScopedRagRetrievalService(memory, replay, threshold(threshold)).retrieve(query(
                        request.path("hotelId").isNull() ? null : request.path("hotelId").asInt(),
                        request.path("question").asText(), LocalDate.parse(request.path("asOfDate").asText())));
                Set<String> retrieved = new HashSet<>(); result.forEach(row -> retrieved.add(refs.get(row.getChunk().getDocumentId())));
                JsonNode required = item.path("expected").path("requiredEvidence"); assertThat(required.size()).isPositive();
                long found = java.util.stream.StreamSupport.stream(required.spliterator(), false)
                        .map(JsonNode::asText).filter(retrieved::contains).count();
                total += (double) found / required.size();
            }
            recalls.put(threshold, total / cases.size());
        }
        double selected = selectThreshold(recalls);
        Path receipt = Path.of("target/rag-gemini-retrieval-calibration.json");
        assertThat(Files.exists(receipt)).as("Do not overwrite a prior calibration receipt").isFalse();
        var result = Map.of(
                "status", "MEASURED_DEV_SNAPSHOT_ONLY_NOT_ACCEPTANCE_APPROVAL", "snapshotSha256", hash(bytes),
                "corpusSha256", hash(corpusBytes), "devSha256", hash(devBytes), "embeddingModel", model,
                "dimension", dimension, "recallAt5", recalls, "selectedThreshold", selected,
                "configuration", "hotel.ai.rag.provider.retrieval-threshold=" + selected,
                "sourceSha256", Map.of(
                        "ScopedRagRetrievalService.java", hash(Files.readAllBytes(Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/service/ScopedRagRetrievalService.java"))),
                        "RagProviderProperties.java", hash(Files.readAllBytes(Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/rag/config/RagProviderProperties.java")))));
        try (var output = Files.newOutputStream(receipt, StandardOpenOption.CREATE_NEW)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(output, result);
        }
    }

    private static float[] readVector(JsonNode node, int dimension) {
        assertThat(node.isArray()).isTrue(); assertThat(node.size()).isEqualTo(dimension);
        float[] vector = new float[dimension]; double squared = 0;
        for (int i = 0; i < dimension; i++) {
            assertThat(node.get(i).isNumber()).isTrue(); vector[i] = node.get(i).floatValue();
            assertThat(Float.isFinite(vector[i])).isTrue(); squared += (double) vector[i] * vector[i];
        }
        assertThat(Math.abs(squared - 1)).isLessThanOrEqualTo(.001); return vector;
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
