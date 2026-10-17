package vn.edu.utc.hotel_booking.modules.ai.ocr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.utc.hotel_booking.modules.ai.ocr.validator.OcrUploadValidator;
import vn.edu.utc.hotel_booking.modules.ai.ocr.validator.OcrUploadValidator.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử bộ kiểm tra file tải lên.
 *
 * <p>Mọi nội dung file là <strong>tổng hợp (synthetic)</strong> – không sử dụng ảnh thật.</p>
 */
@DisplayName("OcrUploadValidator – Unit Tests")
class OcrUploadValidatorTest {

    private OcrUploadValidator validator;

    // Magic bytes tổng hợp
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
    private static final byte[] PNG_MAGIC  = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D};
    private static final byte[] WEBP_MAGIC = {0x52, 0x49, 0x46, 0x46, 0x00, 0x10, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50};

    @BeforeEach
    void setUp() {
        validator = new OcrUploadValidator();
    }

    // =====================================================================
    // TC-08: File JPEG hợp lệ ≤ 5 MB + magic bytes khớp → PASS
    // =====================================================================

    @Test
    @DisplayName("TC-08: JPEG hợp lệ, magic bytes đúng, ≤5MB → PASS")
    void tc08_validJpeg_shouldPass() {
        byte[] content = buildContent(JPEG_MAGIC, 10240); // 10 KB
        MockMultipartFile file = new MockMultipartFile("file", "synthetic.jpg", "image/jpeg", content);

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isTrue();
    }

    // =====================================================================
    // TC-09: File PNG hợp lệ → PASS
    // =====================================================================

    @Test
    @DisplayName("TC-09: PNG hợp lệ, magic bytes đúng → PASS")
    void tc09_validPng_shouldPass() {
        byte[] content = buildContent(PNG_MAGIC, 5120);
        MockMultipartFile file = new MockMultipartFile("file", "synthetic.png", "image/png", content);

        assertThat(validator.validate(file).isValid()).isTrue();
    }

    // =====================================================================
    // TC-10: File WebP hợp lệ → PASS
    // =====================================================================

    @Test
    @DisplayName("TC-10: WebP hợp lệ, magic bytes đúng → PASS")
    void tc10_validWebp_shouldPass() {
        byte[] content = buildContent(WEBP_MAGIC, 8192);
        MockMultipartFile file = new MockMultipartFile("file", "synthetic.webp", "image/webp", content);

        assertThat(validator.validate(file).isValid()).isTrue();
    }

    // =====================================================================
    // TC-11: File rỗng → FAIL
    // =====================================================================

    @Test
    @DisplayName("TC-11: File rỗng (0 bytes) → FAIL")
    void tc11_emptyFile_shouldFail() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("rỗng");
    }

    // =====================================================================
    // TC-12: File vượt 5 MB → FAIL
    // =====================================================================

    @Test
    @DisplayName("TC-12: File > 5 MB → FAIL")
    void tc12_oversizedFile_shouldFail() {
        byte[] content = new byte[(int) OcrUploadValidator.MAX_FILE_SIZE_BYTES + 1];
        content[0] = (byte) 0xFF; content[1] = (byte) 0xD8; content[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", content);

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).containsIgnoringCase("5 MB");
    }

    // =====================================================================
    // TC-13: MIME type không hợp lệ (application/pdf) → FAIL
    // =====================================================================

    @Test
    @DisplayName("TC-13: MIME type application/pdf → FAIL")
    void tc13_pdfMimeType_shouldFail() {
        byte[] content = buildContent(JPEG_MAGIC, 1024);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", content);

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).containsIgnoringCase("không được hỗ trợ");
    }

    // =====================================================================
    // TC-14: Tên file chứa path traversal → FAIL
    // =====================================================================

    @Test
    @DisplayName("TC-14: Tên file chứa ../ (path traversal) → FAIL")
    void tc14_pathTraversalFileName_shouldFail() {
        ValidationResult result = validator.validateFileName("../../etc/passwd.jpg");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).containsIgnoringCase("traversal");
    }

    // =====================================================================
    // TC-15: Tên file không có phần mở rộng hợp lệ → FAIL
    // =====================================================================

    @Test
    @DisplayName("TC-15: Tên file .exe → FAIL")
    void tc15_invalidExtension_shouldFail() {
        ValidationResult result = validator.validateFileName("malware.exe");

        assertThat(result.isValid()).isFalse();
    }

    // =====================================================================
    // TC-16: File giả mạo magic bytes (tên .jpg nhưng nội dung text) → FAIL
    // =====================================================================

    @Test
    @DisplayName("TC-16: File giả mạo magic bytes (text content, JPEG mime) → FAIL")
    void tc16_fakeMagicBytes_shouldFail() {
        byte[] fakeContent = "<script>alert('xss')</script>".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", fakeContent);

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).containsIgnoringCase("magic bytes");
    }

    // =====================================================================
    // TC-17: Magic bytes validation – PNG magic trong JPEG file → FAIL
    // =====================================================================

    @Test
    @DisplayName("TC-17: PNG magic bytes được khai báo là image/jpeg → FAIL")
    void tc17_pngBytesInJpegMime_shouldFail() {
        byte[] content = buildContent(PNG_MAGIC, 1024);
        ValidationResult result = validator.validateMagicBytes("image/jpeg", content);

        assertThat(result.isValid()).isFalse();
    }

    // =====================================================================
    // TC-18: JPEG magic bytes xác nhận đúng
    // =====================================================================

    @Test
    @DisplayName("TC-18: JPEG magic bytes đúng → PASS")
    void tc18_jpegMagicBytes_shouldPass() {
        byte[] content = buildContent(JPEG_MAGIC, 16);
        ValidationResult result = validator.validateMagicBytes("image/jpeg", content);

        assertThat(result.isValid()).isTrue();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private byte[] buildContent(byte[] magic, int totalSize) {
        byte[] content = new byte[Math.max(totalSize, magic.length + 4)];
        System.arraycopy(magic, 0, content, 0, magic.length);
        return content;
    }
}
