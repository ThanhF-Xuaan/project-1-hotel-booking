package vn.edu.utc.hotel_booking.aiassistant.knowledge;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService.StaffActor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Repository
public class KnowledgeRepository {
    private final JdbcTemplate jdbc;

    public KnowledgeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(UUID documentId, UUID versionId, UUID jobId, String title,
                       String content, String checksum, String visibility, Integer hotelId) {
        jdbc.update("INSERT INTO ai_documents(id,title,visibility,hotel_id) VALUES (?,?,?,?)",
                documentId, title, visibility, hotelId);
        jdbc.update("""
                INSERT INTO ai_document_versions(id,document_id,version_number,content,checksum)
                VALUES (?,?,1,?,?)
                """, versionId, documentId, content, checksum);
        jdbc.update("INSERT INTO ai_ingest_jobs(id,version_id) VALUES (?,?)", jobId, versionId);
    }

    public List<DocumentMeta> document(UUID id) {
        return jdbc.query("""
                SELECT d.id, d.title, d.visibility, d.hotel_id, d.status, h.region_id
                FROM ai_documents d LEFT JOIN hotels h ON h.id = d.hotel_id WHERE d.id = ?
                """, (rs, row) -> new DocumentMeta((UUID) rs.getObject("id"), rs.getString("title"),
                rs.getString("visibility"), (Integer) rs.getObject("hotel_id"),
                rs.getString("status"), (Integer) rs.getObject("region_id")), id);
    }

    public List<JobStatus> job(UUID id) {
        return jdbc.query("""
                SELECT j.id, j.status, j.attempts, j.last_error, v.document_id
                FROM ai_ingest_jobs j JOIN ai_document_versions v ON v.id = j.version_id
                WHERE j.id = ?
                """, (rs, row) -> new JobStatus((UUID) rs.getObject("id"),
                rs.getString("status"), rs.getInt("attempts"), rs.getString("last_error"),
                (UUID) rs.getObject("document_id")), id);
    }

    public List<IngestJob> claimJob() {
        jdbc.update("""
                WITH exhausted AS (
                    UPDATE ai_ingest_jobs SET status = 'FAILED', last_error = 'Worker lease expired',
                        updated_at = now()
                    WHERE status = 'RUNNING' AND attempts >= 3
                        AND claimed_at < now() - interval '5 minutes'
                    RETURNING version_id
                )
                UPDATE ai_document_versions SET status = 'FAILED'
                WHERE id IN (SELECT version_id FROM exhausted)
                """);
        return jdbc.query("""
                UPDATE ai_ingest_jobs SET status = 'RUNNING', attempts = attempts + 1,
                    claimed_at = now(), updated_at = now()
                WHERE id = (
                    SELECT id FROM ai_ingest_jobs
                    WHERE attempts < 3 AND (status = 'QUEUED'
                        OR (status = 'RUNNING' AND claimed_at < now() - interval '5 minutes'))
                    ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1
                )
                RETURNING id, version_id, attempts
                """, (rs, row) -> new IngestJob((UUID) rs.getObject("id"),
                (UUID) rs.getObject("version_id"), rs.getInt("attempts")));
    }

    public String versionContent(UUID versionId) {
        return jdbc.queryForObject("SELECT content FROM ai_document_versions WHERE id = ?", String.class, versionId);
    }

    public void finish(IngestJob job, List<EmbeddedChunk> chunks) {
        int owned = jdbc.update("""
                UPDATE ai_ingest_jobs SET updated_at = now()
                WHERE id = ? AND attempts = ? AND status = 'RUNNING'
                """, job.id(), job.attempts());
        if (owned != 1) throw new IllegalStateException("Ingestion job ownership changed");
        jdbc.update("DELETE FROM ai_chunks WHERE version_id = ?", job.versionId());
        for (int index = 0; index < chunks.size(); index++) {
            UUID chunkId = UUID.nameUUIDFromBytes((job.versionId() + ":" + index).getBytes(StandardCharsets.UTF_8));
            EmbeddedChunk chunk = chunks.get(index);
            jdbc.update("""
                    INSERT INTO ai_chunks(id,version_id,chunk_index,content,embedding)
                    VALUES (?,?,?,?,?::vector)
                    """, chunkId, job.versionId(), index, chunk.content(), chunk.vectorLiteral());
        }
        jdbc.update("UPDATE ai_document_versions SET status = 'READY' WHERE id = ?", job.versionId());
        jdbc.update("UPDATE ai_ingest_jobs SET status = 'SUCCEEDED', last_error = NULL WHERE id = ?", job.id());
    }

