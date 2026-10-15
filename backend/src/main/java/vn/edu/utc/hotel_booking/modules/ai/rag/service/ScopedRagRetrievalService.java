package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagCitationDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.RagVectorStorePort;

import java.time.LocalDate;
import java.util.*;

@Service
public class ScopedRagRetrievalService {

    private static final double MIN_SIMILARITY_THRESHOLD = 0.30;

    private final RagVectorStorePort vectorStore;
    private final RagEmbeddingPort embeddingPort;

    public ScopedRagRetrievalService(RagVectorStorePort vectorStore, RagEmbeddingPort embeddingPort) {
        this.vectorStore = vectorStore;
        this.embeddingPort = embeddingPort;
    }

    public List<ScoredChunk> retrieve(RagQueryRequestDto request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            return Collections.emptyList();
        }

        LocalDate asOfDate = request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now();
        int topK = (request.getTopK() != null && request.getTopK() > 0) ? request.getTopK() : 5;

        // 1. Candidate selection with strict tenant scope and date window
        List<KnowledgeChunk> candidates = vectorStore.findCandidateChunks(
                null,
                request.getHotelId(),
                asOfDate
        );

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Multi-version resolution: For the same sourceId, keep only the version with latest effectiveFrom (T12-005, T12-V2-039)
        Map<String, KnowledgeChunk> latestBySource = new HashMap<>();
        for (KnowledgeChunk c : candidates) {
            String sId = c.getSourceId();
            if (!latestBySource.containsKey(sId)) {
                latestBySource.put(sId, c);
            } else {
                KnowledgeChunk existing = latestBySource.get(sId);
                LocalDate d1 = c.getEffectiveFrom() != null ? c.getEffectiveFrom() : LocalDate.MIN;
                LocalDate d2 = existing.getEffectiveFrom() != null ? existing.getEffectiveFrom() : LocalDate.MIN;
                if (d1.isAfter(d2) || (d1.isEqual(d2) && c.getVersion() > existing.getVersion())) {
                    latestBySource.put(sId, c);
                }
            }
        }

        // 3. Generate query vector
        float[] queryVector = embeddingPort.embedText(request.getQuestion());

        // 4. Compute cosine similarity & filter threshold
        List<ScoredChunk> scoredList = new ArrayList<>();
        for (KnowledgeChunk chunk : latestBySource.values()) {
            float[] chunkVector = chunk.getEmbedding();
            if (chunkVector == null || chunkVector.length != queryVector.length) {
                continue;
            }

            double similarity = computeCosineSimilarity(queryVector, chunkVector);
            if (similarity >= MIN_SIMILARITY_THRESHOLD) {
                scoredList.add(new ScoredChunk(chunk, similarity));
            }
        }

        // 5. Rank descending by similarity
        scoredList.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        if (scoredList.size() > topK) {
            return scoredList.subList(0, topK);
        }
        return scoredList;
    }

    private double computeCosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
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
