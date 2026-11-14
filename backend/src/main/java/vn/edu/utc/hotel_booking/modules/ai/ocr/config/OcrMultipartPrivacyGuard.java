package vn.edu.utc.hotel_booking.modules.ai.ocr.config;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.stereotype.Component;

/** Fail closed before serving image OCR if the container could spool identity uploads. */
@Component
public class OcrMultipartPrivacyGuard implements InitializingBean {
    private static final long OCR_MAX_FILE_BYTES = 5_242_880L;

    private final OcrEngineProperties engine;
    private final MultipartProperties multipart;

    public OcrMultipartPrivacyGuard(OcrEngineProperties engine, MultipartProperties multipart) {
        this.engine = engine;
        this.multipart = multipart;
    }

    @Override
    public void afterPropertiesSet() {
        if (!engine.isEnabled()) return;
        long request = multipart.getMaxRequestSize().toBytes();
        long file = multipart.getMaxFileSize().toBytes();
        if (!multipart.isEnabled() || file <= 0 || request < file) {
            throw new IllegalStateException("Image OCR requires enabled multipart configuration; invalid maxFileSize or maxRequestSize");
        }
        if (multipart.getFileSizeThreshold().toBytes() < OCR_MAX_FILE_BYTES) {
            throw new IllegalStateException("Image OCR requires the multipart memory threshold to cover the maximum 5 MiB image; identity uploads must not be spooled to disk");
        }
    }
}
