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

    @Test void enabledEngineAcceptsDefaultMultipartWithoutCrash() {
        var engine = new OcrEngineProperties();
        engine.setEnabled(true);
        // Default strictMultipartCheck is false -> should not throw on default multipart
        assertThatCode(() -> new OcrMultipartPrivacyGuard(engine, new MultipartProperties()).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test void enabledEngineAcceptsSystemMultipartByDefault() {
        var engine = new OcrEngineProperties();
        engine.setEnabled(true);

        var systemMultipart = new MultipartProperties();
        systemMultipart.setMaxFileSize(DataSize.ofMegabytes(10));
        systemMultipart.setMaxRequestSize(DataSize.ofMegabytes(20));
        systemMultipart.setFileSizeThreshold(DataSize.ofMegabytes(0));

        assertThatCode(() -> new OcrMultipartPrivacyGuard(engine, systemMultipart).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test void enabledEngineRejectsDefaultDiskSpoolingWhenStrictCheckEnabled() {
        var engine = new OcrEngineProperties();
        engine.setEnabled(true);
        engine.setStrictMultipartCheck(true);
        assertThatThrownBy(() -> new OcrMultipartPrivacyGuard(engine, new MultipartProperties()).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void enabledEngineAcceptsOnlyBoundedMemorySettingsUnderStrictCheck() {
        var engine = new OcrEngineProperties();
        engine.setEnabled(true);
        engine.setStrictMultipartCheck(true);
        var multipart = new MultipartProperties();
        multipart.setMaxFileSize(DataSize.ofMegabytes(5));
        multipart.setMaxRequestSize(DataSize.ofMegabytes(6));
        multipart.setFileSizeThreshold(DataSize.ofMegabytes(6));
        assertThatCode(() -> new OcrMultipartPrivacyGuard(engine, multipart).afterPropertiesSet()).doesNotThrowAnyException();
        multipart.setMaxRequestSize(DataSize.ofBytes(-1));
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
