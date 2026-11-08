package vn.edu.utc.hotel_booking.modules.ai.ocr.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.stereotype.Component;

/** Fail closed before serving image OCR if the container could spool identity uploads. */
@Component
public class OcrMultipartPrivacyGuard implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(OcrMultipartPrivacyGuard.class);
    public static final long OCR_MAX_FILE_BYTES = 5_242_880L; // 5 MB
    public static final long OCR_MAX_REQUEST_BYTES = 6_291_456L; // 6 MB

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
        if (engine.isStrictMultipartCheck()) {
            if (file > OCR_MAX_FILE_BYTES || request > OCR_MAX_REQUEST_BYTES || multipart.getFileSizeThreshold().toBytes() < request) {
                throw new IllegalStateException("Image OCR requires bounded in-memory multipart configuration; use the ocr-local profile or equivalent settings");
            }
        } else {
            log.info("OCR engine enabled with system multipart (maxFileSize={} bytes, maxRequestSize={} bytes). Local 5MB boundary guard enforced on OCR endpoints.",
                    file, request);
        }
    }
}
