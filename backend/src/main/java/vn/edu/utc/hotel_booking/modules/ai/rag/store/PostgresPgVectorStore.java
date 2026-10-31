package vn.edu.utc.hotel_booking.modules.ai.rag.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeDocumentVersion;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Only RAG-owned tables are reachable through constant, parameterized SQL. */
public class PostgresPgVectorStore implements RagVectorStorePort {
    private static final String DOCUMENT_SELECT = """
            SELECT d.*, s.scope_type, s.hotel_id, s.source_id
            FROM rag_document_versions d JOIN rag_sources s ON s.id=d.source_key
            """;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final int dimension;
    private final String model;

    public PostgresPgVectorStore(DataSource dataSource, RagEmbeddingPort embedding) {
        dimension = embedding.getDimension();
        model = embedding.getModelId();
        if (dimension < 1 || dimension > 16000 || model == null || model.isBlank() || model.length() > 128) {
            throw new IllegalStateException("Invalid RAG embedding identity");
        }
        jdbc = new JdbcTemplate(dataSource);
        jdbc.setQueryTimeout(5);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.setTimeout(5);
        if (jdbc.queryForObject("SELECT count(*) FROM rag_store_config WHERE id=1", Integer.class) == 0) {
            jdbc.update("INSERT INTO rag_store_config(id,embedding_model_id,embedding_dimension) VALUES(1,?,?) ON CONFLICT(id) DO NOTHING", model, dimension);
        }
        Map<String, Object> configured = jdbc.queryForMap("SELECT embedding_model_id,embedding_dimension FROM rag_store_config WHERE id=1");
        if (!model.equals(configured.get("embedding_model_id")) || dimension != ((Number) configured.get("embedding_dimension")).intValue()) {
            throw new IllegalStateException("RAG embedding model/dimension does not match persisted corpus");
        }
    }

    @Override public int getExpectedDimension() { return dimension; }

