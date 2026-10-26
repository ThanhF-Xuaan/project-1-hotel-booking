package vn.edu.utc.hotel_booking.common.storage.policy;

import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Đăng ký tập trung chính sách kỹ thuật và an toàn thông tin cho toàn bộ 10 danh mục media.
 */
@Component
public class StoragePolicyRegistry {

    private final Map<StorageCategory, StoragePolicy> policies = new EnumMap<>(StorageCategory.class);

    public StoragePolicyRegistry() {
        // 1. BRAND_ASSET: Logo chuỗi, favicon, icon cố định
        register(StoragePolicy.builder()
                .category(StorageCategory.BRAND_ASSET)
                .folderPrefix("brand")
                .defaultAccessLevel(StorageAccessLevel.PUBLIC_READ)
                .allowedMimeTypes(Set.of("image/png", "image/jpeg", "image/svg+xml", "image/webp"))
                .maxSizeBytes(5 * 1024 * 1024L) // 5 MB
                .requireContentParser(true)
                .requireVirusScan(true)
                .build());

        // 2. HOTEL_ROOM_PHOTO: Ảnh phòng khách sạn
        register(StoragePolicy.builder()
                .category(StorageCategory.HOTEL_ROOM_PHOTO)
                .folderPrefix("rooms")
                .defaultAccessLevel(StorageAccessLevel.PUBLIC_READ)
                .allowedMimeTypes(Set.of("image/png", "image/jpeg", "image/webp"))
                .maxSizeBytes(10 * 1024 * 1024L) // 10 MB
                .requireContentParser(true)
                .requireVirusScan(true)
                .build());

        // 3. HOTEL_AMENITY: Ảnh tiện nghi, dịch vụ
        register(StoragePolicy.builder()
                .category(StorageCategory.HOTEL_AMENITY)
                .folderPrefix("amenities")
                .defaultAccessLevel(StorageAccessLevel.PUBLIC_READ)
                .allowedMimeTypes(Set.of("image/png", "image/jpeg", "image/webp"))
                .maxSizeBytes(10 * 1024 * 1024L) // 10 MB
                .requireContentParser(true)
                .requireVirusScan(true)
                .build());

        // 4. MARKETING_BANNER: Banner do Admin quản lý
        register(StoragePolicy.builder()
                .category(StorageCategory.MARKETING_BANNER)
                .folderPrefix("banners")
                .defaultAccessLevel(StorageAccessLevel.PUBLIC_READ)
                .allowedMimeTypes(Set.of("image/png", "image/jpeg", "image/webp"))
                .maxSizeBytes(15 * 1024 * 1024L) // 15 MB
                .requireContentParser(true)
                .requireVirusScan(true)
                .build());

        // 5. USER_AVATAR: Avatar nhân viên / khách (BẢO MẬT: Mặc định PRIVATE_RESTRICTED)
        register(StoragePolicy.builder()
                .category(StorageCategory.USER_AVATAR)
                .folderPrefix("avatars")
                .defaultAccessLevel(StorageAccessLevel.PRIVATE_RESTRICTED)
                .allowedMimeTypes(Set.of("image/png", "image/jpeg", "image/webp"))
                .maxSizeBytes(5 * 1024 * 1024L) // 5 MB
                .requireContentParser(true)
                .requireVirusScan(true)
                .build());

        // 6. GUEST_REVIEW_PHOTO: Ảnh khách upload review (Mặc định PRIVATE_RESTRICTED đến khi Approved)
        register(StoragePolicy.builder()
                .category(StorageCategory.GUEST_REVIEW_PHOTO)
                .folderPrefix("reviews")
                .defaultAccessLevel(StorageAccessLevel.PRIVATE_RESTRICTED)
                .allowedMimeTypes(Set.of("image/png", "image/jpeg", "image/webp"))
                .maxSizeBytes(10 * 1024 * 1024L) // 10 MB
                .requireContentParser(true)
                .requireVirusScan(true)
                .build());

        // 7. SECURE_CONTRACT: Hợp đồng số scan/PDF
        register(StoragePolicy.builder()
                .category(StorageCategory.SECURE_CONTRACT)
                .folderPrefix("contracts")
                .defaultAccessLevel(StorageAccessLevel.PRIVATE_RESTRICTED)
                .allowedMimeTypes(Set.of("application/pdf", "image/jpeg", "image/png"))
                .maxSizeBytes(25 * 1024 * 1024L) // 25 MB
                .requireContentParser(true)
                .requireVirusScan(true)
                .build());

        // 8. GUEST_IDENTITY: Ảnh CCCD / Passport (PII)
        register(StoragePolicy.builder()
                .category(StorageCategory.GUEST_IDENTITY)
                .folderPrefix("identities")
                .defaultAccessLevel(StorageAccessLevel.PRIVATE_RESTRICTED)
                .allowedMimeTypes(Set.of("image/jpeg", "image/png", "image/webp", "application/pdf"))
                .maxSizeBytes(10 * 1024 * 1024L) // 10 MB
                .requireContentParser(true)
                .requireVirusScan(true)
                .build());

        // 9. FINANCIAL_RECEIPT: Hóa đơn điện tử, chứng từ thanh toán
        register(StoragePolicy.builder()
                .category(StorageCategory.FINANCIAL_RECEIPT)
                .folderPrefix("receipts")
                .defaultAccessLevel(StorageAccessLevel.PRIVATE_RESTRICTED)
                .allowedMimeTypes(Set.of("application/pdf", "image/jpeg", "image/png"))
                .maxSizeBytes(15 * 1024 * 1024L) // 15 MB
                .requireContentParser(true)
                .requireVirusScan(true)
                .build());

        // 10. SYSTEM_EXPORT: Tệp xuất báo cáo BI, bảng lương
        register(StoragePolicy.builder()
                .category(StorageCategory.SYSTEM_EXPORT)
                .folderPrefix("exports")
                .defaultAccessLevel(StorageAccessLevel.PRIVATE_RESTRICTED)
                .allowedMimeTypes(Set.of(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "text/csv",
                        "application/pdf"))
                .maxSizeBytes(50 * 1024 * 1024L) // 50 MB
                .requireContentParser(false)
                .requireVirusScan(false)
                .build());
    }

    public void register(StoragePolicy policy) {
        policies.put(policy.getCategory(), policy);
    }

    public StoragePolicy getPolicy(StorageCategory category) {
        StoragePolicy policy = policies.get(category);
        if (policy == null) {
            throw new IllegalArgumentException("Chưa cấu hình StoragePolicy cho danh mục: " + category);
        }
        return policy;
    }
}
