package vn.edu.utc.hotel_booking.common.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicy;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicyRegistry;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Định tuyến và sinh Object Key chuẩn hóa theo phạm vi khách sạn, module nghiệp vụ và cấp độ bảo mật.
 */
@Component
@RequiredArgsConstructor
public class StorageKeyResolver {

    private final StoragePolicyRegistry storagePolicyRegistry;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w.-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /**
     * Sinh Object Key chuẩn hóa.
     *
     * @param category Danh mục tài nguyên
     * @param hotelId ID khách sạn cơ sở (nếu có)
     * @param referenceId ID đối tượng nghiệp vụ (RoomTypeId, BookingId, UserId...)
     * @param originalFilename Tên tệp gốc từ client
     * @return Chuỗi Object Key an toàn trên S3
     */
    public String resolveKey(StorageCategory category, Short hotelId, Long referenceId, String originalFilename) {
        StoragePolicy policy = storagePolicyRegistry.getPolicy(category);
        String accessPrefix = policy.getDefaultAccessLevel() == StorageAccessLevel.PUBLIC_READ ? "public" : "private";
        String cleanName = sanitizeFilename(originalFilename);
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        LocalDate now = LocalDate.now();

        if (hotelId != null) {
            String subPath = referenceId != null 
                    ? String.valueOf(referenceId) 
                    : now.getYear() + "/" + String.format("%02d", now.getMonthValue());
            return String.format("%s/hotels/%d/%s/%s/%s_%s", 
                    accessPrefix, hotelId, policy.getFolderPrefix(), subPath, uuid, cleanName);
        } else if (referenceId != null) {
            // Áp dụng cho User Avatar hoặc Banners chung có mã ID tham chiếu
            return String.format("%s/%s/%d/%s_%s", 
                    accessPrefix, policy.getFolderPrefix(), referenceId, uuid, cleanName);
        } else {
            // Tài nguyên chung toàn chuỗi (Brand, Exports)
            return String.format("%s/common/%s/%d/%02d/%s_%s", 
                    accessPrefix, policy.getFolderPrefix(), now.getYear(), now.getMonthValue(), uuid, cleanName);
        }
    }

    /**
     * Chuẩn hóa tên tệp: loại bỏ ký tự điều hướng Path Traversal (../), dấu tiếng Việt, khoảng trắng.
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file.bin";
        }
        // Loại bỏ đường dẫn thư mục nếu client gửi kèm đường dẫn đầy đủ
        String base = filename.replace("\\", "/");
        int lastSlash = base.lastIndexOf('/');
        if (lastSlash >= 0) {
            base = base.substring(lastSlash + 1);
        }
        String baseWithD = base.replace("đ", "d").replace("Đ", "D");
        String nowhitespace = WHITESPACE.matcher(baseWithD).replaceAll("_");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase();
    }
}
