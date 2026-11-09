package vn.edu.utc.hotel_booking.modules.ai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** No credential field: keys must never appear in property binding errors or toString(). */
@ConfigurationProperties(prefix = "hotel.ai.rag.provider")
public class RagProviderProperties {
    private boolean enabled;
    private String chatModel;
    private String embeddingModel;
    private Integer embeddingDimension;
    private int embeddingTimeoutMillis = 3000;
    private int chatTimeoutMillis = 7000;
    private int requestTimeoutMillis = 12000;
    private int maxOutputTokens = 768;
    private int maxEmbeddingInputs = 64;
    private int maxEmbeddingInputCharacters = 2500;
    // Provisional bootstrap candidate only; not a measured/approved Gemini threshold.
    // Calibration must select and record a grid value before acceptance (G06/G07/G09).
    private double retrievalThreshold = 0.30;

    public void validate() {
        if (!enabled) return;
        requireModel(chatModel, "chat-model");
        requireModel(embeddingModel, "embedding-model");
        require(embeddingDimension != null && embeddingDimension >= 1 && embeddingDimension <= 3072,
                "embedding-dimension must be between 1 and 3072");
        require(embeddingTimeoutMillis >= 100 && embeddingTimeoutMillis <= 3000,
                "embedding-timeout-millis must be between 100 and 3000");
        require(chatTimeoutMillis >= 100 && chatTimeoutMillis <= 7000,
                "chat-timeout-millis must be between 100 and 7000");
        require(requestTimeoutMillis >= embeddingTimeoutMillis + chatTimeoutMillis
                        && requestTimeoutMillis <= 12000,
                "request-timeout-millis must cover provider timeouts and be at most 12000");
        require(maxOutputTokens >= 1 && maxOutputTokens <= 768, "max-output-tokens must be between 1 and 768");
        require(maxEmbeddingInputs >= 1 && maxEmbeddingInputs <= 64, "max-embedding-inputs must be between 1 and 64");
        require(maxEmbeddingInputCharacters >= 1 && maxEmbeddingInputCharacters <= 2500,
                "max-embedding-input-characters must be between 1 and 2500");
        validateRetrievalThreshold();
    }

    public void validateRetrievalThreshold() {
        require(java.util.List.of(0.30, 0.40, 0.50, 0.60).contains(retrievalThreshold),
                "retrieval-threshold must be one of the planned calibration grid candidates");
    }

    private static void requireModel(String value, String name) {
        require(value != null && value.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}"),
                name + " must be an explicit model ID without a URL or resource path");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Invalid RAG provider configuration: " + message);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getChatModel() { return chatModel; }
    public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public Integer getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(Integer embeddingDimension) { this.embeddingDimension = embeddingDimension; }
    public int getEmbeddingTimeoutMillis() { return embeddingTimeoutMillis; }
    public void setEmbeddingTimeoutMillis(int embeddingTimeoutMillis) { this.embeddingTimeoutMillis = embeddingTimeoutMillis; }
    public int getChatTimeoutMillis() { return chatTimeoutMillis; }
    public void setChatTimeoutMillis(int chatTimeoutMillis) { this.chatTimeoutMillis = chatTimeoutMillis; }
    public int getRequestTimeoutMillis() { return requestTimeoutMillis; }
    public void setRequestTimeoutMillis(int requestTimeoutMillis) { this.requestTimeoutMillis = requestTimeoutMillis; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public int getMaxEmbeddingInputs() { return maxEmbeddingInputs; }
    public void setMaxEmbeddingInputs(int maxEmbeddingInputs) { this.maxEmbeddingInputs = maxEmbeddingInputs; }
    public int getMaxEmbeddingInputCharacters() { return maxEmbeddingInputCharacters; }
    public void setMaxEmbeddingInputCharacters(int maxEmbeddingInputCharacters) { this.maxEmbeddingInputCharacters = maxEmbeddingInputCharacters; }
    public double getRetrievalThreshold() { return retrievalThreshold; }
    public void setRetrievalThreshold(double retrievalThreshold) { this.retrievalThreshold = retrievalThreshold; }
}
