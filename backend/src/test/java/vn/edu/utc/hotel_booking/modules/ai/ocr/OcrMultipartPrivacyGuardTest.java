package vn.edu.utc.hotel_booking.modules.ai.ocr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.util.unit.DataSize;
import vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties;
import vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrMultipartPrivacyGuard;
import static org.assertj.core.api.Assertions.*;

class OcrMultipartPrivacyGuardTest {
    @Test void disabledEngineKeepsLegacyStartup() {
        assertThatCode(() -> new OcrMultipartPrivacyGuard(new OcrEngineProperties(), new MultipartProperties()).afterPropertiesSet()).doesNotThrowAnyException();
    }

    @Test void enabledEngineRejectsDefaultDiskSpooling() {
        var engine = new OcrEngineProperties();
        engine.setEnabled(true);
        assertThatThrownBy(() -> new OcrMultipartPrivacyGuard(engine, new MultipartProperties()).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void enabledEngineAcceptsSystemMultipartOnlyWhenFullyBufferedInMemory() {
        var engine = new OcrEngineProperties();
        engine.setEnabled(true);

        var systemMultipart = new MultipartProperties();
        systemMultipart.setMaxFileSize(DataSize.ofMegabytes(10));
        systemMultipart.setMaxRequestSize(DataSize.ofMegabytes(20));
        systemMultipart.setFileSizeThreshold(DataSize.ofMegabytes(5));

        assertThatCode(() -> new OcrMultipartPrivacyGuard(engine, systemMultipart).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test void enabledEngineAcceptsLocalOcrMemorySettings() {
        var engine = new OcrEngineProperties();
        engine.setEnabled(true);
        var multipart = new MultipartProperties();
        multipart.setMaxFileSize(DataSize.ofMegabytes(5));
        multipart.setMaxRequestSize(DataSize.ofMegabytes(6));
        multipart.setFileSizeThreshold(DataSize.ofMegabytes(5));
        assertThatCode(() -> new OcrMultipartPrivacyGuard(engine, multipart).afterPropertiesSet()).doesNotThrowAnyException();
        multipart.setMaxRequestSize(DataSize.ofBytes(-1));
        assertThatThrownBy(() -> new OcrMultipartPrivacyGuard(engine, multipart).afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
        multipart.setMaxRequestSize(DataSize.ofMegabytes(6));
        multipart.setFileSizeThreshold(DataSize.ofMegabytes(4));
        assertThatThrownBy(() -> new OcrMultipartPrivacyGuard(engine, multipart).afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
    }

    @Test void invalidMultipartFailsClosed() {
        var engine = new OcrEngineProperties();
        engine.setEnabled(true);
        var disabledMultipart = new MultipartProperties();
        disabledMultipart.setEnabled(false);
        assertThatThrownBy(() -> new OcrMultipartPrivacyGuard(engine, disabledMultipart).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);

        var invalidSizeMultipart = new MultipartProperties();
        invalidSizeMultipart.setMaxFileSize(DataSize.ofMegabytes(10));
        invalidSizeMultipart.setMaxRequestSize(DataSize.ofMegabytes(5));
        assertThatThrownBy(() -> new OcrMultipartPrivacyGuard(engine, invalidSizeMultipart).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
    }
}
