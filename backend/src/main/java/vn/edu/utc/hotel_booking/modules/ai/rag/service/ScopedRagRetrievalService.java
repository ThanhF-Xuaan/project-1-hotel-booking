package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.jspecify.annotations.Nullable;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagCitationDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort.EmbeddingUnavailableException;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.RagProviderProperties;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.RagVectorStorePort;

import java.time.LocalDate;
import java.util.*;

@Service
public class ScopedRagRetrievalService {

    private static final double MIN_SIMILARITY_THRESHOLD = 0.30;

    private final RagVectorStorePort vectorStore;
    private final RagEmbeddingPort embeddingPort;
    private final double minSimilarity;

    public ScopedRagRetrievalService(@Lazy RagVectorStorePort vectorStore, RagEmbeddingPort embeddingPort) {
        this(vectorStore, embeddingPort, null);
    }

    @Autowired
    public ScopedRagRetrievalService(@Lazy RagVectorStorePort vectorStore, RagEmbeddingPort embeddingPort,
                                     @Nullable RagProviderProperties providerProperties) {
        this.vectorStore = vectorStore;
        this.embeddingPort = embeddingPort;
        if (embeddingPort.isExternalProvider()) {
            var properties = providerProperties != null ? providerProperties : new RagProviderProperties();
            properties.validateRetrievalThreshold();
            this.minSimilarity = properties.getRetrievalThreshold();
        }
        else this.minSimilarity = MIN_SIMILARITY_THRESHOLD;
    }

    public List<ScoredChunk> retrieve(RagQueryRequestDto request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            return Collections.emptyList();
        }

        LocalDate asOfDate = request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now();
        int topK = Math.min(5, (request.getTopK() != null && request.getTopK() > 0) ? request.getTopK() : 5);

        // 1. Candidate selection with strict tenant scope and date window
        List<KnowledgeChunk> candidates = vectorStore.findCandidateChunks(
                null,
                request.getHotelId(),
                asOfDate
        );
        if (candidates == null) throw new IllegalStateException("RAG candidate dependency unavailable");
        if (candidates.size() > 1000) throw new EmbeddingUnavailableException();
        candidates = candidates.stream().filter(chunk -> isEligibleEvidence(chunk, request.getHotelId(), asOfDate)).toList();

        if (embeddingPort.isExternalProvider()) {
            try {
                if (embeddingPort.getDimension() < 1 || embeddingPort.getModelId() == null
                        || embeddingPort.getModelId().isBlank()
                        || vectorStore.getExpectedDimension() != embeddingPort.getDimension()) {
                    throw new EmbeddingUnavailableException();
                }
            }
            catch (RuntimeException failure) { throw new EmbeddingUnavailableException(); }
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Multi-version and Scope Precedence Resolution (T12-005, T12-V2-039, H3, M3)
        // Group candidate chunks by sourceId to identify the winning version per sourceId
        Map<String, KnowledgeChunk> winningRepresentativeBySource = new HashMap<>();
        for (KnowledgeChunk c : candidates) {
            String sId = c.getSourceId();
            if (!winningRepresentativeBySource.containsKey(sId)) {
                winningRepresentativeBySource.put(sId, c);
            } else {
                KnowledgeChunk currentBest = winningRepresentativeBySource.get(sId);
                if (isBetterVersion(c, currentBest)) {
                    winningRepresentativeBySource.put(sId, c);
                }
            }
        }

        // Keep ALL chunks belonging to the winning version of each sourceId (Fix H3)
        List<KnowledgeChunk> activeChunks = new ArrayList<>();
        for (KnowledgeChunk c : candidates) {
            KnowledgeChunk winning = winningRepresentativeBySource.get(c.getSourceId());
            if (winning != null
                    && c.getScopeType() == winning.getScopeType()
                    && Objects.equals(c.getHotelId(), winning.getHotelId())
                    && c.getVersion() == winning.getVersion()) {
                activeChunks.add(c);
            }
        }

        // Validate all winning vectors before spending query quota, even if their score would be low.
        for (KnowledgeChunk chunk : activeChunks) validateVector(chunk.getEmbedding(), embeddingPort.getDimension());
        if (embeddingPort.isExternalProvider()) validateDocumentIdentities(activeChunks);

        // 3. Generate query vector
        float[] queryVector = embeddingPort.embedQuery(request.getQuestion());
        validateVector(queryVector, embeddingPort.getDimension());

        // 4. Compute cosine similarity & filter threshold across all active chunks
        List<ScoredChunk> scoredList = new ArrayList<>();
        for (KnowledgeChunk chunk : activeChunks) {
            float[] chunkVector = chunk.getEmbedding();
            double similarity = computeCosineSimilarity(queryVector, chunkVector);
            if (similarity >= minSimilarity) {
                scoredList.add(new ScoredChunk(chunk, similarity));
            }
        }


        // 5. Rank descending by similarity
        scoredList.sort(Comparator.comparingDouble(ScoredChunk::getSimilarity).reversed()
                .thenComparing(sc -> sc.getChunk().getSourceId()).thenComparingInt(sc -> sc.getChunk().getChunkIndex())
                .thenComparing(sc -> sc.getChunk().getChunkId()));

        if (scoredList.size() > topK) {
            return scoredList.subList(0, topK);
        }
        return scoredList;
    }

    private static void validateVector(float[] vector, int dimension) {
        if (dimension < 1 || vector == null || vector.length != dimension) throw new EmbeddingUnavailableException();
        double squared = 0;
        for (float value : vector) {
            if (!Float.isFinite(value)) throw new EmbeddingUnavailableException();
            squared += (double) value * value;
        }
        // Dot product is cosine only for the normalized vectors guaranteed by the embedding adapters.
        if (!Double.isFinite(squared) || Math.abs(squared - 1.0) > 0.001) throw new EmbeddingUnavailableException();
    }

