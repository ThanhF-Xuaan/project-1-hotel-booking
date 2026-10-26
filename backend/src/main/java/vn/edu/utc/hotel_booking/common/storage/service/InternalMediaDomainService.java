package vn.edu.utc.hotel_booking.common.storage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.storage.StorageKeyResolver;
import vn.edu.utc.hotel_booking.common.storage.domain.MediaLifecycleStatus;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaUploadResponse;
import vn.edu.utc.hotel_booking.common.storage.entity.MediaObject;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicy;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicyRegistry;
import vn.edu.utc.hotel_booking.common.storage.repository.MediaObjectRepository;
import vn.edu.utc.hotel_booking.common.storage.spi.ObjectStorageService;
import vn.edu.utc.hotel_booking.common.storage.validator.FileSecurityPipeline;

import java.io.IOException;
import java.time.Duration;

/**
 * Service nội bộ quản lý toàn bộ vòng đời, tính toàn vẹn và nghiệp vụ lưu trữ của tệp tin đa phương tiện.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalMediaDomainService {

    private final ObjectStorageService objectStorageService;
    private final MediaObjectRepository mediaObjectRepository;
    private final StoragePolicyRegistry storagePolicyRegistry;
    private final StorageKeyResolver storageKeyResolver;
    private final FileSecurityPipeline fileSecurityPipeline;

    /**
     * Tải tệp lên hệ thống Object Storage kèm kiểm tra an ninh đa tầng và lưu metadata vào DB.
     */
    @Transactional
    public MediaUploadResponse upload(MultipartFile file, StorageCategory category,
                                      Short hotelId, Long referenceId, String uploaderId) {
        StoragePolicy policy = storagePolicyRegistry.getPolicy(category);

        // 1. Kiểm tra an ninh 3 tầng (L1 Magic Bytes + L2 Parser Decode + L3 Antivirus)
        fileSecurityPipeline.validate(file, policy);

        // 2. Tính mã băm SHA-256 để chống trùng lặp và bảo đảm tính toàn vẹn
        String sha256;
        try {
            sha256 = DigestUtils.sha256Hex(file.getInputStream());
        } catch (IOException e) {
            log.error("Lỗi I/O khi đọc luồng để tính SHA-256: {}", file.getOriginalFilename(), e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Không thể tính toán checksum SHA-256 của tệp");
        }

        // 3. Sinh Object Key chuẩn hóa theo phạm vi khách sạn / người dùng
        String objectKey = storageKeyResolver.resolveKey(category, hotelId, referenceId, file.getOriginalFilename());

        // 4. Lưu bản ghi metadata vào cơ sở dữ liệu
        MediaLifecycleStatus initialStatus = (category == StorageCategory.GUEST_REVIEW_PHOTO)
                ? MediaLifecycleStatus.PENDING_MODERATION
                : (category == StorageCategory.BRAND_ASSET ? MediaLifecycleStatus.PUBLISHED : MediaLifecycleStatus.DRAFT);

        MediaObject mediaObject = MediaObject.builder()
                .storageKey(objectKey)
                .bucketName(objectStorageService.getBucketName())
                .category(category)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .checksumSha256(sha256)
                .lifecycleStatus(initialStatus)
                .accessLevel(policy.getDefaultAccessLevel())
                .hotelId(hotelId)
                .referenceId(referenceId)
                .uploadedBy(uploaderId != null ? uploaderId : "system")
                .scanStatus("CLEAN")
                .build();

        mediaObjectRepository.save(mediaObject);

        // 5. Ghi tệp nhị phân vào Object Storage (MinIO / S3 / Local)
        try {
            objectStorageService.upload(objectKey, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            log.error("Lỗi khi đẩy tệp lên Object Storage: {}", objectKey, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Không thể lưu tệp lên hệ thống lưu trữ");
        }

        // 6. Trả về URLs truy cập tương ứng
        String publicUrl = null;
        String presignedUrl = null;
        if (policy.getDefaultAccessLevel() == StorageAccessLevel.PUBLIC_READ) {
            publicUrl = objectStorageService.getPublicUrl(objectKey);
        } else {
            presignedUrl = objectStorageService.generatePresignedUrl(objectKey, Duration.ofMinutes(15));
        }

        return MediaUploadResponse.builder()
                .mediaId(mediaObject.getId())
                .objectKey(objectKey)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .checksumSha256(sha256)
                .category(category)
                .accessLevel(mediaObject.getAccessLevel())
                .lifecycleStatus(mediaObject.getLifecycleStatus())
                .publicUrl(publicUrl)
                .presignedUrl(presignedUrl)
                .build();
    }

    /**
     * Chuyển đổi trạng thái xuất bản (Ví dụ Admin duyệt ảnh review hoặc publish phòng).
     */
    @Transactional
    public void publishMedia(Long mediaId) {
        MediaObject mediaObject = mediaObjectRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

        mediaObject.setLifecycleStatus(MediaLifecycleStatus.PUBLISHED);
        mediaObject.setAccessLevel(StorageAccessLevel.PUBLIC_READ);
        mediaObjectRepository.save(mediaObject);
    }

    /**
     * Tải mảng byte dữ liệu tệp (phục vụ đối soát, OCR nội bộ hoặc trả về file stream).
     */
    public byte[] loadMediaBytes(String objectKey) {
        return objectStorageService.download(objectKey);
    }

    /**
     * Xóa tệp khỏi Storage và đánh dấu soft delete trong DB.
     */
    @Transactional
    public void deleteMedia(String objectKey) {
        mediaObjectRepository.findByStorageKeyAndIsDeletedFalse(objectKey).ifPresent(mo -> {
            mo.setIsDeleted(true);
            mediaObjectRepository.save(mo);
        });
        objectStorageService.delete(objectKey);
    }

    /**
     * Lấy URL xem/tải tệp: Direct URL nếu Public, hoặc Presigned URL nếu Private.
     */
    public String getDownloadUrl(String objectKey, Duration duration) {
        if (objectKey.startsWith("public/")) {
            return objectStorageService.getPublicUrl(objectKey);
        }
        return objectStorageService.generatePresignedUrl(objectKey, duration != null ? duration : Duration.ofMinutes(15));
    }
}
