package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.RagEmbeddingPort.EmbeddingUnavailableException;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
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
            @Lazy RagVectorStorePort vectorStore
    ) {
        this.governanceValidator = governanceValidator;
        this.embeddingPort = embeddingPort;
        this.vectorStore = vectorStore;
    }

    public RagIngestResultDto ingestAndStage(RagIngestRequestDto request, int version) {
        if (request != null && ((request.getContent() != null && request.getContent().length() > 64_000)
                || (request.getTitle() != null && request.getTitle().length() > 500)
                || (request.getSourceId() != null && request.getSourceId().length() > 128))) {
            return rejectedLimit(version);
        }
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

        if (version < 1 || request.getSourceId() == null
                || !request.getSourceId().trim().toUpperCase(Locale.ROOT).matches("[A-Z0-9][A-Z0-9._:-]{0,127}")
                || (request.getScopeType() == RagScopeType.CHAIN_PUBLIC && request.getHotelId() != null)
                || (request.getHotelId() != null && request.getHotelId() > 32767)
                || (request.getEffectiveFrom() != null && request.getEffectiveTo() != null
                    && request.getEffectiveTo().isBefore(request.getEffectiveFrom()))) {
            return RagIngestResultDto.builder().version(version).status(RagVersionStatus.REJECTED)
                    .errorMessage("REJECTED_INVALID_DOCUMENT_METADATA").build();
        }

        UUID docId = UUID.randomUUID();
        String raw = request.getContent() != null ? request.getContent().trim() : "";
        String hash = computeSha256(raw);

        // 2. Semantic paragraph chunking
        String[] rawChunks = raw.split("(?m)^\\s*\\n\\s*");
        long chunkCount = Arrays.stream(rawChunks).filter(part -> !part.isBlank()).limit(257).count();
        if (chunkCount > 256) return rejectedLimit(version);
        int chunkLimit = Math.min(256, embeddingPort.getMaxDocumentChunks());
        if (chunkCount > chunkLimit) {
            return rejectedLimit(version);
        }
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int chunkIdx = 0;

        List<String> docWarnings = new ArrayList<>();
        List<String> embeddingInputs = new ArrayList<>();

        for (String cText : rawChunks) {
            String trimmed = cText.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.length() > embeddingPort.getMaxDocumentChunkCharacters()) return rejectedLimit(version);

            // 3. Indirect injection & price claims sanitization (T12-014, T12-020, T12-V2-038)
            SanitizedContentResult san = governanceValidator.sanitizeChunkContent(trimmed);
            if (!san.getWarnings().isEmpty()) {
                docWarnings.addAll(san.getWarnings());
            }

            String textToEmbed = (request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle() + "\n" : "") + san.getSanitizedText();
            if (san.getSanitizedText().isBlank() || textToEmbed.length() > embeddingPort.getMaxEmbeddingInputCharacters()) {
                return rejectedLimit(version);
            }
            embeddingInputs.add(textToEmbed);

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
                    .effectiveFrom(request.getEffectiveFrom())
                    .effectiveTo(request.getEffectiveTo())
                    .status(RagVersionStatus.STAGED)
                    .warnings(san.getWarnings())
                    .build();

            chunks.add(chunk);
        }

        if (chunks.isEmpty()) return rejectedLimit(version);

        // Read-only preflight, outside any write transaction. Store retains its atomic duplicate check.
        try {
            if (vectorStore.getExpectedDimension() != embeddingPort.getDimension()) {
                if (embeddingPort.isExternalProvider()) throw new EmbeddingUnavailableException();
                return RagIngestResultDto.builder().version(version).status(RagVersionStatus.REJECTED)
                        .errorMessage("REJECTED_EMBEDDING_DIMENSION_MISMATCH").build();
            }
            var existing = vectorStore.getDocument(request.getScopeType(), request.getHotelId(), request.getSourceId(), version);
            if (existing == null) throw new EmbeddingUnavailableException();
            if (existing.isPresent()) return RagIngestResultDto.builder().sourceId(request.getSourceId()).version(version)
                    .status(RagVersionStatus.REJECTED).errorMessage("REJECTED_DUPLICATE_VERSION").build();
        }
        catch (RuntimeException dependencyFailure) { throw new EmbeddingUnavailableException(); }

        // No store mutation until the complete batch has been returned and validated.
        try {
            List<float[]> embeddings = embeddingPort.embedDocuments(embeddingInputs);
            if (embeddings == null || embeddings.size() != chunks.size()) throw new EmbeddingUnavailableException();
            for (int i = 0; i < chunks.size(); i++) {
                float[] vector = embeddings.get(i);
                if (vector == null || vector.length != embeddingPort.getDimension()) throw new EmbeddingUnavailableException();
                double squared = 0;
                for (float value : vector) {
                    if (!Float.isFinite(value)) throw new EmbeddingUnavailableException();
                    squared += (double) value * value;
                }
                if (!(squared > 0) || !Double.isFinite(squared)) throw new EmbeddingUnavailableException();
                chunks.get(i).setEmbedding(vector.clone());
            }
        }
        catch (RuntimeException providerFailure) { throw new EmbeddingUnavailableException(); }

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

    public Optional<KnowledgeDocumentVersion> getActiveDocument(RagScopeType scopeType, Integer hotelId, String sourceId) {
        return vectorStore.getActiveDocument(scopeType, hotelId, sourceId);
    }

    public Optional<KnowledgeDocumentVersion> getDocument(RagScopeType scopeType, Integer hotelId, String sourceId, int version) {
        return vectorStore.getDocument(scopeType, hotelId, sourceId, version);
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

    private RagIngestResultDto rejectedLimit(int version) {
        return RagIngestResultDto.builder().version(version).status(RagVersionStatus.REJECTED)
                .errorMessage("REJECTED_INPUT_LIMIT: Corpus input exceeds the bounded local limits").build();
    }
}