    @Override
    public RagIngestResultDto stageDocument(KnowledgeDocumentVersion doc, List<KnowledgeChunk> chunks) {
        Key key;
        List<String> payloads = new ArrayList<>();
        String documentPayload;
        try {
            if (doc == null || doc.getDocumentId() == null || doc.getCategory() == null || doc.getVersion() < 1
                    || doc.getContentHash() == null || !doc.getContentHash().matches("[0-9a-f]{64}")
                    || doc.getRawContent() == null || doc.getRawContent().length() > 64_000
                    || !sha256(doc.getRawContent()).equals(doc.getContentHash())
                    || !"paragraph-v1".equals(doc.getChunkingVersion()) || !model.equals(doc.getEmbeddingModelId())
                    || doc.getEmbeddingDimension() != dimension || chunks == null || chunks.isEmpty() || chunks.size() > 256
                    || (doc.getEffectiveFrom() != null && doc.getEffectiveTo() != null && doc.getEffectiveTo().isBefore(doc.getEffectiveFrom()))) {
                return rejected("REJECTED_INVALID_DOCUMENT_OR_EMBEDDING");
            }
            key = key(doc.getScopeType(), doc.getHotelId(), doc.getSourceId());
            documentPayload = payload(doc, 262144);
            Set<UUID> ids = new HashSet<>();
            Set<Integer> positions = new HashSet<>();
            for (KnowledgeChunk chunk : chunks) {
                if (chunk == null || chunk.getChunkId() == null || !ids.add(chunk.getChunkId()) || !positions.add(chunk.getChunkIndex())
                        || chunk.getChunkIndex() < 0 || !doc.getDocumentId().equals(chunk.getDocumentId())
                        || chunk.getVersion() != doc.getVersion() || chunk.getCategory() != doc.getCategory()
                        || !key.equals(key(chunk.getScopeType(), chunk.getHotelId(), chunk.getSourceId()))
                        || !Objects.equals(doc.getEffectiveFrom(), chunk.getEffectiveFrom())
                        || !Objects.equals(doc.getEffectiveTo(), chunk.getEffectiveTo()) || !validVector(chunk.getEmbedding())) {
                    return rejected("REJECTED_CHUNK_METADATA_OR_VECTOR");
                }
                ObjectNode node = mapper.valueToTree(chunk);
                node.remove("embedding");
                payloads.add(payload(node, 32768));
            }
        } catch (IllegalArgumentException e) {
            return rejected("REJECTED_INVALID_SCOPE_OR_PAYLOAD");
        }
        return transaction.execute(status -> {
            jdbc.update("INSERT INTO rag_sources(scope_type,hotel_id,source_id) VALUES(?,?,?) ON CONFLICT(scope_type,hotel_key,source_id) DO NOTHING",
                    key.scope().name(), key.hotel(), key.source());
            Long source = lockSource(key);
            if (jdbc.queryForObject("SELECT count(*) FROM rag_document_versions WHERE source_key=? AND version=?", Integer.class, source, doc.getVersion()) != 0) {
                return RagIngestResultDto.builder().sourceId(key.source()).version(doc.getVersion())
                        .status(RagVersionStatus.REJECTED).errorMessage("REJECTED_DUPLICATE_VERSION").build();
            }
            jdbc.update("""
                    INSERT INTO rag_document_versions(id,source_key,version,status,embedding_model_id,embedding_dimension,content_hash,effective_from,effective_to,payload)
                    VALUES(?,?,?,'STAGED',?,?,?,?,?,?::jsonb)
                    """, doc.getDocumentId(), source, doc.getVersion(), model, dimension, doc.getContentHash(),
                    doc.getEffectiveFrom(), doc.getEffectiveTo(), documentPayload);
            for (int i = 0; i < chunks.size(); i++) {
                KnowledgeChunk chunk = chunks.get(i);
                jdbc.update("INSERT INTO rag_chunks(id,document_id,chunk_index,embedding_dimension,embedding,payload) VALUES(?,?,?,?,?::vector,?::jsonb)",
                        chunk.getChunkId(), doc.getDocumentId(), chunk.getChunkIndex(), dimension, vectorLiteral(chunk.getEmbedding()), payloads.get(i));
            }
            return RagIngestResultDto.builder().documentId(doc.getDocumentId()).sourceId(key.source()).version(doc.getVersion())
                    .status(RagVersionStatus.STAGED).chunkCount(chunks.size()).contentHash(doc.getContentHash()).build();
        });
    }

    @Override public boolean activateVersion(RagScopeType scope, Integer hotel, String source, int version) {
        return transition(key(scope, hotel, source), version, false);
    }

    @Override public boolean rollbackVersion(RagScopeType scope, Integer hotel, String source, Integer targetVersion) {
        return transition(key(scope, hotel, source), targetVersion, true);
    }

    private boolean transition(Key key, Integer targetVersion, boolean rollback) {
        return Boolean.TRUE.equals(transaction.execute(status -> {
            Long source = lockSource(key);
            if (source == null) return false;
            String required = rollback ? "SUPERSEDED" : "STAGED";
            List<Integer> candidates = jdbc.queryForList("SELECT version FROM rag_document_versions WHERE source_key=? AND status=? ORDER BY version DESC",
                    Integer.class, source, required);
            Integer target = targetVersion == null && rollback ? candidates.stream().findFirst().orElse(null) : targetVersion;
            if (target == null || !candidates.contains(target)) return false;
            jdbc.update("UPDATE rag_document_versions SET status=? WHERE source_key=? AND status='ACTIVE'",
                    rollback ? "ROLLED_BACK" : "SUPERSEDED", source);
            jdbc.update("UPDATE rag_document_versions SET status='ACTIVE',activated_at=now() WHERE source_key=? AND version=?", source, target);
            return true;
        }));
    }

