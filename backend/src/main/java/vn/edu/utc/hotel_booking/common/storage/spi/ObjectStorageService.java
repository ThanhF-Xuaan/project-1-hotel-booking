package vn.edu.utc.hotel_booking.common.storage.spi;

import java.io.InputStream;
import java.time.Duration;

/**
 * SPI trừu tượng hóa toàn bộ thao tác lưu trữ tệp tin trên Object Storage (S3 / MinIO / Local).
 */
public interface ObjectStorageService {

    /**
     * Tải tệp lên hệ thống lưu trữ.
     */
    void upload(String key, InputStream inputStream, long contentLength, String contentType);

    /**
     * Tải nội dung tệp dưới dạng mảng byte.
     */
    byte[] download(String key);

    /**
     * Tải nội dung tệp dưới dạng luồng dữ liệu InputStream (thích hợp cho tệp lớn).
     */
    InputStream downloadStream(String key);

    /**
     * Xóa tệp khỏi hệ thống lưu trữ.
     */
    void delete(String key);

    /**
     * Kiểm tra tệp có tồn tại không.
     */
    boolean exists(String key);

    /**
     * Sinh SigV4 Presigned URL truy cập tệp có thời hạn (dành cho tệp Private).
     */
    String generatePresignedUrl(String key, Duration duration);

    /**
     * Lấy URL công khai trực tiếp (dành cho tệp Public).
     */
    String getPublicUrl(String key);

    /**
     * Lấy tên bucket hiện tại.
     */
    String getBucketName();
}
