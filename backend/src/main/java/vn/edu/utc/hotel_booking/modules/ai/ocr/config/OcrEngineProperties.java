package vn.edu.utc.hotel_booking.modules.ai.ocr.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Cấu hình runtime local cho Tesseract. Engine mặc định tắt. */
@Component
@Validated
@ConfigurationProperties(prefix = "hotel.ai.ocr")
public class OcrEngineProperties {

    private boolean enabled = false;
    @NotBlank
    private String executablePath = "tesseract";
    @NotBlank
    private String languages = "vie+eng";
    @Min(1)
    @Max(3000)
    private long timeoutMillis = 3000;
    @Min(1024)
    @Max(1_048_576)
    private int maxOutputBytes = 1_048_576;
    @Min(1)
    @Max(2)
    private int maxConcurrent = 2;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getExecutablePath() { return executablePath; }
    public void setExecutablePath(String executablePath) { this.executablePath = executablePath; }
    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }
    public long getTimeoutMillis() { return timeoutMillis; }
    public void setTimeoutMillis(long timeoutMillis) { this.timeoutMillis = timeoutMillis; }
    public int getMaxOutputBytes() { return maxOutputBytes; }
    public void setMaxOutputBytes(int maxOutputBytes) { this.maxOutputBytes = maxOutputBytes; }
    public int getMaxConcurrent() { return maxConcurrent; }
    public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
}