    @Override public boolean deleteVersion(RagScopeType scope, Integer hotel, String sourceId, int version) {
        Key key = key(scope, hotel, sourceId);
        return Boolean.TRUE.equals(transaction.execute(status -> {
            Long source = lockSource(key);
            if (source == null) return false;
            List<String> states = jdbc.queryForList("SELECT status FROM rag_document_versions WHERE source_key=? AND version=?", String.class, source, version);
            if (states.isEmpty()) return false;
            if (states.getFirst().equals("ACTIVE")) throw new IllegalStateException("Cannot delete active corpus version");
            jdbc.update("DELETE FROM rag_document_versions WHERE source_key=? AND version=?", source, version);
            jdbc.update("DELETE FROM rag_sources WHERE id=? AND NOT EXISTS(SELECT 1 FROM rag_document_versions WHERE source_key=?)", source, source);
            return true;
        }));
    }

    @Override public List<KnowledgeChunk> findCandidateChunks(RagScopeType scope, Integer hotel, LocalDate asOf) {
        if (hotel != null && (hotel < 1 || hotel > 32767)) throw new IllegalArgumentException("Invalid RAG hotel scope");
        if (scope == RagScopeType.HOTEL_SCOPED && hotel == null) throw new IllegalArgumentException("Hotel scope required");
        LocalDate date = asOf == null ? LocalDate.now() : asOf;
        String scopeFilter;
        List<Object> args = new ArrayList<>();
        if (scope == RagScopeType.CHAIN_PUBLIC || hotel == null) {
            scopeFilter = "s.scope_type='CHAIN_PUBLIC' AND s.hotel_id IS NULL";
        } else if (scope == RagScopeType.HOTEL_SCOPED) {
            scopeFilter = "s.scope_type='HOTEL_SCOPED' AND s.hotel_id=?"; args.add(hotel);
        } else {
            scopeFilter = "((s.scope_type='HOTEL_SCOPED' AND s.hotel_id=?) OR (s.scope_type='CHAIN_PUBLIC' AND s.hotel_id IS NULL))"; args.add(hotel);
        }
        args.add(date); args.add(date);
        List<KnowledgeChunk> rows = jdbc.query("""
                SELECT c.payload::text,c.embedding::text,c.chunk_index,c.document_id,d.status,d.version,d.embedding_model_id,d.embedding_dimension,
                       s.scope_type,s.hotel_id,s.source_id FROM rag_chunks c
                JOIN rag_document_versions d ON d.id=c.document_id JOIN rag_sources s ON s.id=d.source_key
                WHERE
                """ + scopeFilter + " AND d.status IN ('ACTIVE','SUPERSEDED') AND (d.effective_from IS NULL OR d.effective_from<=?)"
                + " AND (d.effective_to IS NULL OR d.effective_to>=?) ORDER BY s.scope_type,s.hotel_key,s.source_id,d.version,c.chunk_index LIMIT 1001",
                (rs, index) -> readChunk(rs), args.toArray());
        if (rows.size() > 1000) throw new IllegalStateException("RAG candidate bound exceeded");
        return rows;
    }

    @Override public Optional<KnowledgeDocumentVersion> getDocument(RagScopeType scope, Integer hotel, String source, int version) {
        return document(key(scope, hotel, source), "d.version=?", version);
    }

    @Override public Optional<KnowledgeDocumentVersion> getActiveDocument(RagScopeType scope, Integer hotel, String source) {
        return document(key(scope, hotel, source), "d.status=?", "ACTIVE");
    }

    private Optional<KnowledgeDocumentVersion> document(Key key, String predicate, Object value) {
        return jdbc.query(DOCUMENT_SELECT + " WHERE s.scope_type=? AND s.hotel_key=? AND s.source_id=? AND " + predicate,
                (rs, index) -> readDocument(rs), key.scope().name(), key.hotelKey(), key.source(), value).stream().findFirst();
    }

    @Override public void clearAll() { throw new UnsupportedOperationException("Persistent RAG requires scoped lifecycle deletion"); }

    private Long lockSource(Key key) {
        return jdbc.queryForList("SELECT id FROM rag_sources WHERE scope_type=? AND hotel_key=? AND source_id=? FOR UPDATE",
                Long.class, key.scope().name(), key.hotelKey(), key.source()).stream().findFirst().orElse(null);
    }

