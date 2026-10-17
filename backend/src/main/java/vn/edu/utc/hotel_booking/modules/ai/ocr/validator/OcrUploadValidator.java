package vn.edu.utc.hotel_booking.modules.ai.ocr.validator;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/**
 * Kiểm tra tính hợp lệ của file ảnh giấy tờ tuỳ thân được tải lên.
 *
 * <p>Xử lý hoàn toàn trong bộ nhớ (in-memory).
 * Tuyệt đối không ghi file tạm xuống ổ đĩa.</p>
 *
 * <h3>Các điều kiện kiểm tra:</h3>
 * <ol>
 *   <li>MIME type phải thuộc danh sách cho phép: image/jpeg, image/png, image/webp</li>
 *   <li>Kích thước: 0 &lt; size &le; 5,242,880 bytes (5 MB)</li>
 *   <li>Phần mở rộng file (.jpg, .jpeg, .png, .webp) – không chứa ../, ..\ (path traversal)</li>
 *   <li>Magic bytes: nội dung thực tế phải khớp với MIME type khai báo</li>
 * </ol>
 */
@Component
public class OcrUploadValidator {

    public static final long MAX_FILE_SIZE_BYTES = 5_242_880L;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // Magic bytes
    private static final byte[] MAGIC_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_PNG  = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] MAGIC_WEBP_RIFF = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] MAGIC_WEBP_WEBP = {0x57, 0x45, 0x42, 0x50}; // "WEBP" at offset 8

    /**
     * Kết quả kiểm tra – bất biến.
     */
    public static final class ValidationResult {
        private final boolean valid;
        private final String reason;

        private ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult fail(String reason) { return new ValidationResult(false, reason); }

        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
    }

    /**
     * Kiểm tra đầy đủ file MultipartFile (dùng cho endpoint /extract-file).
     *
     * @param file File được tải lên
     * @return {@link ValidationResult} kết quả kiểm tra
     */
    public ValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.fail("File không được rỗng");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ValidationResult.fail("File vượt quá giới hạn 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            return ValidationResult.fail("Định dạng file không được hỗ trợ: " + contentType
                    + ". Chỉ chấp nhận image/jpeg, image/png, image/webp");
        }

        String originalName = file.getOriginalFilename();
        ValidationResult nameResult = validateFileName(originalName);
        if (!nameResult.isValid()) return nameResult;

        byte[] header;
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 12) {
                return ValidationResult.fail("File quá nhỏ, không đủ dữ liệu header");
            }
            header = bytes;
        } catch (IOException e) {
            return ValidationResult.fail("Không đọc được nội dung file");
        }

        return validateMagicBytes(contentType.toLowerCase(), header);
    }

    /**
     * Kiểm tra tên file không chứa path traversal và có phần mở rộng hợp lệ.
     */
    public ValidationResult validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return ValidationResult.fail("Tên file không được để trống");
        }
        // Path traversal
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ValidationResult.fail("Tên file chứa ký tự không hợp lệ (path traversal)");
        }
        String lower = fileName.toLowerCase();
        if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")
                && !lower.endsWith(".png") && !lower.endsWith(".webp")) {
            return ValidationResult.fail("Phần mở rộng file không hợp lệ. Chỉ chấp nhận: .jpg, .jpeg, .png, .webp");
        }
        return ValidationResult.ok();
    }

    /**
     * Kiểm tra magic bytes của nội dung file so với MIME type khai báo.
     *
     * @param mimeType  MIME type đã được lower-case
     * @param fileBytes Nội dung file đầy đủ (ít nhất 12 bytes)
     */
    public ValidationResult validateMagicBytes(String mimeType, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 4) {
            return ValidationResult.fail("Không đủ dữ liệu để kiểm tra magic bytes");
        }
        return switch (mimeType) {
            case "image/jpeg" -> startsWith(fileBytes, MAGIC_JPEG)
                    ? ValidationResult.ok()
                    : ValidationResult.fail("Nội dung file không khớp định dạng JPEG (magic bytes sai)");
            case "image/png" -> startsWith(fileBytes, MAGIC_PNG)
                    ? ValidationResult.ok()
                    : ValidationResult.fail("Nội dung file không khớp định dạng PNG (magic bytes sai)");
            case "image/webp" -> isWebp(fileBytes)
                    ? ValidationResult.ok()
                    : ValidationResult.fail("Nội dung file không khớp định dạng WebP (magic bytes sai)");
            default -> ValidationResult.fail("MIME type không được hỗ trợ: " + mimeType);
        };
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    /**
     * WebP: bytes 0-3 = "RIFF", bytes 8-11 = "WEBP".
     */
    private boolean isWebp(byte[] data) {
        if (data.length < 12) return false;
        return startsWith(data, MAGIC_WEBP_RIFF)
                && data[8] == MAGIC_WEBP_WEBP[0]
                && data[9] == MAGIC_WEBP_WEBP[1]
                && data[10] == MAGIC_WEBP_WEBP[2]
                && data[11] == MAGIC_WEBP_WEBP[3];
    }
}