    public void fail(IngestJob job, String safeError) {
        int owned = jdbc.update("""
                UPDATE ai_ingest_jobs SET status = ?, last_error = ?
                WHERE id = ? AND attempts = ? AND status = 'RUNNING'
                """, job.attempts() >= 3 ? "FAILED" : "QUEUED", safeError, job.id(), job.attempts());
        if (owned == 1 && job.attempts() >= 3) {
            jdbc.update("UPDATE ai_document_versions SET status = 'FAILED' WHERE id = ?", job.versionId());
        }
    }

    public int publish(UUID documentId) {
        var ready = jdbc.query("""
                SELECT id FROM ai_document_versions WHERE document_id = ? AND status = 'READY'
                ORDER BY version_number DESC LIMIT 1 FOR UPDATE
                """, (rs, row) -> (UUID) rs.getObject(1), documentId);
        if (ready.isEmpty()) return 0;
        jdbc.update("UPDATE ai_document_versions SET status = 'SUPERSEDED' WHERE document_id = ? AND status = 'PUBLISHED'",
                documentId);
        jdbc.update("UPDATE ai_documents SET status = 'ACTIVE' WHERE id = ?", documentId);
        return jdbc.update("UPDATE ai_document_versions SET status = 'PUBLISHED', published_at = now() WHERE id = ?",
                ready.getFirst());
    }

    public int revoke(UUID documentId) {
        return jdbc.update("UPDATE ai_documents SET status = 'REVOKED' WHERE id = ?", documentId);
    }

    public List<SourceChunk> search(String vector, StaffActor actor, Integer selectedHotelId, int limit) {
        String scope = switch (actor.scopeType()) {
            case "CHAIN" -> "";
            case "REGION" -> " AND (d.hotel_id IS NULL OR h.region_id = ?)";
            case "PROPERTY" -> " AND (d.hotel_id IS NULL OR d.hotel_id = ?)";
            default -> throw new IllegalArgumentException("Unknown scope");
        };
        String selectedHotel = selectedHotelId == null ? "" : " AND (d.hotel_id IS NULL OR d.hotel_id = ?)";
        String sql = """
                SELECT c.id, c.content, d.id AS document_id, d.title, d.hotel_id,
                       h.region_id, 1 - (c.embedding <=> ?::vector) AS similarity
                FROM ai_chunks c JOIN ai_document_versions v ON v.id = c.version_id
                JOIN ai_documents d ON d.id = v.document_id
                LEFT JOIN hotels h ON h.id = d.hotel_id
                WHERE d.status = 'ACTIVE' AND v.status = 'PUBLISHED'
                  AND d.visibility IN ('PUBLIC', 'STAFF')
                """ + scope + selectedHotel + " AND 1 - (c.embedding <=> ?::vector) >= 0.55"
                + " ORDER BY c.embedding <=> ?::vector LIMIT ?";
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        args.add(vector);
        if (!actor.scopeType().equals("CHAIN")) args.add(actor.scopeEntityId());
        if (selectedHotelId != null) args.add(selectedHotelId);
        args.add(vector);
        args.add(vector);
        args.add(limit);
        return jdbc.query(sql, (rs, row) -> new SourceChunk((UUID) rs.getObject("id"),
                (UUID) rs.getObject("document_id"), rs.getString("title"), rs.getString("content"),
                (Integer) rs.getObject("hotel_id"), (Integer) rs.getObject("region_id"),
                rs.getDouble("similarity")), args.toArray());
    }

    public List<SourceChunk> source(UUID chunkId) {
        return jdbc.query("""
                SELECT c.id, c.content, d.id AS document_id, d.title, d.hotel_id,
                       h.region_id, 1.0 AS similarity
                FROM ai_chunks c JOIN ai_document_versions v ON v.id = c.version_id
                JOIN ai_documents d ON d.id = v.document_id
                LEFT JOIN hotels h ON h.id = d.hotel_id
                WHERE c.id = ? AND d.status = 'ACTIVE' AND v.status = 'PUBLISHED'
                """, (rs, row) -> new SourceChunk((UUID) rs.getObject("id"),
                (UUID) rs.getObject("document_id"), rs.getString("title"), rs.getString("content"),
                (Integer) rs.getObject("hotel_id"), (Integer) rs.getObject("region_id"),
                rs.getDouble("similarity")), chunkId);
    }

    public record DocumentMeta(UUID id, String title, String visibility, Integer hotelId,
                               String status, Integer regionId) {}
    public record JobStatus(UUID id, String status, int attempts, String lastError, UUID documentId) {}
    public record IngestJob(UUID id, UUID versionId, int attempts) {}
    public record EmbeddedChunk(String content, String vectorLiteral) {}
    public record SourceChunk(UUID id, UUID documentId, String title, String content,
                              Integer hotelId, Integer regionId, double similarity) {}
}