    private KnowledgeDocumentVersion readDocument(ResultSet rs) throws SQLException {
        requireModel(rs);
        KnowledgeDocumentVersion doc = decode(rs.getString("payload"), KnowledgeDocumentVersion.class);
        doc.setDocumentId(rs.getObject("id", UUID.class));
        doc.setSourceId(rs.getString("source_id")); doc.setVersion(rs.getInt("version"));
        doc.setScopeType(RagScopeType.valueOf(rs.getString("scope_type"))); doc.setHotelId((Integer) rs.getObject("hotel_id", Integer.class));
        doc.setStatus(RagVersionStatus.valueOf(rs.getString("status")));
        doc.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        doc.setActivatedAt(rs.getTimestamp("activated_at") == null ? null : rs.getTimestamp("activated_at").toInstant());
        return doc;
    }

    private KnowledgeChunk readChunk(ResultSet rs) throws SQLException {
        requireModel(rs);
        KnowledgeChunk chunk = decode(rs.getString("payload"), KnowledgeChunk.class);
        chunk.setDocumentId(rs.getObject("document_id", UUID.class)); chunk.setChunkIndex(rs.getInt("chunk_index"));
        chunk.setSourceId(rs.getString("source_id")); chunk.setVersion(rs.getInt("version"));
        chunk.setScopeType(RagScopeType.valueOf(rs.getString("scope_type"))); chunk.setHotelId(rs.getObject("hotel_id", Integer.class));
        chunk.setStatus(RagVersionStatus.valueOf(rs.getString("status")));
        String[] values = rs.getString("embedding").replace("[", "").replace("]", "").split(",");
        float[] vector = new float[values.length];
        for (int i = 0; i < values.length; i++) vector[i] = Float.parseFloat(values[i]);
        if (!validVector(vector)) throw new IllegalStateException("Persisted RAG vector is incompatible");
        chunk.setEmbedding(vector);
        return chunk;
    }

    private void requireModel(ResultSet rs) throws SQLException {
        if (!model.equals(rs.getString("embedding_model_id")) || dimension != rs.getInt("embedding_dimension")) {
            throw new IllegalStateException("Persisted RAG model is incompatible");
        }
    }

    private <T> T decode(String text, Class<T> type) {
        try { return mapper.readValue(text, type); }
        catch (Exception e) { throw new IllegalStateException("Invalid persisted RAG payload"); }
    }

    private String payload(Object value, int limit) {
        try {
            String json = mapper.writeValueAsString(value);
            if (json.getBytes(StandardCharsets.UTF_8).length > limit) throw new IllegalArgumentException("RAG payload limit");
            return json;
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("Invalid RAG payload"); }
    }

    private boolean validVector(float[] vector) {
        if (vector == null || vector.length != dimension) return false;
        double norm = 0;
        for (float value : vector) { if (!Float.isFinite(value)) return false; norm += (double) value * value; }
        return Double.isFinite(norm) && norm > 0;
    }

    private String vectorLiteral(float[] vector) {
        StringJoiner values = new StringJoiner(",", "[", "]");
        for (float value : vector) values.add(Float.toString(value));
        return values.toString();
    }

    private RagIngestResultDto rejected(String message) {
        return RagIngestResultDto.builder().status(RagVersionStatus.REJECTED).errorMessage(message).build();
    }

    private String sha256(String content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 is required for corpus integrity"); }
    }

    private Key key(RagScopeType scope, Integer hotel, String source) {
        if (scope == null || (scope == RagScopeType.HOTEL_SCOPED && (hotel == null || hotel < 1 || hotel > 32767))
                || (scope == RagScopeType.CHAIN_PUBLIC && hotel != null)) throw new IllegalArgumentException("Invalid RAG scope");
        String normalized = source == null ? "" : source.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9][A-Z0-9._:-]{0,127}")) throw new IllegalArgumentException("Invalid RAG source");
        return new Key(scope, hotel, normalized);
    }

    private record Key(RagScopeType scope, Integer hotel, String source) {
        int hotelKey() { return hotel == null ? 0 : hotel; }
    }
}
