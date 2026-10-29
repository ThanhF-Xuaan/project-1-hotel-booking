package vn.edu.utc.hotel_booking.modules.ai.ocr.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.stereotype.Component;

/** Fail closed before serving image OCR if the container could spool identity uploads. */
@Component
public class OcrMultipartPrivacyGuard implements InitializingBean {
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
        if (!multipart.isEnabled() || file <= 0 || file > 5_242_880 || request < file
                || request > 6_291_456 || multipart.getFileSizeThreshold().toBytes() < request) {
            throw new IllegalStateException("Image OCR requires bounded in-memory multipart configuration; use the ocr-local profile or equivalent settings");
        }
    }
}
