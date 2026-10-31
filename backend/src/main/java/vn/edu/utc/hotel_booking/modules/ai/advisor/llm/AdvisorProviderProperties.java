package vn.edu.utc.hotel_booking.modules.ai.advisor.llm;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** External extraction is opt-in; credentials alone never enable it. */
@Component
@Validated
@ConfigurationProperties(prefix = "hotel.ai.advisor.provider")
public class AdvisorProviderProperties {
    private boolean geminiEnabled = false;
    @Min(100)
    @Max(4000)
    private long timeoutMillis = 4000;
    @Min(1)
    @Max(1024)
    private int maxOutputTokens = 512;

    public boolean isGeminiEnabled() { return geminiEnabled; }
    public void setGeminiEnabled(boolean geminiEnabled) { this.geminiEnabled = geminiEnabled; }
    public long getTimeoutMillis() { return timeoutMillis; }
    public void setTimeoutMillis(long timeoutMillis) { this.timeoutMillis = timeoutMillis; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }

    boolean permitsExternalCall() {
        return geminiEnabled && timeoutMillis >= 100 && timeoutMillis <= 4000
                && maxOutputTokens >= 1 && maxOutputTokens <= 1024;
    }
}
