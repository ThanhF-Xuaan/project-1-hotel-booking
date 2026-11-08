package vn.edu.utc.hotel_booking.modules.ai.rag.llm;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Gemini generation remains an explicit opt-in so local/offline RAG stays deterministic. */
@Component
@Validated
@ConfigurationProperties(prefix = "hotel.ai.rag.generation")
public class RagGeminiGenerationProperties {
    private boolean geminiEnabled;

    @Min(100)
    @Max(15_000)
    private long timeoutMillis = 6_000;

    @Min(64)
    @Max(1_024)
    private int maxOutputTokens = 512;

    @Min(1_000)
    @Max(24_000)
    private int maxEvidenceChars = 12_000;

    public boolean isGeminiEnabled() { return geminiEnabled; }
    public void setGeminiEnabled(boolean geminiEnabled) { this.geminiEnabled = geminiEnabled; }
    public long getTimeoutMillis() { return timeoutMillis; }
    public void setTimeoutMillis(long timeoutMillis) { this.timeoutMillis = timeoutMillis; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public int getMaxEvidenceChars() { return maxEvidenceChars; }
    public void setMaxEvidenceChars(int maxEvidenceChars) { this.maxEvidenceChars = maxEvidenceChars; }

    boolean permitsExternalCall() {
        return geminiEnabled
                && timeoutMillis >= 100 && timeoutMillis <= 15_000
                && maxOutputTokens >= 64 && maxOutputTokens <= 1_024
                && maxEvidenceChars >= 1_000 && maxEvidenceChars <= 24_000;
    }
}
