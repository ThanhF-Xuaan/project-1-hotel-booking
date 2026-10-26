package vn.edu.utc.hotel_booking.common.storage.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicy;
import vn.edu.utc.hotel_booking.common.storage.security.AntivirusScanner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Pipeline bảo vệ tệp tin đa tầng (Defense-in-Depth):
 * 1. Hạn mức dung lượng & Whitelist MIME Type
 * 2. L1: Magic Bytes Check
 * 3. L2: Content Parser Decoding (ImageIO / PDF structure)
 * 4. L3: Antivirus Scanner Hook (ClamAV)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileSecurityPipeline {

    private final AntivirusScanner antivirusScanner;

    private static final byte[] MAGIC_JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] MAGIC_PDF = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] MAGIC_RIFF = new byte[]{0x52, 0x49, 0x46, 0x46}; // RIFF (WebP)

    public void validate(MultipartFile file, StoragePolicy policy) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        // 1. Kiểm tra dung lượng
        if (file.getSize() > policy.getMaxSizeBytes()) {
            throw new AppException(ErrorCode.FILE_SIZE_EXCEEDED,
                    String.format("Dung lượng tệp (%d KB) vượt quá hạn mức %d MB",
                            file.getSize() / 1024, policy.getMaxSizeBytes() / (1024 * 1024)));
        }

        // 2. Kiểm tra MIME Type
        String contentType = file.getContentType();
        if (contentType == null || !policy.getAllowedMimeTypes().contains(contentType.toLowerCase())) {
            throw new AppException(ErrorCode.FILE_FORMAT_NOT_ALLOWED,
                    String.format("Định dạng tệp '%s' không được chấp nhận cho danh mục %s",
                            contentType, policy.getCategory()));
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Không thể đọc dữ liệu tệp tải lên");
        }

        // 3. Tầng L1: Magic Bytes Check
        validateMagicBytes(fileBytes, contentType);

        // 4. Tầng L2: Content Parser Validation (Decode thực tế)
        if (policy.isRequireContentParser()) {
            validateContentParsing(fileBytes, contentType);
        }

        // 5. Tầng L3: Antivirus Scanner Check
        if (policy.isRequireVirusScan()) {
            var scanResult = antivirusScanner.scan(fileBytes, file.getOriginalFilename());
            if (!scanResult.isClean()) {
                log.warn("CẢNH BÁO AN NINH: Tệp '{}' bị nghi nhiễm mã độc: {}", file.getOriginalFilename(), scanResult.getDetails());
                throw new AppException(ErrorCode.FILE_CORRUPTED_OR_MALICIOUS, "Tệp bị từ chối do không vượt qua kiểm tra an toàn mã độc");
            }
        }
    }

    private void validateMagicBytes(byte[] fileBytes, String contentType) {
        if (fileBytes.length < 4) {
            throw new AppException(ErrorCode.FILE_CORRUPTED_OR_MALICIOUS, "Tệp tin bị hỏng hoặc dung lượng không hợp lệ");
        }

        boolean matched = switch (contentType.toLowerCase()) {
            case "image/jpeg" -> startsWith(fileBytes, MAGIC_JPEG);
            case "image/png" -> startsWith(fileBytes, MAGIC_PNG);
            case "application/pdf" -> startsWith(fileBytes, MAGIC_PDF);
            case "image/webp" -> startsWith(fileBytes, MAGIC_RIFF);
            case "image/svg+xml", "text/csv", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> true;
            default -> false;
        };

        if (!matched) {
            throw new AppException(ErrorCode.FILE_CORRUPTED_OR_MALICIOUS, "Header nhị phân không khớp với định dạng MIME khai báo");
        }
    }

    private void validateContentParsing(byte[] fileBytes, String contentType) {
        if (contentType.startsWith("image/") && !contentType.equals("image/svg+xml")) {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new AppException(ErrorCode.FILE_CORRUPTED_OR_MALICIOUS, "Tệp ảnh bị lỗi cấu trúc dữ liệu hoặc chứa mã độc nhúng");
                }
            } catch (AppException ae) {
                throw ae;
            } catch (Exception e) {
                log.warn("Lỗi khi phân tích cấu trúc pixel ảnh: {}", e.getMessage());
                throw new AppException(ErrorCode.FILE_CORRUPTED_OR_MALICIOUS, "Không thể giải mã dữ liệu ảnh");
            }
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
