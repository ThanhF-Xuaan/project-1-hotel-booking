package vn.edu.utc.hotel_booking.common.storage.policy;

import lombok.Builder;
import lombok.Getter;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;

import java.util.Set;

/**
 * Định nghĩa các ràng buộc kỹ thuật và chính sách an ninh cho từng danh mục tài nguyên.
 */
@Getter
@Builder
public class StoragePolicy {
    private final StorageCategory category;
    private final String folderPrefix;
    private final StorageAccessLevel defaultAccessLevel;
    private final Set<String> allowedMimeTypes;
    private final long maxSizeBytes;
    private final boolean requireVirusScan;
    private final boolean requireContentParser;
}
