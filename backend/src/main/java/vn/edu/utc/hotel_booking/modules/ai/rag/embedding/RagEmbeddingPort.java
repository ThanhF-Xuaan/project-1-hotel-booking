package vn.edu.utc.hotel_booking.modules.ai.rag.embedding;

import java.util.List;

public interface RagEmbeddingPort {
    float[] embedText(String text);

    List<float[]> embedBatch(List<String> texts);

    int getDimension();

    String getModelId();

    default float[] embedDocument(String text) { return embedText(text); }

    default float[] embedQuery(String text) { return embedText(text); }

    default List<float[]> embedDocuments(List<String> texts) {
        return texts.stream().map(this::embedDocument).toList();
    }

    default int getMaxDocumentChunks() { return 256; }

    default int getMaxDocumentChunkCharacters() { return Integer.MAX_VALUE; }

    default int getMaxEmbeddingInputCharacters() { return Integer.MAX_VALUE; }

    default boolean isExternalProvider() { return false; }

    /** No provider payload/cause: safe for controller mapping without logging sensitive exceptions. */
    class EmbeddingUnavailableException extends RuntimeException {
        public EmbeddingUnavailableException() { super("RAG embedding temporarily unavailable"); }
    }
}
