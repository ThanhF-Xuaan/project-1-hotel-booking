package vn.edu.utc.hotel_booking.common.storage.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.utc.hotel_booking.common.entity.BaseEntity;
import vn.edu.utc.hotel_booking.common.storage.domain.MediaLifecycleStatus;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;

/**
 * Thực thể lưu trữ metadata tệp tin và đối tượng đa phương tiện trên Object Storage.
 */
@Entity
@Table(name = "media_objects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaObject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private StorageCategory category;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 30)
    @Builder.Default
    private MediaLifecycleStatus lifecycleStatus = MediaLifecycleStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 30)
    @Builder.Default
    private StorageAccessLevel accessLevel = StorageAccessLevel.PRIVATE_RESTRICTED;

    @Column(name = "hotel_id")
    private Short hotelId;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "uploaded_by", nullable = false, length = 100)
    private String uploadedBy;

    @Column(name = "scan_status", nullable = false, length = 30)
    @Builder.Default
    private String scanStatus = "CLEAN";

    @Column(name = "scan_result")
    private String scanResult;
}