    private record DocumentKey(RagScopeType scope, Integer hotel, String source, int version) { }

    private void validateDocumentIdentities(List<KnowledgeChunk> chunks) {
        // Chunks have no model-ID field. Check trusted document metadata once per winning version.
        // This is bounded by the same 1,000-candidate cap and never reads a foreign hotel's document.
        Map<DocumentKey, vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeDocumentVersion> documents = new HashMap<>();
        try {
            for (KnowledgeChunk chunk : chunks) {
                var key = new DocumentKey(chunk.getScopeType(), chunk.getHotelId(), chunk.getSourceId(), chunk.getVersion());
                var doc = documents.computeIfAbsent(key, k -> vectorStore.getDocument(k.scope(), k.hotel(), k.source(), k.version())
                        .orElseThrow(EmbeddingUnavailableException::new));
                if (!Objects.equals(doc.getDocumentId(), chunk.getDocumentId())
                        || !Objects.equals(doc.getEmbeddingModelId(), embeddingPort.getModelId())
                        || doc.getEmbeddingDimension() != embeddingPort.getDimension()
                        || doc.getScopeType() != chunk.getScopeType() || !Objects.equals(doc.getHotelId(), chunk.getHotelId())
                        || !Objects.equals(doc.getSourceId(), chunk.getSourceId()) || doc.getVersion() != chunk.getVersion()
                        || doc.getStatus() != chunk.getStatus() || doc.getCategory() != chunk.getCategory()
                        || !Objects.equals(doc.getEffectiveFrom(), chunk.getEffectiveFrom())
                        || !Objects.equals(doc.getEffectiveTo(), chunk.getEffectiveTo())) {
                    throw new EmbeddingUnavailableException();
                }
            }
        }
        catch (RuntimeException failure) { throw new EmbeddingUnavailableException(); }
    }

    public static boolean isEligibleEvidence(KnowledgeChunk chunk, Integer hotel, LocalDate date) {
        return chunk != null && chunk.getChunkId() != null && chunk.getDocumentId() != null && chunk.getSourceId() != null
                && !chunk.getSourceId().isBlank() && chunk.getVersion() > 0 && chunk.getCategory() != null
                && (chunk.getStatus() == RagVersionStatus.ACTIVE || chunk.getStatus() == RagVersionStatus.SUPERSEDED)
                && ((chunk.getScopeType() == RagScopeType.CHAIN_PUBLIC && chunk.getHotelId() == null)
                    || (chunk.getScopeType() == RagScopeType.HOTEL_SCOPED && hotel != null && hotel.equals(chunk.getHotelId())))
                && (chunk.getEffectiveFrom() == null || !chunk.getEffectiveFrom().isAfter(date))
                && (chunk.getEffectiveTo() == null || !chunk.getEffectiveTo().isBefore(date));
    }

    private double computeCosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private boolean isBetterVersion(KnowledgeChunk candidate, KnowledgeChunk currentBest) {
        if (currentBest == null) {
            return true;
        }

        // 1. Scope Priority (M3): HOTEL_SCOPED overrides CHAIN_PUBLIC
        if (candidate.getScopeType() == RagScopeType.HOTEL_SCOPED && currentBest.getScopeType() == RagScopeType.CHAIN_PUBLIC) {
            return true;
        }
        if (candidate.getScopeType() == RagScopeType.CHAIN_PUBLIC && currentBest.getScopeType() == RagScopeType.HOTEL_SCOPED) {
            return false;
        }

        // 2. Status Priority: ACTIVE beats SUPERSEDED
        if (candidate.getStatus() == RagVersionStatus.ACTIVE && currentBest.getStatus() != RagVersionStatus.ACTIVE) {
            return true;
        }
        if (candidate.getStatus() != RagVersionStatus.ACTIVE && currentBest.getStatus() == RagVersionStatus.ACTIVE) {
            return false;
        }

        // 3. Effective date priority: later effectiveFrom wins
        LocalDate d1 = candidate.getEffectiveFrom() != null ? candidate.getEffectiveFrom() : LocalDate.MIN;
        LocalDate d2 = currentBest.getEffectiveFrom() != null ? currentBest.getEffectiveFrom() : LocalDate.MIN;
        if (d1.isAfter(d2)) {
            return true;
        }
        if (d1.isBefore(d2)) {
            return false;
        }

        // 4. Version number priority: higher version number wins
        return candidate.getVersion() > currentBest.getVersion();
    }


    public static class ScoredChunk {
        private final KnowledgeChunk chunk;
        private final double similarity;

        public ScoredChunk(KnowledgeChunk chunk, double similarity) {
            this.chunk = chunk;
            this.similarity = similarity;
        }

        public KnowledgeChunk getChunk() {
            return chunk;
        }

        public double getSimilarity() {
            return similarity;
        }

        public RagCitationDto toCitation() {
            String text = chunk.getSanitizedContent() != null ? chunk.getSanitizedContent() : chunk.getContent();
            String snippet = text.length() > 150 ? text.substring(0, 150) + "..." : text;
            return RagCitationDto.builder()
                    .sourceId(chunk.getSourceId())
                    .version(chunk.getVersion())
                    .chunkId(chunk.getChunkId())
                    .category(chunk.getCategory())
                    .hotelId(chunk.getHotelId())
                    .effectiveFrom(chunk.getEffectiveFrom())
                    .snippet(snippet)
                    .similarityScore(Math.round(similarity * 1000.0) / 1000.0)
                    .build();
        }
    }
}
