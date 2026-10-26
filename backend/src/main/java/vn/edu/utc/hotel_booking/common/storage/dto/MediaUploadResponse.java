package vn.edu.utc.hotel_booking.common.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.utc.hotel_booking.common.storage.domain.MediaLifecycleStatus;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {
    private Long mediaId;
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private String checksumSha256;
    private StorageCategory category;
    private StorageAccessLevel accessLevel;
    private MediaLifecycleStatus lifecycleStatus;
    /** URL truy cập công khai (nếu là PUBLIC_READ) */
    private String publicUrl;
    /** Presigned URL có thời hạn (nếu là PRIVATE_RESTRICTED / AUTHENTICATED_READ) */
    private String presignedUrl;
}
