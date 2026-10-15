package vn.edu.utc.hotel_booking.modules.ai.rag.embedding;

import java.util.List;

public interface RagEmbeddingPort {
    float[] embedText(String text);

    List<float[]> embedBatch(List<String> texts);

    int getDimension();

    String getModelId();
}
