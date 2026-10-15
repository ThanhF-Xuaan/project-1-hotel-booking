package vn.edu.utc.hotel_booking.modules.ai.rag.store;

import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeDocumentVersion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class InMemoryScopedVectorStore implements RagVectorStorePort {

    private final int expectedDimension;

    // Key: scopeType + ":" + (hotelId != null ? hotelId : "chain") + ":" + sourceId
    private final Map<String, List<KnowledgeDocumentVersion>> documentStore = new ConcurrentHashMap<>();
    private final Map<String, List<KnowledgeChunk>> chunkStore = new ConcurrentHashMap<>();

    public InMemoryScopedVectorStore() {
        this(256);
    }

    public InMemoryScopedVectorStore(int expectedDimension) {
        this.expectedDimension = expectedDimension;
    }

    @Override
    public int getExpectedDimension() {
        return expectedDimension;
    }

    @Override
    public synchronized RagIngestResultDto stageDocument(KnowledgeDocumentVersion doc, List<KnowledgeChunk> chunks) {
        if (doc == null) {
            return RagIngestResultDto.builder()
                    .status(RagVersionStatus.REJECTED)
                    .errorMessage("REJECTED_NULL_DOCUMENT: Document cannot be null")
                    .build();
        }

        // 1. Verify embedding dimension compatibility across all chunks (T12-V2-041)
        if (chunks != null) {
            for (KnowledgeChunk chunk : chunks) {
                if (chunk.getEmbedding() == null || chunk.getEmbedding().length != expectedDimension) {
                    int actualDim = chunk.getEmbedding() != null ? chunk.getEmbedding().length : 0;
                    return RagIngestResultDto.builder()
                            .sourceId(doc.getSourceId())
                            .version(doc.getVersion())
                            .status(RagVersionStatus.REJECTED)
                            .errorMessage("REJECTED_EMBEDDING_DIMENSION_MISMATCH: Expected vector dimension "
                                    + expectedDimension + ", but received " + actualDim
                                    + ". Staging aborted safely without modifying existing active corpus.")
                            .build();
                }
            }
        }

        String key = buildKey(doc.getScopeType(), doc.getHotelId(), doc.getSourceId());
        List<KnowledgeDocumentVersion> docList = documentStore.computeIfAbsent(key, k -> new ArrayList<>());

        // Check if version already exists
        boolean versionExists = docList.stream().anyMatch(d -> d.getVersion() == doc.getVersion());
        if (versionExists) {
            return RagIngestResultDto.builder()
                    .sourceId(doc.getSourceId())
                    .version(doc.getVersion())
                    .status(RagVersionStatus.REJECTED)
                    .errorMessage("REJECTED_DUPLICATE_VERSION: Version " + doc.getVersion() + " already exists for " + key)
                    .build();
        }

        doc.setStatus(RagVersionStatus.STAGED);
        doc.setEmbeddingDimension(expectedDimension);
        docList.add(doc);

        if (chunks != null && !chunks.isEmpty()) {
            List<KnowledgeChunk> currentChunks = chunkStore.computeIfAbsent(key, k -> new ArrayList<>());
            for (KnowledgeChunk c : chunks) {
                c.setStatus(RagVersionStatus.STAGED);
                currentChunks.add(c);
            }
        }

        return RagIngestResultDto.builder()
                .documentId(doc.getDocumentId())
                .sourceId(doc.getSourceId())
                .version(doc.getVersion())
                .status(RagVersionStatus.STAGED)
                .chunkCount(chunks != null ? chunks.size() : 0)
                .contentHash(doc.getContentHash())
                .build();
    }

    @Override
    public synchronized boolean activateVersion(RagScopeType scopeType, Integer hotelId, String sourceId, int version) {
        String key = buildKey(scopeType, hotelId, sourceId);
        List<KnowledgeDocumentVersion> docList = documentStore.get(key);
        if (docList == null) {
            return false;
        }

        KnowledgeDocumentVersion targetDoc = docList.stream()
                .filter(d -> d.getVersion() == version && d.getStatus() == RagVersionStatus.STAGED)
                .findFirst()
                .orElse(null);

        if (targetDoc == null) {
            return false;
        }

        // Transition any currently ACTIVE version to SUPERSEDED
        for (KnowledgeDocumentVersion d : docList) {
            if (d.getStatus() == RagVersionStatus.ACTIVE) {
                d.setStatus(RagVersionStatus.SUPERSEDED);
            }
        }

        // Activate target doc
        targetDoc.setStatus(RagVersionStatus.ACTIVE);
        targetDoc.setActivatedAt(Instant.now());

        // Update corresponding chunks
        List<KnowledgeChunk> currentChunks = chunkStore.get(key);
        if (currentChunks != null) {
            for (KnowledgeChunk c : currentChunks) {
                if (c.getVersion() == version) {
                    c.setStatus(RagVersionStatus.ACTIVE);
                } else if (c.getStatus() == RagVersionStatus.ACTIVE) {
                    c.setStatus(RagVersionStatus.SUPERSEDED);
                }
            }
        }

        return true;
    }

    @Override
    public synchronized boolean rollbackVersion(RagScopeType scopeType, Integer hotelId, String sourceId, Integer targetVersion) {
        String key = buildKey(scopeType, hotelId, sourceId);
        List<KnowledgeDocumentVersion> docList = documentStore.get(key);
        if (docList == null || docList.isEmpty()) {
            return false;
        }

        KnowledgeDocumentVersion activeDoc = docList.stream()
                .filter(d -> d.getStatus() == RagVersionStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        KnowledgeDocumentVersion rollbackTarget;
        if (targetVersion != null) {
            rollbackTarget = docList.stream()
                    .filter(d -> d.getVersion() == targetVersion && d.getStatus() == RagVersionStatus.SUPERSEDED)
                    .findFirst()
                    .orElse(null);
        } else {
            // Find latest superseded version
            rollbackTarget = docList.stream()
                    .filter(d -> d.getStatus() == RagVersionStatus.SUPERSEDED)
                    .max(Comparator.comparingInt(KnowledgeDocumentVersion::getVersion))
                    .orElse(null);
        }

        if (rollbackTarget == null) {
            return false;
        }

        if (activeDoc != null) {
            activeDoc.setStatus(RagVersionStatus.ROLLED_BACK);
        }

        rollbackTarget.setStatus(RagVersionStatus.ACTIVE);
        rollbackTarget.setActivatedAt(Instant.now());

        // Update chunks
        List<KnowledgeChunk> currentChunks = chunkStore.get(key);
        if (currentChunks != null) {
            for (KnowledgeChunk c : currentChunks) {
                if (activeDoc != null && c.getVersion() == activeDoc.getVersion()) {
                    c.setStatus(RagVersionStatus.ROLLED_BACK);
                } else if (c.getVersion() == rollbackTarget.getVersion()) {
                    c.setStatus(RagVersionStatus.ACTIVE);
                }
            }
        }

        return true;
    }

    @Override
    public synchronized boolean deleteVersion(RagScopeType scopeType, Integer hotelId, String sourceId, int version) {
        String key = buildKey(scopeType, hotelId, sourceId);
        List<KnowledgeDocumentVersion> docList = documentStore.get(key);
        if (docList == null) {
            return false;
        }

        KnowledgeDocumentVersion target = docList.stream()
                .filter(d -> d.getVersion() == version)
                .findFirst()
                .orElse(null);

        if (target == null) {
            return false;
        }

        // Disallow deleting ACTIVE version directly without prior replacement or rollback
        if (target.getStatus() == RagVersionStatus.ACTIVE) {
            throw new IllegalStateException("Cannot delete active corpus version directly. Please activate a replacement version or roll back first.");
        }

        docList.remove(target);

        List<KnowledgeChunk> currentChunks = chunkStore.get(key);
        if (currentChunks != null) {
            currentChunks.removeIf(c -> c.getVersion() == version);
        }

        return true;
    }

    @Override
    public List<KnowledgeChunk> findCandidateChunks(RagScopeType scopeType, Integer hotelId, LocalDate asOfDate) {
        LocalDate queryDate = asOfDate != null ? asOfDate : LocalDate.now();
        List<KnowledgeChunk> candidates = new ArrayList<>();

        for (Map.Entry<String, List<KnowledgeChunk>> entry : chunkStore.entrySet()) {
            for (KnowledgeChunk chunk : entry.getValue()) {
                if (chunk.getStatus() != RagVersionStatus.ACTIVE && chunk.getStatus() != RagVersionStatus.SUPERSEDED) {
                    continue;
                }

                // Scope matching:
                // If hotelId is provided, accept chunks matching hotelId OR chain public chunks.
                // If hotelId is null, ONLY accept chain public chunks.
                if (chunk.getScopeType() == RagScopeType.HOTEL_SCOPED) {
                    if (hotelId == null || !chunk.getHotelId().equals(hotelId)) {
                        continue;
                    }
                }

                // Date filtering: effectiveFrom <= queryDate and (effectiveTo == null || effectiveTo >= queryDate)
                if (chunk.getEffectiveFrom() != null && chunk.getEffectiveFrom().isAfter(queryDate)) {
                    continue;
                }
                if (chunk.getEffectiveTo() != null && chunk.getEffectiveTo().isBefore(queryDate)) {
                    continue;
                }

                candidates.add(chunk);
            }
        }

        return candidates;
    }

    @Override
    public Optional<KnowledgeDocumentVersion> getDocument(RagScopeType scopeType, Integer hotelId, String sourceId, int version) {
        String key = buildKey(scopeType, hotelId, sourceId);
        List<KnowledgeDocumentVersion> docList = documentStore.get(key);
        if (docList == null) {
            return Optional.empty();
        }
        return docList.stream().filter(d -> d.getVersion() == version).findFirst();
    }

    @Override
    public Optional<KnowledgeDocumentVersion> getActiveDocument(RagScopeType scopeType, Integer hotelId, String sourceId) {
        String key = buildKey(scopeType, hotelId, sourceId);
        List<KnowledgeDocumentVersion> docList = documentStore.get(key);
        if (docList == null) {
            return Optional.empty();
        }
        return docList.stream().filter(d -> d.getStatus() == RagVersionStatus.ACTIVE).findFirst();
    }

    @Override
    public void clearAll() {
        documentStore.clear();
        chunkStore.clear();
    }

    private String buildKey(RagScopeType scopeType, Integer hotelId, String sourceId) {
        String scopePart = scopeType != null ? scopeType.name() : "UNKNOWN";
        String hotelPart = (hotelId != null && hotelId > 0) ? String.valueOf(hotelId) : "CHAIN";
        String sourcePart = sourceId != null ? sourceId.trim().toUpperCase(Locale.ROOT) : "";
        return scopePart + ":" + hotelPart + ":" + sourcePart;
    }
}
