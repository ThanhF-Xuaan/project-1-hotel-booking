package vn.edu.utc.hotel_booking.modules.booking.validator;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Kiểm tra tính hợp lệ và toàn vẹn của tệp tin hợp đồng số tải lên.
 *
 * <p>Tuân thủ:
 * - Luật Giao dịch điện tử 2023 (Toàn vẹn thông điệp dữ liệu, mã băm SHA-256).
 * - Thông tư 105/2020/TT-BTC (Quy chuẩn Mã số thuế Việt Nam).
 * - Tiêu chuẩn an toàn thông tin: Giới hạn dung lượng, chặn Path Traversal, kiểm tra Magic Bytes.
 * </p>
 */
@Component
public class ContractFileValidator {

    public static final long MAX_FILE_SIZE_BYTES = 15_728_640L; // 15 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf",
            "docx",
            "doc"
    );

    // Magic bytes signatures
    private static final byte[] MAGIC_PDF = {0x25, 0x50, 0x44, 0x46}; // "%PDF"
    private static final byte[] MAGIC_DOCX_ZIP = {0x50, 0x4B, 0x03, 0x04}; // "PK.."
    private static final byte[] MAGIC_DOC_OLE = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    // Regex Mã số thuế Việt Nam: 10 chữ số hoặc 13 chữ số có dấu gạch ngang (VD: 0101234567 hoặc 0101234567-001)
    private static final Pattern VIETNAM_TAX_CODE_PATTERN = Pattern.compile("^\\d{10}(-\\d{3})?$");

    /**
     * Kết quả kiểm tra tính hợp lệ của tệp tin.
     */
    public static final class ValidationResult {
        private final boolean valid;
        private final String reason;

        private ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Kiểm tra toàn diện tệp hợp đồng tải lên.
     *
     * @param file Tệp MultipartFile
     * @return {@link ValidationResult}
     */
    public ValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.fail("File hợp đồng không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ValidationResult.fail("Dung lượng file vượt quá giới hạn tối đa 15 MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return ValidationResult.fail("Tên file không hợp lệ");
        }

        // Chống Path Traversal và ký tự độc hại
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\") || originalFilename.contains("\0")) {
            return ValidationResult.fail("Tên file chứa ký tự không an toàn (Path Traversal)");
        }

        String extension = getFileExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            return ValidationResult.fail("Phần mở rộng file không được hỗ trợ (Chỉ chấp nhận: .pdf, .docx, .doc)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return ValidationResult.fail("MIME Type không được hỗ trợ: " + contentType);
        }

        // Kiểm tra chữ ký nhị phân thực tế (Magic Bytes)
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(8);
            if (!matchesMagicBytes(header, extension)) {
                return ValidationResult.fail("Nội dung nhị phân thực tế của file không khớp với định dạng: " + extension);
            }
        } catch (IOException e) {
            return ValidationResult.fail("Không thể đọc luồng dữ liệu file: " + e.getMessage());
        }

        return ValidationResult.ok();
    }

    /**
     * Kiểm tra tính hợp lệ của Mã số thuế doanh nghiệp Việt Nam theo Thông tư 105/2020/TT-BTC.
     *
     * @param taxCode Mã số thuế cần kiểm tra
     * @return true nếu hợp lệ, false nếu không hợp lệ
     */
    public boolean isValidVietnamTaxCode(String taxCode) {
        if (taxCode == null || taxCode.isBlank()) {
            return false;
        }
        return VIETNAM_TAX_CODE_PATTERN.matcher(taxCode.trim()).matches();
    }

    /**
     * Tính toán mã băm SHA-256 xác thực tính toàn vẹn thông điệp dữ liệu theo Luật Giao dịch điện tử 2023.
     *
     * @param bytes Mảng byte dữ liệu
     * @return Chuỗi Hex SHA-256 (64 ký tự thường)
     */
    public String calculateSha256(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Không thể tính SHA-256 cho mảng byte rỗng");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thuật toán SHA-256 không khả dụng trên JVM", e);
        }
    }

    private boolean matchesMagicBytes(byte[] header, String extension) {
        if (header == null || header.length < 4) {
            return false;
        }

        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "pdf" -> header[0] == MAGIC_PDF[0] && header[1] == MAGIC_PDF[1]
                    && header[2] == MAGIC_PDF[2] && header[3] == MAGIC_PDF[3];
            case "docx" -> header[0] == MAGIC_DOCX_ZIP[0] && header[1] == MAGIC_DOCX_ZIP[1]
                    && header[2] == MAGIC_DOCX_ZIP[2] && header[3] == MAGIC_DOCX_ZIP[3];
            case "doc" -> header.length >= 8
                    && header[0] == MAGIC_DOC_OLE[0] && header[1] == MAGIC_DOC_OLE[1]
                    && header[2] == MAGIC_DOC_OLE[2] && header[3] == MAGIC_DOC_OLE[3]
                    && header[4] == MAGIC_DOC_OLE[4] && header[5] == MAGIC_DOC_OLE[5]
                    && header[6] == MAGIC_DOC_OLE[6] && header[7] == MAGIC_DOC_OLE[7];
            default -> false;
        };
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDotIndex + 1);
    }
}
