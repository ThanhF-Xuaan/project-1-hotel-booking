package vn.edu.utc.hotel_booking.modules.ai.rag.store;

import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeDocumentVersion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RagVectorStorePort {
    RagIngestResultDto stageDocument(KnowledgeDocumentVersion doc, List<KnowledgeChunk> chunks);

    boolean activateVersion(RagScopeType scopeType, Integer hotelId, String sourceId, int version);

    boolean rollbackVersion(RagScopeType scopeType, Integer hotelId, String sourceId, Integer targetVersion);

    boolean deleteVersion(RagScopeType scopeType, Integer hotelId, String sourceId, int version);

    List<KnowledgeChunk> findCandidateChunks(RagScopeType scopeType, Integer hotelId, LocalDate asOfDate);

    int getExpectedDimension();

    Optional<KnowledgeDocumentVersion> getDocument(RagScopeType scopeType, Integer hotelId, String sourceId, int version);

    Optional<KnowledgeDocumentVersion> getActiveDocument(RagScopeType scopeType, Integer hotelId, String sourceId);

    void clearAll();
}
