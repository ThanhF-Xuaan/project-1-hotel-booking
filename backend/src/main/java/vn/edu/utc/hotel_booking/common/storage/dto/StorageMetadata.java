package vn.edu.utc.hotel_booking.common.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageMetadata {
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private StorageCategory category;
    private StorageAccessLevel accessLevel;
    private String publicUrl;
    private String eTag;
    private Instant createdAt;
    private Map<String, String> userMetadata;
}
