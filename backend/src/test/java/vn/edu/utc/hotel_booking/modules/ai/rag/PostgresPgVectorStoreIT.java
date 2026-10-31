package vn.edu.utc.hotel_booking.modules.ai.rag;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.RagVectorStoreConfiguration;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.InMemoryScopedVectorStore;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.HybridRagEmbeddingService;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.PostgresPgVectorStore;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Opt-in, synthetic-only: separate loopback t12_pgvector_* database and restricted RAG roles. */
@EnabledIfEnvironmentVariable(named = "T12_PGVECTOR_IT", matches = "true")
class PostgresPgVectorStoreIT {
    private static final LocalDate DATE = LocalDate.of(2026, 6, 1);
    private final HybridRagEmbeddingService embedding = new HybridRagEmbeddingService();
    private PostgresPgVectorStore store;
    private JdbcTemplate jdbc;
    private String prefix;
    @TempDir Path temporary;

    @BeforeEach void prepare() {
        String url = System.getenv("T12_RAG_JDBC_URL");
        assertThat(url).matches("jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):15434/t12_pgvector_[a-z0-9_]+");
        DriverManagerDataSource ds = dataSource(false);
        jdbc = new JdbcTemplate(ds);
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).startsWith("t12_pgvector_");
        store = new PostgresPgVectorStore(ds, embedding);
        prefix = "IT-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
    }

    @AfterEach void cleanupOwnSyntheticSources() {
        if (prefix == null) return;
        assertThat(prefix).matches("IT-[0-9A-F-]{36}");
        jdbc.update("DELETE FROM rag_document_versions WHERE source_key IN (SELECT id FROM rag_sources WHERE source_id LIKE ?)", prefix + "-%");
        jdbc.update("DELETE FROM rag_sources WHERE source_id LIKE ?", prefix + "-%");
    }

    @Test void stagesAtomicallyAndSurvivesNewStoreAndNewJvm() throws Exception {
        String source = source("PERSIST");
        stage(source, RagScopeType.CHAIN_PUBLIC, null, 1, null, null);
        assertThat(store.findCandidateChunks(null, null, DATE)).isEmpty();
        assertThat(store.activateVersion(RagScopeType.CHAIN_PUBLIC, null, source, 1)).isTrue();
        PostgresPgVectorStore restarted = new PostgresPgVectorStore(dataSource(true), embedding);
        assertThat(restarted.getActiveDocument(RagScopeType.CHAIN_PUBLIC, null, source)).isPresent();
        List<KnowledgeChunk> chunks = restarted.findCandidateChunks(null, null, DATE);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().getEmbedding()).containsExactly(embedding.embedText("Synthetic pool\nPublic swimming pool policy."));
        assertThat(jdbc.queryForObject("SELECT vector_dims(embedding) FROM rag_chunks WHERE id=?", Integer.class, chunks.getFirst().getChunkId()))
                .isEqualTo(embedding.getDimension());
        assertThat(jdbc.queryForObject("SELECT embedding <=> embedding FROM rag_chunks WHERE id=?", Double.class, chunks.getFirst().getChunkId()))
                .isCloseTo(0.0, within(0.00001));
        Path args = temporary.resolve("probe.args");
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Files.writeString(args, "-Xmx128m\n-cp\n\"" + classpath.replace("\\", "\\\\").replace("\"", "\\\"") + "\"\n"
                + Probe.class.getName() + "\nverify\n" + source + "\n");
        Path output = temporary.resolve("probe.log");
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "@" + args)
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertThat(child.waitFor(20, TimeUnit.SECONDS)).isTrue();
            assertThat(child.exitValue()).withFailMessage(Files.readString(output)).isZero();
            assertThat(Files.readString(output)).contains("SYNTHETIC_PERSISTENCE_OK");
        } finally {
            if (child.isAlive()) { child.destroyForcibly(); child.waitFor(5, TimeUnit.SECONDS); }
        }
    }

    @Test void filtersHotelAndEffectiveWindowsAndKeepsHistoricalPrecedence() {
        String source = source("POOL");
        stage(source, RagScopeType.CHAIN_PUBLIC, null, 1, null, null);
        stage(source, RagScopeType.HOTEL_SCOPED, 1, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31));
        stage(source, RagScopeType.HOTEL_SCOPED, 1, 2, DATE, null);
        stage(source, RagScopeType.HOTEL_SCOPED, 2, 1, null, null);
        store.activateVersion(RagScopeType.CHAIN_PUBLIC, null, source, 1);
        store.activateVersion(RagScopeType.HOTEL_SCOPED, 1, source, 1);
        store.activateVersion(RagScopeType.HOTEL_SCOPED, 1, source, 2);
        store.activateVersion(RagScopeType.HOTEL_SCOPED, 2, source, 1);
        assertThat(store.findCandidateChunks(null, 1, DATE)).allMatch(c -> c.getHotelId() == null || c.getHotelId() == 1);
        assertThat(store.findCandidateChunks(null, null, DATE)).allMatch(c -> c.getScopeType() == RagScopeType.CHAIN_PUBLIC);
        ScopedRagRetrievalService retrieval = new ScopedRagRetrievalService(store, embedding);
        var historical = retrieval.retrieve(RagQueryRequestDto.builder().hotelId(1).question("swimming pool policy")
                .asOfDate(LocalDate.of(2026, 4, 1)).topK(5).build());
        assertThat(historical).hasSize(1);
        assertThat(historical.getFirst().getChunk().getVersion()).isEqualTo(1);
        assertThat(historical.getFirst().getChunk().getHotelId()).isEqualTo(1);
        var current = retrieval.retrieve(RagQueryRequestDto.builder().hotelId(1).question("swimming pool policy").asOfDate(DATE).topK(5).build());
        assertThat(current).hasSize(1);
        assertThat(current.getFirst().getChunk().getVersion()).isEqualTo(2);
    }

    @Test void rollbackAndDeleteAreScopedAndActiveDeletionIsForbidden() {
        String source = source("ROLLBACK");
        stage(source, RagScopeType.HOTEL_SCOPED, 1, 1, null, null);
        store.activateVersion(RagScopeType.HOTEL_SCOPED, 1, source, 1);
        stage(source, RagScopeType.HOTEL_SCOPED, 1, 2, null, null);
        store.activateVersion(RagScopeType.HOTEL_SCOPED, 1, source, 2);
        assertThat(store.deleteVersion(RagScopeType.HOTEL_SCOPED, 2, source, 2)).isFalse();
        assertThatThrownBy(() -> store.deleteVersion(RagScopeType.HOTEL_SCOPED, 1, source, 2)).isInstanceOf(IllegalStateException.class);
        assertThat(store.rollbackVersion(RagScopeType.HOTEL_SCOPED, 1, source, null)).isTrue();
        assertThat(store.getActiveDocument(RagScopeType.HOTEL_SCOPED, 1, source).orElseThrow().getVersion()).isEqualTo(1);
        assertThat(store.getDocument(RagScopeType.HOTEL_SCOPED, 1, source, 2).orElseThrow().getStatus()).isEqualTo(RagVersionStatus.ROLLED_BACK);
        assertThat(store.deleteVersion(RagScopeType.HOTEL_SCOPED, 1, source, 2)).isTrue();
        assertThat(store.getDocument(RagScopeType.HOTEL_SCOPED, 1, source, 2)).isEmpty();
        assertThatThrownBy(store::clearAll).isInstanceOf(UnsupportedOperationException.class);
        assertThat(store.getActiveDocument(RagScopeType.HOTEL_SCOPED, 1, source)).isPresent();
    }

    @Test void metadataAndVectorMismatchRejectBeforeWriting() {
        Pair pair = pair(source("BAD"), 1);
        long before = jdbc.queryForObject("SELECT count(*) FROM rag_document_versions", Long.class);
        String validHash = pair.doc().getContentHash();
        pair.doc().setContentHash("a".repeat(64));
        assertThat(store.stageDocument(pair.doc(), List.of(pair.chunk())).getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        pair.doc().setContentHash(validHash);
        pair.chunk().setEmbedding(new float[128]);
        assertThat(store.stageDocument(pair.doc(), List.of(pair.chunk())).getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        pair.chunk().setEmbedding(embedding.embedText("Public swimming pool policy."));
        pair.chunk().getEmbedding()[0] = Float.NaN;
        assertThat(store.stageDocument(pair.doc(), List.of(pair.chunk())).getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        pair.chunk().setEmbedding(embedding.embedText("Public swimming pool policy.")); pair.chunk().setHotelId(2);
        assertThat(store.stageDocument(pair.doc(), List.of(pair.chunk())).getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rag_document_versions", Long.class)).isEqualTo(before);
        assertThatThrownBy(() -> new PostgresPgVectorStore(dataSource(false), new HybridRagEmbeddingService(128, embedding.getModelId())))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PostgresPgVectorStore(dataSource(false), new HybridRagEmbeddingService(256, "other-model")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void aDatabaseFailureCannotLeaveAPartialReplacement() {
        String source = source("ATOMIC"); Pair first = pair(source, 1);
        assertThat(store.stageDocument(first.doc(), List.of(first.chunk())).getStatus()).isEqualTo(RagVersionStatus.STAGED);
        store.activateVersion(RagScopeType.HOTEL_SCOPED, 1, source, 1);
        Pair second = pair(source, 2);
        second.chunk().setChunkId(first.chunk().getChunkId());
        assertThatThrownBy(() -> store.stageDocument(second.doc(), List.of(second.chunk()))).isInstanceOf(DataAccessException.class);
        assertThat(store.getDocument(RagScopeType.HOTEL_SCOPED, 1, source, 2)).isEmpty();
        assertThat(store.getActiveDocument(RagScopeType.HOTEL_SCOPED, 1, source).orElseThrow().getDocumentId()).isEqualTo(first.doc().getDocumentId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rag_chunks WHERE document_id=?", Integer.class, second.doc().getDocumentId())).isZero();
    }

    @Test void concurrentDuplicateStageAndActivationRemainConsistent() throws Exception {
        String source = source("CONCURRENT");
        try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
            Pair a = pair(source, 1), b = pair(source, 1);
            CountDownLatch go = new CountDownLatch(1);
            var f1 = workers.submit(() -> { go.await(); return store.stageDocument(a.doc(), List.of(a.chunk())).getStatus(); });
            var f2 = workers.submit(() -> { go.await(); return new PostgresPgVectorStore(dataSource(false), embedding).stageDocument(b.doc(), List.of(b.chunk())).getStatus(); });
            go.countDown();
            assertThat(List.of(f1.get(10, TimeUnit.SECONDS), f2.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(RagVersionStatus.STAGED, RagVersionStatus.REJECTED);
            stage(source, RagScopeType.HOTEL_SCOPED, 1, 2, null, null);
            var v1 = workers.submit(() -> store.activateVersion(RagScopeType.HOTEL_SCOPED, 1, source, 1));
            var v2 = workers.submit(() -> new PostgresPgVectorStore(dataSource(false), embedding).activateVersion(RagScopeType.HOTEL_SCOPED, 1, source, 2));
            assertThat(v1.get(10, TimeUnit.SECONDS)).isTrue(); assertThat(v2.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM rag_document_versions d JOIN rag_sources s ON s.id=d.source_key WHERE s.source_id=? AND d.status='ACTIVE'", Integer.class, source)).isEqualTo(1);
        }
    }

    @Test void restrictedRolesCannotReadBusinessTablesOrPerformDdl() {
        DataAccessException business = catchThrowableOfType(() -> jdbc.queryForObject("SELECT count(*) FROM bookings", Long.class), DataAccessException.class);
        assertThat(business).isNotNull();
        assertThat(((java.sql.SQLException) business.getRootCause()).getSQLState()).isEqualTo("42501");
        assertThatThrownBy(() -> jdbc.execute("CREATE TABLE public.rag_unauthorized_probe(id int)")).isInstanceOf(DataAccessException.class);
        DataAccessException temporary = catchThrowableOfType(() -> jdbc.execute("CREATE TEMP TABLE rag_unauthorized_temp(id int)"), DataAccessException.class);
        assertThat(temporary).isNotNull();
        assertThat(((java.sql.SQLException) temporary.getRootCause()).getSQLState()).isEqualTo("42501");
        PostgresPgVectorStore reader = new PostgresPgVectorStore(dataSource(true), embedding);
        Pair pair = pair(source("READER"), 1);
        assertThatThrownBy(() -> reader.stageDocument(pair.doc(), List.of(pair.chunk()))).isInstanceOf(DataAccessException.class);
    }

    @Test void databaseConstraintsRejectNullHotelAndWrongVectorDimension() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO rag_sources(scope_type,hotel_id,source_id) VALUES('HOTEL_SCOPED',NULL,?)", source("NULL")))
                .isInstanceOf(DataAccessException.class);
        Pair pair = pair(source("VECTOR"), 1); store.stageDocument(pair.doc(), List.of(pair.chunk()));
        assertThatThrownBy(() -> jdbc.update("UPDATE rag_chunks SET embedding='[1,2]'::vector WHERE id=?", pair.chunk().getChunkId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test void sourceNormalizationAndDuplicateVersionKeepExistingCorpus() {
        String source = source("NORMALIZE");
        stage(source.toLowerCase(Locale.ROOT), RagScopeType.HOTEL_SCOPED, 1, 1, null, null);
        UUID original = store.getDocument(RagScopeType.HOTEL_SCOPED, 1, source, 1).orElseThrow().getDocumentId();
        Pair duplicate = pair(source, 1);
        var rejected = store.stageDocument(duplicate.doc(), List.of(duplicate.chunk()));
        assertThat(rejected.getStatus()).isEqualTo(RagVersionStatus.REJECTED);
        assertThat(rejected.getSourceId()).isEqualTo(source);
        assertThat(rejected.getVersion()).isEqualTo(1);
        assertThat(store.getDocument(RagScopeType.HOTEL_SCOPED, 1, " " + source.toLowerCase(Locale.ROOT) + " ", 1).orElseThrow().getDocumentId()).isEqualTo(original);
    }

    @Test void excessiveCandidateSetFailsInsteadOfReturningAnIncompleteSearch() {
        RagKnowledgeLifecycleService lifecycle = new RagKnowledgeLifecycleService(new RagCorpusGovernanceValidator(), embedding, store);
        for (int i = 0; i < 5; i++) {
            String source = source("CAP-" + i);
            var result = lifecycle.ingestAndStage(RagIngestRequestDto.builder().sourceId(source).scopeType(RagScopeType.HOTEL_SCOPED)
                    .hotelId(1).category(RagDocumentCategory.FAQ).title("Synthetic pool")
                    .content("Public swimming pool policy.\n\n".repeat(i == 4 ? 1 : 250)).build(), 1);
            assertThat(result.getStatus()).isEqualTo(RagVersionStatus.STAGED);
            assertThat(store.activateVersion(RagScopeType.HOTEL_SCOPED, 1, source, 1)).isTrue();
        }
        assertThatThrownBy(() -> store.findCandidateChunks(null, 1, DATE)).isInstanceOf(IllegalStateException.class).hasMessageContaining("candidate bound");
    }

    @Test void springUsesDedicatedRagCredentialsWithoutTouchingPrimaryDatasource() {
        javax.sql.DataSource business = mock(javax.sql.DataSource.class);
        new ApplicationContextRunner().withUserConfiguration(InMemoryScopedVectorStore.class, RagVectorStoreConfiguration.class,
                        RagKnowledgeLifecycleService.class, RagCorpusGovernanceValidator.class)
                .withBean(RagEmbeddingPort.class, () -> embedding).withBean(javax.sql.DataSource.class, () -> business)
                .withPropertyValues("hotel.ai.rag.store-type=pgvector", "hotel.ai.rag.demo-corpus.enabled=false",
                        "hotel.ai.rag.pgvector.jdbc-url=" + System.getenv("T12_RAG_JDBC_URL"),
                        "hotel.ai.rag.pgvector.username=" + System.getenv("T12_RAG_WRITER_USER"),
                        "hotel.ai.rag.pgvector.password=" + System.getenv("T12_RAG_WRITER_PASSWORD"))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(RagKnowledgeLifecycleService.class).getActiveDocument(RagScopeType.CHAIN_PUBLIC, null, source("ABSENT"))).isEmpty();
                    assertThat(ctx).hasSingleBean(javax.sql.DataSource.class);
                    assertThat(ctx.getBean(javax.sql.DataSource.class)).isSameAs(business);
                    verifyNoInteractions(business);
                });
    }

    private String source(String suffix) { return prefix + "-" + suffix; }
    private void stage(String source, RagScopeType scope, Integer hotel, int version, LocalDate from, LocalDate to) {
        RagKnowledgeLifecycleService lifecycle = new RagKnowledgeLifecycleService(new RagCorpusGovernanceValidator(), embedding, store);
        var result = lifecycle.ingestAndStage(RagIngestRequestDto.builder().sourceId(source).scopeType(scope).hotelId(hotel)
                .category(RagDocumentCategory.FAQ).title("Synthetic pool").content("Public swimming pool policy.")
                .effectiveFrom(from).effectiveTo(to).build(), version);
        assertThat(result.getStatus()).isEqualTo(RagVersionStatus.STAGED);
    }
    private Pair pair(String source, int version) {
        UUID id = UUID.randomUUID();
        KnowledgeDocumentVersion doc = KnowledgeDocumentVersion.builder().documentId(id).sourceId(source).version(version)
                .scopeType(RagScopeType.HOTEL_SCOPED).hotelId(1).category(RagDocumentCategory.FAQ).title("Synthetic pool")
                .rawContent("Public swimming pool policy.").contentHash("abd47ad19f75c8ecde2da3201804919f926ceb66c0f78e4678e53036d8f1a176").embeddingModelId(embedding.getModelId())
                .embeddingDimension(embedding.getDimension()).chunkingVersion("paragraph-v1").createdAt(Instant.now()).build();
        KnowledgeChunk chunk = KnowledgeChunk.builder().chunkId(UUID.randomUUID()).documentId(id).sourceId(source).version(version)
                .scopeType(RagScopeType.HOTEL_SCOPED).hotelId(1).category(RagDocumentCategory.FAQ).content("Public swimming pool policy.")
                .sanitizedContent("Public swimming pool policy.").embedding(embedding.embedText("Public swimming pool policy.")).build();
        return new Pair(doc, chunk);
    }
    private record Pair(KnowledgeDocumentVersion doc, KnowledgeChunk chunk) { }
    static DriverManagerDataSource dataSource(boolean reader) {
        String url = System.getenv("T12_RAG_JDBC_URL");
        if (url == null || !url.matches("jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):15434/t12_pgvector_[a-z0-9_]+")) {
            throw new IllegalStateException("Synthetic RAG probe requires its dedicated loopback database");
        }
        return new DriverManagerDataSource(url, System.getenv(reader ? "T12_RAG_READER_USER" : "T12_RAG_WRITER_USER"),
                System.getenv(reader ? "T12_RAG_READER_PASSWORD" : "T12_RAG_WRITER_PASSWORD"));
    }

    /** Separate-JVM restart probe, also used by the sandbox container-restart protocol. */
    public static class Probe {
        public static void main(String[] args) {
            HybridRagEmbeddingService embedding = new HybridRagEmbeddingService();
            PostgresPgVectorStore store = new PostgresPgVectorStore(dataSource(!args[0].equals("seed")), embedding);
            String source = args[1];
            if (args[0].equals("seed")) {
                var result = new RagKnowledgeLifecycleService(new RagCorpusGovernanceValidator(), embedding, store).ingestAndStage(
                        RagIngestRequestDto.builder().sourceId(source).scopeType(RagScopeType.CHAIN_PUBLIC).category(RagDocumentCategory.FAQ)
                                .title("Synthetic pool").content("Public swimming pool policy.").build(), 1);
                if (result.getStatus() != RagVersionStatus.STAGED || !store.activateVersion(RagScopeType.CHAIN_PUBLIC, null, source, 1)) throw new AssertionError("Synthetic probe seed failed");
            }
            if (store.getActiveDocument(RagScopeType.CHAIN_PUBLIC, null, source).isEmpty()
                    || store.findCandidateChunks(null, null, DATE).stream().noneMatch(c -> c.getSourceId().equals(source))) {
                throw new AssertionError("Synthetic corpus did not persist");
            }
            System.out.println("SYNTHETIC_PERSISTENCE_OK");
        }
    }
}
