package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagVersionStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.CorpusValidationResult;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeDocumentVersion;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.SanitizedContentResult;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.RagVectorStorePort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class RagKnowledgeLifecycleService {

    private final RagCorpusGovernanceValidator governanceValidator;
    private final RagEmbeddingPort embeddingPort;
    private final RagVectorStorePort vectorStore;

    public RagKnowledgeLifecycleService(
            RagCorpusGovernanceValidator governanceValidator,
            RagEmbeddingPort embeddingPort,
            RagVectorStorePort vectorStore
    ) {
        this.governanceValidator = governanceValidator;
        this.embeddingPort = embeddingPort;
        this.vectorStore = vectorStore;
    }

    public RagIngestResultDto ingestAndStage(RagIngestRequestDto request, int version) {
        // 1. Corpus governance validation (T12-V2-036)
        CorpusValidationResult val = governanceValidator.validateForIngestion(request);
        if (!val.isValid()) {
            return RagIngestResultDto.builder()
                    .sourceId(request != null ? request.getSourceId() : null)
                    .version(version)
                    .status(RagVersionStatus.REJECTED)
                    .errorMessage(val.getRejectionReason())
                    .build();
        }

        UUID docId = UUID.randomUUID();
        String raw = request.getContent() != null ? request.getContent().trim() : "";
        String hash = computeSha256(raw);

        // 2. Semantic paragraph chunking
        String[] rawChunks = raw.split("(?m)^\\s*\\n\\s*");
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int chunkIdx = 0;

        List<String> docWarnings = new ArrayList<>();

        for (String cText : rawChunks) {
            String trimmed = cText.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            // 3. Indirect injection & price claims sanitization (T12-014, T12-020, T12-V2-038)
            SanitizedContentResult san = governanceValidator.sanitizeChunkContent(trimmed);
            if (!san.getWarnings().isEmpty()) {
                docWarnings.addAll(san.getWarnings());
            }

            String textToEmbed = (request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle() + "\n" : "") + san.getSanitizedText();
            float[] embedding = embeddingPort.embedText(textToEmbed);

            KnowledgeChunk chunk = KnowledgeChunk.builder()
                    .chunkId(UUID.randomUUID())
                    .documentId(docId)
                    .sourceId(request.getSourceId())
                    .version(version)
                    .chunkIndex(chunkIdx++)
                    .scopeType(request.getScopeType())
                    .hotelId(request.getHotelId())
                    .category(request.getCategory())
                    .content(trimmed)
                    .sanitizedContent(san.getSanitizedText())
                    .embedding(embedding)
                    .effectiveFrom(request.getEffectiveFrom())
                    .effectiveTo(request.getEffectiveTo())
                    .status(RagVersionStatus.STAGED)
                    .warnings(san.getWarnings())
                    .build();

            chunks.add(chunk);
        }

        KnowledgeDocumentVersion doc = KnowledgeDocumentVersion.builder()
                .documentId(docId)
                .sourceId(request.getSourceId())
                .version(version)
                .scopeType(request.getScopeType())
                .hotelId(request.getHotelId())
                .category(request.getCategory())
                .title(request.getTitle())
                .rawContent(raw)
                .contentHash(hash)
                .status(RagVersionStatus.STAGED)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .embeddingModelId(embeddingPort.getModelId())
                .embeddingDimension(embeddingPort.getDimension())
                .chunkingVersion("paragraph-v1")
                .createdAt(Instant.now())
                .build();

        // 4. Staging into vector store
        RagIngestResultDto result = vectorStore.stageDocument(doc, chunks);
        if (!docWarnings.isEmpty()) {
            result.getWarnings().addAll(docWarnings);
        }
        return result;
    }

    public boolean activate(RagActivationRequestDto request) {
        if (request == null) return false;
        return vectorStore.activateVersion(
                request.getScopeType(),
                request.getHotelId(),
                request.getSourceId(),
                request.getVersion()
        );
    }

    public boolean rollback(RagRollbackRequestDto request) {
        if (request == null) return false;
        return vectorStore.rollbackVersion(
                request.getScopeType(),
                request.getHotelId(),
                request.getSourceId(),
                request.getTargetVersion()
        );
    }

    public boolean delete(RagDeleteRequestDto request) {
        if (request == null) return false;
        return vectorStore.deleteVersion(
                request.getScopeType(),
                request.getHotelId(),
                request.getSourceId(),
                request.getVersion()
        );
    }

    private String computeSha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
