package vn.edu.utc.hotel_booking.aiassistant.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@Slf4j
public class KnowledgeIngestionWorker {
    private final KnowledgeRepository repository;
    private final EmbeddingModel embeddingModel;
    private final TransactionTemplate transactions;
    private final int dimensions;

    public KnowledgeIngestionWorker(KnowledgeRepository repository, EmbeddingModel embeddingModel,
                                    TransactionTemplate transactions,
                                    @Value("${app.ai.embedding-dimension}") int dimensions) {
        if (dimensions != 1536) throw new IllegalArgumentException("Migration V002 requires 1536-dimensional embeddings");
        this.repository = repository;
        this.embeddingModel = embeddingModel;
        this.transactions = transactions;
        this.dimensions = dimensions;
    }

    @Scheduled(fixedDelayString = "${app.ai.ingest-delay-ms:5000}")
    public void processOne() {
        List<KnowledgeRepository.IngestJob> claimed = transactions.execute(status -> repository.claimJob());
        if (claimed == null || claimed.isEmpty()) return;
        var job = claimed.getFirst();
        try {
            String content = repository.versionContent(job.versionId());
            List<KnowledgeRepository.EmbeddedChunk> embedded = new ArrayList<>();
            for (String chunk : split(content)) {
                float[] vector = embeddingModel.embed(chunk);
                if (vector.length != dimensions) throw new IllegalStateException("Embedding dimension mismatch");
                embedded.add(new KnowledgeRepository.EmbeddedChunk(chunk, VectorLiteral.of(vector)));
            }
            transactions.executeWithoutResult(status -> repository.finish(job, embedded));
            log.info("AI ingestion completed for version {}", job.versionId());
        } catch (Exception exception) {
            log.warn("AI ingestion failed for version {}: {}", job.versionId(), exception.getClass().getSimpleName());
            transactions.executeWithoutResult(status -> repository.fail(job, exception.getClass().getSimpleName()));
        }
    }

    static List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + 800, text.length());
            if (end < text.length()) {
                int space = text.lastIndexOf(' ', end);
                if (space > start + 400) end = space;
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) chunks.add(chunk);
            start = end;
        }
        return chunks;
    }

}
