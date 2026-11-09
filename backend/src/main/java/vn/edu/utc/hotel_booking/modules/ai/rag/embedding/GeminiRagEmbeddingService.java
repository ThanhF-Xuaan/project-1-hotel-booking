package vn.edu.utc.hotel_booking.modules.ai.rag.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.PiiRedactionUtil;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.RagProviderProperties;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;

import java.util.ArrayList;
import java.util.List;

/** Fixed embedding operations only. Never fall back to the local hashing model. */
@Service
@ConditionalOnProperty(prefix = "hotel.ai.rag.provider", name = "enabled", havingValue = "true")
public class GeminiRagEmbeddingService implements RagEmbeddingPort {
    private final EmbeddingModel embeddingModel;
    private final String modelId;
    private final int dimension;
    private final int batchSize;
    private final int inputCharacters;

    public GeminiRagEmbeddingService(@Lazy @Qualifier("ragEmbeddingModel") EmbeddingModel embeddingModel,
                                     RagProviderProperties properties) {
        properties.validate();
        if (!properties.isEnabled()) throw new IllegalStateException("Gemini RAG embedding must be explicitly enabled");
        this.embeddingModel = embeddingModel;
        this.modelId = properties.getEmbeddingModel();
        this.dimension = properties.getEmbeddingDimension();
        this.batchSize = properties.getMaxEmbeddingInputs();
        this.inputCharacters = properties.getMaxEmbeddingInputCharacters();
    }

    @Override public int getDimension() { return dimension; }
    @Override public String getModelId() { return modelId; }
    @Override public int getMaxDocumentChunks() { return 64; }
    @Override public int getMaxDocumentChunkCharacters() { return 2000; }
    @Override public int getMaxEmbeddingInputCharacters() { return inputCharacters; }
    @Override public boolean isExternalProvider() { return true; }
    @Override public float[] embedText(String text) { return embedDocument(text); }
    @Override public List<float[]> embedBatch(List<String> texts) { return embedDocuments(texts); }
    @Override public float[] embedDocument(String text) { return embedDocuments(List.of(checkedText(text, inputCharacters))).getFirst(); }

    @Override
    public float[] embedQuery(String text) {
        return embedValidated(List.of(checkedText(text, Math.min(2000, inputCharacters))),
                GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_QUERY).getFirst();
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        if (texts == null || texts.isEmpty() || texts.size() > getMaxDocumentChunks()) {
            throw new IllegalArgumentException("Invalid RAG document batch size");
        }
        // Prepare the ENTIRE version before the first request, including the last input.
        List<String> inputs = texts.stream().map(text -> checkedText(text, inputCharacters)).toList();
        return embedValidated(inputs, GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_DOCUMENT);
    }

    private String checkedText(String text, int limit) {
        if (text == null || text.isBlank() || text.length() > limit) {
            throw new IllegalArgumentException("Invalid or oversized RAG embedding text");
        }
        String safe = PiiRedactionUtil.redact(RagCorpusGovernanceValidator.canonicalSafetyText(text));
        if (safe.isBlank() || safe.length() > limit) throw new IllegalArgumentException("Oversized normalized RAG embedding text");
        return safe;
    }

    private List<float[]> embedValidated(List<String> inputs, GoogleGenAiTextEmbeddingOptions.TaskType task) {
        // Fail closed if a future caller accidentally wraps provider I/O in a DB transaction.
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw new EmbeddingUnavailableException();
        try {
            List<float[]> complete = new ArrayList<>();
            for (int from = 0; from < inputs.size(); from += batchSize) {
                List<String> batch = inputs.subList(from, Math.min(from + batchSize, inputs.size()));
                var response = embeddingModel.call(new EmbeddingRequest(batch,
                        GoogleGenAiTextEmbeddingOptions.builder().model(modelId).dimensions(dimension).taskType(task).build()));
                if (response == null || response.getResults() == null || response.getResults().size() != batch.size()) {
                    throw new EmbeddingUnavailableException();
                }
                for (int i = 0; i < batch.size(); i++) {
                    var value = response.getResults().get(i);
                    if (value == null || value.getIndex() == null || value.getIndex() != i) {
                        throw new EmbeddingUnavailableException();
                    }
                    complete.add(normalize(value.getOutput()));
                }
            }
            return List.copyOf(complete);
        }
        catch (RuntimeException failure) {
            throw new EmbeddingUnavailableException();
        }
    }

    private float[] normalize(float[] vector) {
        if (vector == null || vector.length != dimension) throw new EmbeddingUnavailableException();
        double squared = 0;
        for (float value : vector) {
            if (!Float.isFinite(value)) throw new EmbeddingUnavailableException();
            squared += (double) value * value;
        }
        if (!(squared > 0) || !Double.isFinite(squared)) throw new EmbeddingUnavailableException();
        double norm = Math.sqrt(squared);
        float[] normalized = new float[dimension];
        for (int i = 0; i < dimension; i++) normalized[i] = (float) (vector[i] / norm);
        return normalized;
    }
}
