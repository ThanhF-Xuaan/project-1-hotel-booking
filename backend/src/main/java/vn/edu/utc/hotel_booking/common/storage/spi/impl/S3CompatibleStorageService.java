package vn.edu.utc.hotel_booking.common.storage.spi.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import vn.edu.utc.hotel_booking.common.config.StorageProperties;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.storage.spi.ObjectStorageService;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hotel.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3CompatibleStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    @PostConstruct
    public void initBucket() {
        var s3Props = storageProperties.getS3();
        if (!s3Props.isAutoCreateBucket()) {
            log.info("hotel.storage.s3.auto-create-bucket=false: Bỏ qua bước tự động khởi tạo bucket S3.");
            return;
        }

        String bucketName = s3Props.getBucketName();
        boolean bucketReady = false;

        // 1. Kiểm tra bucket tồn tại (Idempotent check)
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 Bucket '{}' đã tồn tại và sẵn sàng hoạt động.", bucketName);
            bucketReady = true;
        } catch (NoSuchBucketException e) {
            log.info("Bucket '{}' chưa tồn tại trên S3/MinIO. Đang tiến hành tạo mới...", bucketName);
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("Đã tạo thành công S3 Bucket '{}'.", bucketName);
                bucketReady = true;
            } catch (Exception createEx) {
                log.warn("Không thể tạo bucket '{}': {}. Ứng dụng sẽ tiếp tục khởi động.", bucketName, createEx.getMessage());
            }
        } catch (Exception e) {
            log.warn("Không thể kết nối tới S3 endpoint '{}' khi khởi động (nguyên nhân: {}). Ứng dụng tiếp tục khởi động.",
                    s3Props.getEndpoint(), e.getMessage());
        }

        // 2. Gán chính sách Public Read cho thư mục public/* (Idempotent policy application)
        if (bucketReady) {
            try {
                String publicReadPolicy = String.format("""
                        {
                          "Version": "2012-10-17",
                          "Statement": [
                            {
                              "Sid": "AllowPublicReadForPublicFolder",
                              "Effect": "Allow",
                              "Principal": "*",
                              "Action": ["s3:GetObject"],
                              "Resource": ["arn:aws:s3:::%s/public/*"]
                            }
                          ]
                        }
                        """, bucketName);

                s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                        .bucket(bucketName)
                        .policy(publicReadPolicy)
                        .build());
                log.info("Đã thiết lập Public Read Policy cho prefix 'public/*' trên bucket '{}'.", bucketName);
            } catch (Exception policyEx) {
                log.warn("Bỏ qua hoặc không thể cập nhật policy cho bucket '{}': {}. Tệp vẫn hoạt động bình thường.",
                        bucketName, policyEx.getMessage());
            }
        }
    }

    @Override
    public void upload(String key, InputStream inputStream, long contentLength, String contentType) {
        String bucketName = getBucketName();
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
            log.debug("Đã tải tệp lên S3: bucket={}, key={}", bucketName, key);
        } catch (Exception e) {
            log.error("Lỗi khi tải tệp lên S3: bucket={}, key={}", bucketName, key, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Lỗi khi lưu trữ tệp lên S3/MinIO: " + e.getMessage());
        }
    }

    @Override
    public byte[] download(String key) {
        String bucketName = getBucketName();
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(getRequest).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND, "Không tìm thấy tệp trên S3: " + key);
        } catch (Exception e) {
            log.error("Lỗi khi tải tệp từ S3: bucket={}, key={}", bucketName, key, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Lỗi khi đọc tệp từ S3/MinIO: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadStream(String key) {
        String bucketName = getBucketName();
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObject(getRequest);
        } catch (NoSuchKeyException e) {
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND, "Không tìm thấy tệp trên S3: " + key);
        } catch (Exception e) {
            log.error("Lỗi khi mở luồng đọc tệp từ S3: bucket={}, key={}", bucketName, key, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Lỗi khi đọc tệp từ S3/MinIO: " + e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        String bucketName = getBucketName();
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.debug("Đã xóa tệp khỏi S3: bucket={}, key={}", bucketName, key);
        } catch (Exception e) {
            log.error("Lỗi khi xóa tệp khỏi S3: bucket={}, key={}", bucketName, key, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Lỗi khi xóa tệp khỏi S3/MinIO: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        String bucketName = getBucketName();
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String generatePresignedUrl(String key, Duration duration) {
        String bucketName = getBucketName();
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration != null ? duration : Duration.ofMinutes(15))
                    .getObjectRequest(getRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Lỗi khi sinh presigned URL cho tệp: bucket={}, key={}", bucketName, key, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Không thể sinh Presigned URL: " + e.getMessage());
        }
    }

    @Override
    public String getPublicUrl(String key) {
        var s3Props = storageProperties.getS3();
        if (s3Props.getCdnPublicBaseUrl() != null && !s3Props.getCdnPublicBaseUrl().isBlank()) {
            String base = s3Props.getCdnPublicBaseUrl().replaceAll("/+$", "");
            return base + "/" + key;
        }
        String endpoint = s3Props.getEndpoint().replaceAll("/+$", "");
        return String.format("%s/%s/%s", endpoint, s3Props.getBucketName(), key);
    }

    @Override
    public String getBucketName() {
        return storageProperties.getS3().getBucketName();
    }
}
